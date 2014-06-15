package pl.edu.agh.mobile.mqtt_test;


public interface TestClient {

    void setMessageHandler(MessageHandler handler);

    boolean create();

    void dispose();

    // An interface to be implemented by an object that is interested in messages(listener)
    public static interface MessageHandler {
        public void onReceiveMessage(String message);
    }
}
