package pl.edu.agh.xmppclient.app;

public class Statistics {

    public static int messagesNumber = 0;
    public static double averageDelay = 0.0;
    public static long firstMessage = 0l;
    public static long lastMessage = 0l;
    public static boolean order = true;
    public static long oldMsg = 0l;

    public static double addMessage(String message) {

        long msgTime = Long.parseLong(message);
        lastMessage = System.currentTimeMillis();

        if (firstMessage == 0l)
            firstMessage = System.currentTimeMillis();

        System.out.println("old : " + oldMsg + " new: " + msgTime);
        order = order & (msgTime >= oldMsg);
        oldMsg = msgTime;

        long newDelay = lastMessage - msgTime;

        double num = (double) messagesNumber;
        averageDelay = (num / (num + 1.0)) * averageDelay + ((double) newDelay) / (num + 1);

        messagesNumber++;
        return averageDelay;
    }

    public static void reset() {
        messagesNumber = 0;
        averageDelay = 0.0;
        firstMessage = 0l;
        lastMessage = 0l;
        order = true;
        oldMsg = 0l;
    }

}
