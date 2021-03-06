package pl.edu.agh.xmppclient.app;

import android.content.Context;
import android.os.Handler;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackAndroid;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.util.StringUtils;

import java.io.IOException;

/**
 * Consumes messages from a RabbitMQ broker
 */
public class MessageConsumer implements TestClient {

    String host = "192.168.2.10";
    String port = "5225";
    String username = "admin";
    String password = "ala123";
    private String lastMessage;

    Boolean isRunning = false;

    XMPPTCPConnection connection;
    ConnectionConfiguration connectionConfig;

    private Handler messageHandler = new Handler();
    private Handler consumeHandler = new Handler();

    //A reference to the listener, we can only have one at a time(for now)
    private MessageHandler messageListener;

    // Create runnable for posting back to main thread
    final Runnable returnMessage = new Runnable() {
        public void run() {
            messageListener.onReceiveMessage(lastMessage);
        }
    };

    final Runnable consumeRunner = new Runnable() {
        public void run() {
            consume();
        }
    };

    public MessageConsumer(String server, Context ctx) {
        host = server;
        SmackAndroid.init(ctx);
    }

    /**
     * Set the callback for received messages
     *
     * @param handler The callback
     */
    @Override
    public void setMessageHandler(MessageHandler handler) {
        messageListener = handler;
    }

    /**
     * Create Exchange and then start consuming. A binding needs to be added before any messages will be delivered
     */
    @Override
    public boolean create() {
        try {
            consumeHandler.post(consumeRunner);
            connectionConfig = new ConnectionConfiguration(host, Integer.parseInt(port));
            connectionConfig.setDebuggerEnabled(true);
            connection = new XMPPTCPConnection(connectionConfig);
            connection.connect();
            connection.login(username, password);
            Presence presence = new Presence(Presence.Type.available);
            connection.sendPacket(presence);
            isRunning = true;
            if (connection != null) {
                PacketFilter filter = new MessageTypeFilter(Message.Type.chat);
                connection.addPacketListener(new PacketListener() {
                    public void processPacket(Packet packet) {
                        Message message = (Message) packet;
                        if (message.getBody() != null) {
                            String fromName = StringUtils.parseBareAddress(message.getFrom());
                            lastMessage = message.getBody();
                            messageHandler.post(returnMessage);
                        }
                    }
                }, filter);
            }
        } catch (SmackException.ConnectionException e) {
            e.printStackTrace();
            return false;
        } catch (XMPPException e) {
            e.printStackTrace();
            return false;
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (SmackException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void consume() {
        Thread thread = new Thread() {
            @Override
            public void run() {
            }
        };
        thread.start();
    }

    @Override
    public void dispose() {
        isRunning = false;
    }
}
