package com.tokudu.demo;

import java.io.IOException;

import com.ibm.mqtt.IMqttClient;
import com.ibm.mqtt.MqttClient;
import com.ibm.mqtt.MqttException;
import com.ibm.mqtt.MqttPersistence;
import com.ibm.mqtt.MqttPersistenceException;
import com.ibm.mqtt.MqttSimpleCallback;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;


public class PushService extends Service {

    public static final String tag = "Mqtt Demo";

    public static  TestClient.MessageHandler messageListener;

    private String lastMessage = "no msg";

    private Handler messageHandler = new Handler();

    final Runnable returnMessage = new Runnable() {
        public void run() {
            messageListener.onReceiveMessage(lastMessage);
        }
    };

    private static final String mqttHost = "10.0.2.2";
    private static int mqttPort = 1884;

    // settings
    private static MqttPersistence MQTT_PERSISTENCE = null;
    private static short MQTT_KEEP_ALIVE = 60 * 15;
    private static int[] MQTT_QUALITIES_OF_SERVICE = {0};
    private static int MQTT_QUALITY_OF_SERVICE = 0;

    // retaining messages
    private static boolean MQTT_RETAINED_PUBLISH = false;

    // MQTT client ID
    public static String MQTT_CLIENT_ID = "test";

    // These are the actions for the service (name are descriptive enough)
    private static final String ACTION_START = MQTT_CLIENT_ID + ".START";
    private static final String ACTION_STOP = MQTT_CLIENT_ID + ".STOP";
    private static final String ACTION_KEEPALIVE = MQTT_CLIENT_ID + ".KEEP_ALIVE";
    private static final String ACTION_RECONNECT = MQTT_CLIENT_ID + ".RECONNECT";

    // Connection log for the push service. Good for debugging.
    private ConnectionLog log;

    // connectivity manager to determining, when the phone loses connection
    private ConnectivityManager connectionManager;
    private boolean isRunning;

    // application level keep-alive interval
    private static final long KEEP_ALIVE_INTERVAL = 1000 * 60 * 28;

    // retry intervals, when the connection is lost.
    private static final long INITIAL_RETRY_INTERVAL = 1000 * 10;
    private static final long MAXIMUM_RETRY_INTERVAL = 1000 * 60 * 30;

    // Preferences instance
    private SharedPreferences preferences;
    // whether or not the service has been started
    public static final String PREF_STARTED = "isStarted";
    // store the deviceID
    public static final String PREF_DEVICE_ID = "deviceID";
    // last retry interval
    public static final String PREF_RETRY = "retryInterval";

    // This is the instance of an MQTT connection.
    private MQTTConnection connection;
    private long startTime;


    // Static method to start the service
    public static void actionStart(Context ctx) {
        Intent i = new Intent(ctx, PushService.class);
        i.setAction(ACTION_START);
        ctx.startService(i);
    }

    // Static method to stop the service
    public static void actionStop(Context ctx) {
        Intent i = new Intent(ctx, PushService.class);
        i.setAction(ACTION_STOP);
        ctx.startService(i);
    }

    // Static method to send a keep alive message
    public static void actionPing(Context ctx) {
        Intent i = new Intent(ctx, PushService.class);
        i.setAction(ACTION_KEEPALIVE);
        ctx.startService(i);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        log("Creating service");
        startTime = System.currentTimeMillis();

        try {
            log = new ConnectionLog();
            Log.i(tag, "Opened log at " + log.getPath());
        } catch (IOException e) {
            Log.e(tag, "Failed to open log", e);
        }

        // Get instances of preferences, connectivity manager and notification manager
        preferences = getSharedPreferences(tag, MODE_PRIVATE);
        connectionManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        handleCrashedService();
    }

    // This method does any necessary clean-up need in case the server has been destroyed by the system
    // and then restarted
    private void handleCrashedService() {
        if (wasStarted()) {
            log("Handling crashed service...");
            stopKeepAlives();
            start();
        }
    }

