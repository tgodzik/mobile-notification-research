package pl.edu.agh.mobilne.rabbitmq_test.app;

import android.os.Handler;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

import java.io.IOException;


/**
 * Consumes messages from a RabbitMQ broker
 */
public class MessageConsumer {

    public String serverPath;
    public String queueName;

    protected Channel channel = null;
    protected Connection connection;
    private QueueingConsumer consumer;

    protected boolean isRunning;
    private String lastMessage;

    private Handler messageHandler = new Handler();
    private Handler consumeHandler = new Handler();

    //A reference to the listener, we can only have one at a time(for now)
    private OnReceiveMessageHandler onReceiveMessageHandler;

    // Create runnable for posting back to main thread
    final Runnable returnMessage = new Runnable() {
        public void run() {
            onReceiveMessageHandler.onReceiveMessage(lastMessage);
        }
    };

    final Runnable consumeRunner = new Runnable() {
        public void run() {
            consume();
        }
    };


    // An interface to be implemented by an object that is interested in messages(listener)
    public interface OnReceiveMessageHandler {
        public void onReceiveMessage(String message);
    }

    public MessageConsumer(String server, String queue) {
        serverPath = server;
        queueName = queue;
    }

    /**
     * Set the callback for received messages
     *
     * @param handler The callback
     */
    public void setOnReceiveMessageHandler(OnReceiveMessageHandler handler) {
        onReceiveMessageHandler = handler;
    }

     /**
     * Connect to the broker and create the queue
     * @return success
     */
    public boolean connectToRabbitMQ()
    {
        if(channel != null && channel.isOpen() )
            return true;
        try
        {
            ConnectionFactory connectionFactory = new ConnectionFactory();
            connectionFactory.setUsername("test");
            connectionFactory.setPassword("test");
            connectionFactory.setHost(serverPath);
            connection = connectionFactory.newConnection();
            channel = connection.createChannel();
            channel.queueDeclare("hello", false, false, false, null);
            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }
    /**
     * Create Exchange and then start consuming. A binding needs to be added before any messages will be delivered
     */
    public boolean createConsumer() {

        if (connectToRabbitMQ()) {

            try {
                consumer = new QueueingConsumer(channel);
                channel.basicConsume(queueName, false, consumer);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            isRunning = true;
            consumeHandler.post(consumeRunner);

            return true;
        }
        return false;
    }


    private void consume() {
        Thread thread = new Thread() {

            @Override
            public void run() {
                while (isRunning) {
                    QueueingConsumer.Delivery delivery;
                    try {
                        delivery = consumer.nextDelivery();
                        lastMessage = new String(delivery.getBody());
                        messageHandler.post(returnMessage);
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                }
            }
        };
        thread.start();

    }

    public void dispose() {
        isRunning = false;

        try {
            channel.close();
            connection.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
