package pl.edu.agh.mobilne.rabbitmq_test.app;

public class Statistics {

    public static int messagesNumber = 0;
    public static double averageDelay = 0.0;
    public static long firstMessage = 0l;
    public static long lastMessage = 0l;

    public static double addMessage(String message) {

        lastMessage = System.currentTimeMillis();

        if (firstMessage == 0l)
            firstMessage = lastMessage;

        long newDelay = lastMessage - Long.parseLong(message);

        double num = (double) messagesNumber;
        averageDelay = (num / (num + 1.0)) * averageDelay + ((double) newDelay) / (num + 1);

        messagesNumber++;
        return averageDelay;
    }

}