package pl.edu.agh.mobilne.rabbitmq_test.app;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

/**
 * Base class for objects that connect to a RabbitMQ Broker
 */
public abstract class IConnectToRabbitMQ {

    public String serverPath;
    public String queueName;

    protected Channel channel = null;
    protected Connection connection;

    protected boolean isRunning;

    /**
     *
     * @param server The server address
     * @param queue The named queue
     */
    public IConnectToRabbitMQ(String server, String queue)
    {
        serverPath = server;
        queueName = queue;
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
}