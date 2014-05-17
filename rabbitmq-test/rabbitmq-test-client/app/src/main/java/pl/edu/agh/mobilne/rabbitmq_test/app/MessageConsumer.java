package pl.edu.agh.mobilne.rabbitmq_test.app;

import android.os.Handler;

import com.rabbitmq.client.QueueingConsumer;

import java.io.IOException;


/**
 * Consumes messages from a RabbitMQ broker
 */
public class MessageConsumer extends IConnectToRabbitMQ {


    public MessageConsumer(String server, String exchange) {
        super(server, exchange);
    }

    private QueueingConsumer consumer;

    //last message to post back
    private String message;

    // An interface to be implemented by an object that is interested in messages(listener)
    public interface OnReceiveMessageHandler {
        public void onReceiveMessage(String message);
    }

    //A reference to the listener, we can only have one at a time(for now)
    private OnReceiveMessageHandler onReceiveMessageHandler;

    /**
     * Set the callback for received messages
     *
     * @param handler The callback
     */
    public void setOnReceiveMessageHandler(OnReceiveMessageHandler handler) {
        onReceiveMessageHandler = handler;
    }

    private Handler messageHandler = new Handler();
    private Handler consumeHandler = new Handler();

    // Create runnable for posting back to main thread
    final Runnable returnMessage = new Runnable() {
        public void run() {
            onReceiveMessageHandler.onReceiveMessage(message);
        }
    };

    final Runnable consumeRunner = new Runnable() {
        public void run() {
            consume();
        }
    };

    /**
     * Create Exchange and then start consuming. A binding needs to be added before any messages will be delivered
     */
    @Override
    public boolean connectToRabbitMQ() {

        if (super.connectToRabbitMQ()) {

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
                        message = new String(delivery.getBody());
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
    }
}
