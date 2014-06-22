package pl.edu.agh.mobilne.coap_test;


import android.os.Handler;


import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;

import java.net.URI;
import java.net.URISyntaxException;

import de.uniluebeck.itm.ncoap.application.client.CoapClientApplication;
import de.uniluebeck.itm.ncoap.application.client.CoapResponseProcessor;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.EmptyAcknowledgementProcessor;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.InternalEmptyAcknowledgementReceivedMessage;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.InternalRetransmissionTimeoutMessage;
import de.uniluebeck.itm.ncoap.communication.reliability.outgoing.RetransmissionTimeoutProcessor;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.InvalidMessageException;
import de.uniluebeck.itm.ncoap.message.header.Code;
import de.uniluebeck.itm.ncoap.message.header.MsgType;
import de.uniluebeck.itm.ncoap.message.options.InvalidOptionException;
import de.uniluebeck.itm.ncoap.message.options.ToManyOptionsException;


public class CoapTestClient implements TestClient {


    private String SERVER_ADDRESS = "10.0.2.2";
    private int NUMBER_OF_PARALLEL_REQUESTS = 100;
    private int PORT = 5683;
    private MessageHandler messageListener;
    private String lastMessage = "no msg";
    private CoapClientApplication client;
    private Handler messageHandler = new Handler();

    public class SimpleResponseProcessor implements CoapResponseProcessor, EmptyAcknowledgementProcessor,
            RetransmissionTimeoutProcessor {

        private Logger log = Logger.getLogger(this.getClass().getName());

        @Override
        public void processCoapResponse(CoapResponse coapResponse) {
            ChannelBuffer buffer = coapResponse.getPayload();
            byte[] readable = new byte[buffer.readableBytes()];
            buffer.toByteBuffer().get(readable, buffer.readerIndex(), buffer.readableBytes());
            lastMessage = new String(readable);
            messageHandler.post(returnMessage);
        }

        @Override
        public void processEmptyAcknowledgement(InternalEmptyAcknowledgementReceivedMessage message) {
            log.info("Received empty ACK: " + message);
        }

        @Override
        public void processRetransmissionTimeout(InternalRetransmissionTimeoutMessage timeoutMessage) {
            log.info("Transmission timed out: " + timeoutMessage);
        }
    }


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
    public void setMessageHandler(MessageHandler handler) {
        this.messageListener = handler;
    }

    @Override
    public boolean create() {
        try {
            client = new CoapClientApplication();

            for (int i = 1; i <= NUMBER_OF_PARALLEL_REQUESTS; i++) {
                URI targetURI = new URI("coap://" + SERVER_ADDRESS + "/observable/utc-time");
                CoapRequest coapRequest = new CoapRequest(MsgType.CON, Code.GET, targetURI);
                // this option makes server send notifications
                // coapRequest.setObserveOptionRequest();
                client.writeCoapRequest(coapRequest, new SimpleResponseProcessor());
            }


        } catch (InvalidOptionException e) {
            return false;
        } catch (ToManyOptionsException e) {
            return false;
        } catch (URISyntaxException e) {
            return false;
        } catch (InvalidMessageException e) {
            return false;
        }
        return true;
    }

    @Override
    public void dispose() {
        Statistics.reset();
        client.shutdown();

    }
}