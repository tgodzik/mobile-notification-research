package pl.edu.agh.mobilne.coaptest.app;

import org.ws4d.coap.interfaces.CoapChannelManager;
import org.ws4d.coap.interfaces.CoapClient;
import org.ws4d.coap.interfaces.CoapClientChannel;
import org.ws4d.coap.interfaces.CoapRequest;
import org.ws4d.coap.interfaces.CoapResponse;
import org.ws4d.coap.messages.CoapRequestCode;

import java.net.InetAddress;
import java.net.UnknownHostException;


public class BasicCoapClient implements CoapClient {

    private static final String SERVER_ADDRESS = "10.0.2.2";
    private static final int PORT = 5683;
    CoapChannelManager channelManager = null;
    CoapClientChannel clientChannel = null;


    public void sendRequest(){
        try {
            clientChannel = channelManager.connect(this, InetAddress.getByName(SERVER_ADDRESS), PORT);
            CoapRequest coapRequest = clientChannel.createRequest(true, CoapRequestCode.GET);
            clientChannel.sendMessage(coapRequest);

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnectionFailed(CoapClientChannel channel, boolean notReachable, boolean resetByServer) {

        System.out.println("Connection Failed");
    }

    @Override
    public void onResponse(CoapClientChannel channel, CoapResponse response) {
        System.out.println("Received response");
    }
}