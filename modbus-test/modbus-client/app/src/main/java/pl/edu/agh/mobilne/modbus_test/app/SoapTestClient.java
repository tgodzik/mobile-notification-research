package pl.edu.agh.mobilne.modbus_test.app;


import android.os.Handler;

import org.droidsoapclient.client.SoapClient;


public class SoapTestClient implements TestClient {

    SoapClient client;
    private Handler messageHandler = new Handler();
    private MessageHandler messageListener;
    private String lastMessage = "no msg";

    final Runnable returnMessage = new Runnable() {
        public void run() {
            messageListener.onReceiveMessage(lastMessage);
        }
    };

    @Override
    public void setMessageHandler(MessageHandler handler) {
        this.messageListener = handler;
    }

    @Override
    public boolean create() {
        client = new SoapClient("http://10.0.2.2:8008/", "Time", "http://example.com/sample.wsdl",
                "http://10.0.2.2:8008/",false);

         //get response

        int NUMBER_OF_PARALLEL_REQUESTS = 100;
        for (int i = 1; i <= NUMBER_OF_PARALLEL_REQUESTS; i++) {
            client.addParameter("a", 0);
            try {
                Object obj = client.executeCallResponse();
                lastMessage =  obj.toString();
                System.out.println(lastMessage);
                messageHandler.post(returnMessage);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    @Override
    public void dispose() {
        Statistics.reset();
    }
}
