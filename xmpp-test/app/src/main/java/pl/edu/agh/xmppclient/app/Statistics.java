package pl.edu.agh.xmppclient.app;

public class Statistics {

    public static int messagesNumber = 0;
    public static double averageDelay = 0.0;

    public static double addMessage(String message) {

        long newDelay = System.currentTimeMillis()-Long.parseLong(message);
        double num = (double) messagesNumber;
        averageDelay = (num / (num + 1.0)) * averageDelay + ((double) newDelay) / (num + 1);

        messagesNumber++;
        return averageDelay;
    }

}
