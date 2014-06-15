package com.tokudu.demo;

import java.io.IOException;

import com.ibm.mqtt.IMqttClient;
import com.ibm.mqtt.MqttClient;
import com.ibm.mqtt.MqttException;
import com.ibm.mqtt.MqttPersistence;
import com.ibm.mqtt.MqttPersistenceException;
import com.ibm.mqtt.MqttSimpleCallback;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;


public class MqttTestClient implements TestClient {

    public static final String tag = "Mqtt Demo";

    public static TestClient.MessageHandler messageListener;

    private String lastMessage = "no msg";

    private Handler messageHandler = new Handler();

    final Runnable returnMessage = new Runnable() {
        public void run() {
            messageListener.onReceiveMessage(lastMessage);
        }
    };

    private String mqttHost = "10.0.2.2";
    private int mqttPort = 1884;

    // settings
    private static MqttPersistence MQTT_PERSISTENCE = null;
    private static int[] MQTT_QUALITIES_OF_SERVICE = {0};

    // MQTT client ID
    public static String MQTT_CLIENT_ID = "test";

    // connectivity manager to determining, when the phone loses connection
    private ConnectivityManager connectionManager;
    private boolean isRunning;

    // Preferences instance
    private SharedPreferences preferences;
    // whether or not the service has been started
    public static final String PREF_STARTED = "isStarted";
    // store the deviceID
    public static final String PREF_DEVICE_ID = "deviceID";

    // This is the instance of an MQTT connection.
    private MQTTConnection connection;

    private String mDeviceID;
    private Activity parent;

    public MqttTestClient(String host, int port, Activity _parent) {
        mqttHost = host;
        mqttPort = port;
        parent = _parent;
        mDeviceID = Settings.Secure.getString(parent.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    @Override
    public void setMessageHandler(TestClient.MessageHandler handler) {
        MqttTestClient.messageListener = handler;
    }

    @Override
    public boolean create() {
        SharedPreferences.Editor editor = parent.getSharedPreferences(MqttTestClient.tag, Context.MODE_PRIVATE).edit();
        editor.putString(MqttTestClient.PREF_DEVICE_ID, mDeviceID);
        editor.commit();
        log("Creating service");

        // Get instances of preferences, connectivity manager and notification manager
        preferences = parent.getSharedPreferences(tag, Context.MODE_PRIVATE);
        connectionManager = (ConnectivityManager) parent.getSystemService(Context.CONNECTIVITY_SERVICE);
        handleCrashedService();


        return true;
    }

    @Override
    public void dispose() {
        log("Service destroyed (isRunning=" + isRunning + ")");

        if (isRunning) {
            stop();
        }

    }


    /**
     * This method does any necessary clean-up need in case the server has been destroyed by the system
     * and then restarted
     */
    private void handleCrashedService() {
        if (wasStarted()) {
            log("Handling crashed service...");
            start();
        }
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
    }

    /**
     * Reads whether or not the service has been isRunning from the preferences
     *
     * @return whether it was started
     */
    private boolean wasStarted() {
        return preferences.getBoolean(PREF_STARTED, false);
    }

    /**
     * Sets whether or not the services has been isRunning in the preferences.
     *
     * @param running is it running
     */
    private void setRunning(boolean running) {
        preferences.edit().putBoolean(PREF_STARTED, running).commit();
        this.isRunning = running;
    }

    private synchronized void start() {
        log("Starting...");

        // Do nothing, if the service is already running.
        if (isRunning) {
            Log.w(tag, "Attempt to start connection that is already active");
            return;
        }

        // Establish an MQTT connection
        connect();
    }

    private synchronized void stop() {

        if (!isRunning) {
            Log.w(tag, "Attempt to stop connection not active.");
            return;
        }

        // Save stopped state in the preferences
        setRunning(false);

        // Destroy the MQTT connection if there is one
        if (connection != null) {
            connection.disconnect();
            connection = null;
        }
    }

    /**
     *
     */
    private synchronized void connect() {
        log("Connecting...");
        // fetch the device ID from the preferences.
        String deviceID = preferences.getString(PREF_DEVICE_ID, null);
        if (deviceID == null) {
            log("Device ID not found.");
        } else {
            try {
                connection = new MQTTConnection(mqttHost, deviceID);
            } catch (MqttException e) {
                log("MqttException: " + (e.getMessage() != null ? e.getMessage() : "NULL"));
            }
            setRunning(true);
        }
    }

    private synchronized void reconnectIfNecessary() {
        if (isRunning && connection == null) {
            log("Reconnecting...");
            connect();
        }
    }

    /**
     * Check if we are online
     */
    private boolean isNetworkAvailable() {
        NetworkInfo info = connectionManager.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }

    /**
     * This inner class is a wrapper on top of MQTT client.
     */
    private class MQTTConnection implements MqttSimpleCallback {
        IMqttClient mqttClient = null;

        /**
         * Creates a new connection given the broker address and initial topic
         */
        public MQTTConnection(String brokerHostName, String initTopic) throws MqttException {
            // Create connection spec
            String mqttConnSpec = "tcp://" + brokerHostName + "@" + mqttPort;
            // Create the client and connect
            mqttClient = MqttClient.createMqttClient(mqttConnSpec, MQTT_PERSISTENCE);
            String clientID = MQTT_CLIENT_ID + "/" + preferences.getString(PREF_DEVICE_ID, "");

            short MQTT_KEEP_ALIVE = 60 * 15;
            mqttClient.connect(clientID, true, MQTT_KEEP_ALIVE);

            // register this client app has being able to receive messages
            mqttClient.registerSimpleHandler(this);

            // Subscribe to an initial topic, which is combination of client ID and device ID.
            initTopic = "test";
            subscribeToTopic(initTopic);

            log("Connection established to " + brokerHostName + " on topic " + initTopic);

        }

        /**
         * Disconnect.
         */
        public void disconnect() {
            try {
                mqttClient.disconnect();
            } catch (MqttPersistenceException e) {
                log("MqttException" + (e.getMessage() != null ? e.getMessage() : " NULL"), e);
            }
        }

        /**
         * Send a request to the message broker to be sent messages published with
         * the specified topic name. Wildcards are allowed.
         */
        private void subscribeToTopic(String topicName) throws MqttException {

            if ((mqttClient == null) || (!mqttClient.isConnected())) {
                log("No connection");
            } else {
                String[] topics = {topicName};
                mqttClient.subscribe(topics, MQTT_QUALITIES_OF_SERVICE);
            }
        }

        /**
         * Called if the application loses it's connection to the message broker.
         */
        public void connectionLost() throws Exception {
            log("Loss of connection.");
            connection = null;
            if (isNetworkAvailable()) {
                reconnectIfNecessary();
            }
        }

        /**
         * Called when we receive a message from the message broker.
         */
        public void publishArrived(String topicName, byte[] payload, int qos, boolean retained) {
            String s = new String(payload);
            lastMessage = s;
            messageHandler.post(returnMessage);
            log("Got message: " + s);
        }


    }
}