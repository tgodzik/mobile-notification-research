package pl.edu.agh.mobilne.coap_test;


import android.os.Handler;

import org.ws4d.coap.connection.BasicCoapChannelManager;
import org.ws4d.coap.interfaces.CoapChannelManager;
import org.ws4d.coap.interfaces.CoapClient;
import org.ws4d.coap.interfaces.CoapClientChannel;
import org.ws4d.coap.interfaces.CoapRequest;
import org.ws4d.coap.interfaces.CoapResponse;
import org.ws4d.coap.messages.CoapRequestCode;

import java.net.InetAddress;
import java.net.UnknownHostException;


public class CoapTestClient implements CoapClient, TestClient {

    private String SERVER_ADDRESS = "10.0.2.2";
    private int PORT = 5683;
    CoapChannelManager channelManager = null;
    CoapClientChannel clientChannel = null;
    private MessageHandler messageListener;

    private String lastMessage = "no msg";

    private Handler messageHandler = new Handler();
    final Runnable returnMessage = new Runnable() {
        public void run() {
            messageListener.onReceiveMessage(lastMessage);
        }
    };

    public CoapTestClient(String serverName, int port) {
        this.SERVER_ADDRESS = serverName;
        this.PORT = port;
    }

    @Override
    public void onConnectionFailed(CoapClientChannel channel, boolean notReachable, boolean resetByServer) {
        System.out.println("Connection Failed");
    }

    @Override
    public void onResponse(CoapClientChannel channel, CoapResponse response) {
        System.out.println("Received response");
        lastMessage = new String(response.getPayload());
        messageHandler.post(returnMessage);
    }

    @Override
    public void setMessageHandler(MessageHandler handler) {
        this.messageListener = handler;
    }

    @Override
    public boolean create() {
        try {
            channelManager = BasicCoapChannelManager.getInstance();
            clientChannel = channelManager.connect(this, InetAddress.getByName(SERVER_ADDRESS), PORT);
            CoapRequest coapRequest = clientChannel.createRequest(true, CoapRequestCode.GET);
            clientChannel.sendMessage(coapRequest);
            System.out.println("Sent Request");

        } catch (UnknownHostException e) {
            return false;
        }
        return true;
    }


    @Override
    public void dispose() {
        clientChannel.close();

    }
}