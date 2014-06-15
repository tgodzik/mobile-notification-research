package pl.edu.agh.xmppclient.app;


public class Statistics {

    public static int messagesNumber = 0;
    public static double averageDelay = 0.0;
    public static long firstMessage = 0l;
    public static long lastMessage = 0l;

    public static double addMessage(String message) {

        lastMessage = System.currentTimeMillis();
        long msgTime = Long.parseLong(message);

        if (firstMessage == 0l)
            firstMessage = msgTime;

        long newDelay = lastMessage - msgTime;

        double num = (double) messagesNumber;
        averageDelay = (num / (num + 1.0)) * averageDelay + ((double) newDelay) / (num + 1);

        messagesNumber++;
        return averageDelay;
    }

}
