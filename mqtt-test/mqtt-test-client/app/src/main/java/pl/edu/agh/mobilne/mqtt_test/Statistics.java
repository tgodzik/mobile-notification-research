package pl.edu.agh.mobilne.mqtt_test;

public class Statistics {

    public static int messagesNumber = 0;
    public static double averageDelay = 0.0;
    public static long firstMessage = 0l;
    public static long lastMessage = 0l;

    public static double addMessage(String message) {

        long msgTime = Long.parseLong(message);
        lastMessage = System.currentTimeMillis();

        if (firstMessage == 0l)
            firstMessage = System.currentTimeMillis();

        long newDelay = lastMessage - msgTime;

        double num = (double) messagesNumber;
        averageDelay = (num / (num + 1.0)) * averageDelay + ((double) newDelay) / (num + 1);

        messagesNumber++;
        return averageDelay;
    }

}
//211 409 542 401 254 522 288 236 244 289