    @Override
    public void onDestroy() {
        log("Service destroyed (isRunning=" + isRunning + ")");

        // Stop the services, if it has been isRunning
        if (isRunning) {
            stop();
        }

        try {
            if (log != null)
                log.close();
        } catch (IOException e) {
        }
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        log("Service isRunning with intent=" + intent);

        // Do an appropriate action based on the intent.
        if (intent.getAction().equals(ACTION_STOP)) {
            stop();
            stopSelf();
        } else if (intent.getAction().equals(ACTION_START)) {
            start();
        } else if (intent.getAction().equals(ACTION_KEEPALIVE)) {
            keepAlive();
        } else if (intent.getAction().equals(ACTION_RECONNECT)) {
            if (isNetworkAvailable()) {
                reconnectIfNecessary();
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // log helper function
    private void log(String message) {
        log(message, null);
    }

    private void log(String message, Throwable e) {
        if (e != null) {
            Log.e(tag, message, e);

        } else {
            Log.i(tag, message);
        }

        if (log != null) {
            try {
                log.println(message);
            } catch (IOException ex) {
            }
        }
    }

    // Reads whether or not the service has been isRunning from the preferences
    private boolean wasStarted() {
        return preferences.getBoolean(PREF_STARTED, false);
    }

    // Sets whether or not the services has been isRunning in the preferences.
    private void setRunning(boolean running) {
        preferences.edit().putBoolean(PREF_STARTED, running).commit();
        this.isRunning = running;
    }

    private synchronized void start() {
        log("Starting service...");

        // Do nothing, if the service is already running.
        if (isRunning) {
            Log.w(tag, "Attempt to start connection that is already active");
            return;
        }

        // Establish an MQTT connection
        connect();

        // Register a connectivity listener
        registerReceiver(mConnectivityChanged, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    private synchronized void stop() {
        // Do nothing, if the service is not running.
        if (!isRunning) {
            Log.w(tag, "Attempt to stop connection not active.");
            return;
        }

        // Save stopped state in the preferences
        setRunning(false);

        // Remove the connectivity receiver
        unregisterReceiver(mConnectivityChanged);
        // Any existing reconnect timers should be removed, since we explicitly stopping the service.
        cancelReconnect();

        // Destroy the MQTT connection if there is one
        if (connection != null) {
            connection.disconnect();
            connection = null;
        }
    }

    //
    private synchronized void connect() {
        log("Connecting...");
        // fetch the device ID from the preferences.
        String deviceID = preferences.getString(PREF_DEVICE_ID, null);
        // Create a new connection only if the device id is not NULL
        if (deviceID == null) {
            log("Device ID not found.");
        } else {
            try {
                connection = new MQTTConnection(mqttHost, deviceID);
            } catch (MqttException e) {
                // Schedule a reconnect, if we failed to connect
                log("MqttException: " + (e.getMessage() != null ? e.getMessage() : "NULL"));
                if (isNetworkAvailable()) {
                    scheduleReconnect(startTime);
                }
            }
            setRunning(true);
        }
    }

    private synchronized void keepAlive() {
        try {
            // Send a keep alive, if there is a connection.
            if (isRunning && connection != null) {
                connection.sendKeepAlive();
            }
        } catch (MqttException e) {
            log("MqttException: " + (e.getMessage() != null ? e.getMessage() : "NULL"), e);

            connection.disconnect();
            connection = null;
            cancelReconnect();
        }
    }

    // Schedule application level keep-alives using the AlarmManager
    private void startKeepAlives() {
        Intent i = new Intent();
        i.setClass(this, PushService.class);
        i.setAction(ACTION_KEEPALIVE);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        AlarmManager alarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + KEEP_ALIVE_INTERVAL,
                KEEP_ALIVE_INTERVAL, pi);
    }

    // Remove all scheduled keep alives
    private void stopKeepAlives() {
        Intent i = new Intent();
        i.setClass(this, PushService.class);
        i.setAction(ACTION_KEEPALIVE);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        AlarmManager alarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmMgr.cancel(pi);
    }

    // We schedule a reconnect based on the starttime of the service
    public void scheduleReconnect(long startTime) {
        // the last keep-alive interval
        long interval = preferences.getLong(PREF_RETRY, INITIAL_RETRY_INTERVAL);

        // Calculate the elapsed time since the start
        long now = System.currentTimeMillis();
        long elapsed = now - startTime;


        // Set an appropriate interval based on the elapsed time since start
        if (elapsed < interval) {
            interval = Math.min(interval * 4, MAXIMUM_RETRY_INTERVAL);
        } else {
            interval = INITIAL_RETRY_INTERVAL;
        }

        log("Rescheduling connection in " + interval + "ms.");

        // Save the new internval
        preferences.edit().putLong(PREF_RETRY, interval).commit();

        // Schedule a reconnect using the alarm manager.
        Intent i = new Intent();
        i.setClass(this, PushService.class);
        i.setAction(ACTION_RECONNECT);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        AlarmManager alarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmMgr.set(AlarmManager.RTC_WAKEUP, now + interval, pi);
    }

    // Remove the scheduled reconnect
    public void cancelReconnect() {
        Intent i = new Intent();
        i.setClass(this, PushService.class);
        i.setAction(ACTION_RECONNECT);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        AlarmManager alarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmMgr.cancel(pi);
    }

    private synchronized void reconnectIfNecessary() {
        if (isRunning && connection == null) {
            log("Reconnecting...");
            connect();
        }
    }

    // This receiver listeners for network changes and updates the MQTT connection
    // accordingly
    private BroadcastReceiver mConnectivityChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get network info
            NetworkInfo info = (NetworkInfo) intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);

            // Is there connectivity?
            boolean hasConnectivity = (info != null && info.isConnected());

            log("Connectivity changed: connected=" + hasConnectivity);

            if (hasConnectivity) {
                reconnectIfNecessary();
            } else if (connection != null) {
                // if there no connectivity, make sure MQTT connection is destroyed
                connection.disconnect();
                cancelReconnect();
                connection = null;
            }
        }
    };


    // Check if we are online
    private boolean isNetworkAvailable() {
        NetworkInfo info = connectionManager.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    // This inner class is a wrapper on top of MQTT client.
    private class MQTTConnection implements MqttSimpleCallback {
        IMqttClient mqttClient = null;

        // Creates a new connection given the broker address and initial topic
        public MQTTConnection(String brokerHostName, String initTopic) throws MqttException {
            // Create connection spec
            String mqttConnSpec = "tcp://" + brokerHostName + "@" + mqttPort;
            // Create the client and connect
            mqttClient = MqttClient.createMqttClient(mqttConnSpec, MQTT_PERSISTENCE);
            String clientID = MQTT_CLIENT_ID + "/" + preferences.getString(PREF_DEVICE_ID, "");

            mqttClient.connect(clientID, true, MQTT_KEEP_ALIVE);

            // register this client app has being able to receive messages
            mqttClient.registerSimpleHandler(this);

            // Subscribe to an initial topic, which is combination of client ID and device ID.
            initTopic = "test";
            subscribeToTopic(initTopic);

            log("Connection established to " + brokerHostName + " on topic " + initTopic);

            // Save start time
            startTime = System.currentTimeMillis();
            // Star the keep-alives
            startKeepAlives();
        }

        // Disconnect
        public void disconnect() {
            try {
                stopKeepAlives();
                mqttClient.disconnect();
            } catch (MqttPersistenceException e) {
                log("MqttException" + (e.getMessage() != null ? e.getMessage() : " NULL"), e);
            }
        }

        /*
         * Send a request to the message broker to be sent messages published with
         *  the specified topic name. Wildcards are allowed.
         */
        private void subscribeToTopic(String topicName) throws MqttException {

            if ((mqttClient == null) || (!mqttClient.isConnected())) {
                // quick sanity check - don't try and subscribe if we don't have
                //  a connection
                log("Connection error" + "No connection");
            } else {
                String[] topics = {topicName};
                mqttClient.subscribe(topics, MQTT_QUALITIES_OF_SERVICE);
            }
        }

        /*
         * Sends a message to the message broker, requesting that it be published
         *  to the specified topic.
         */
        private void publishToTopic(String topicName, String message) throws MqttException {
            if ((mqttClient == null) || (!mqttClient.isConnected())) {
                log("No connection to public to");
            } else {
                mqttClient.publish(topicName,
                        message.getBytes(),
                        MQTT_QUALITY_OF_SERVICE,
                        MQTT_RETAINED_PUBLISH);
            }
        }

        /*
         * Called if the application loses it's connection to the message broker.
         */
        public void connectionLost() throws Exception {
            log("Loss of connection" + "connection downed");
            stopKeepAlives();
            // null itself
            connection = null;
            if (isNetworkAvailable()) {
                reconnectIfNecessary();
            }
        }

        /*
         * Called when we receive a message from the message broker.
         */
        public void publishArrived(String topicName, byte[] payload, int qos, boolean retained) {
            // Show a notification
            String s = new String(payload);
            lastMessage = s;
            messageHandler.post(returnMessage);
            log("Got message: " + s);
        }

        public void sendKeepAlive() throws MqttException {
            log("Sending keep alive");
            // publish to a keep-alive topic
            publishToTopic(MQTT_CLIENT_ID + "/keepalive", preferences.getString(PREF_DEVICE_ID, ""));
        }
    }
}