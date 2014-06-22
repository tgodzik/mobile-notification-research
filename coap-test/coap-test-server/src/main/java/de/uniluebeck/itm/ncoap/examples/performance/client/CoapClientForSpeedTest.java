package de.uniluebeck.itm.ncoap.examples.performance.client;

import de.uniluebeck.itm.ncoap.application.client.CoapClientApplication;
import de.uniluebeck.itm.ncoap.application.client.CoapResponseProcessor;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.header.Code;
import de.uniluebeck.itm.ncoap.message.header.MsgType;
import org.apache.log4j.*;

import java.net.URI;
import java.nio.charset.Charset;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 23.06.13
 * Time: 19:03
 * To change this template use File | Settings | File Templates.
 */
public class CoapClientForSpeedTest {

    public static final int NUMBER_OF_PARALLEL_REQUESTS = 1000;
    private static Logger log = Logger.getLogger(CoapClientForSpeedTest.class.getName());

    private static void initializeLogging(){
        //Output pattern
        String pattern = "%-23d{yyyy-MM-dd HH:mm:ss,SSS} | %-32.32t | %-35.35c{1} | %-5p | %m%n";
        PatternLayout patternLayout = new PatternLayout(pattern);

        //Appenders
        AsyncAppender appender = new AsyncAppender();
        appender.addAppender(new ConsoleAppender(patternLayout));
        appender.setBufferSize(2000000);

        Logger.getRootLogger().addAppender(appender);

        //Define log levels
        Logger.getRootLogger().setLevel(Level.ERROR);
        Logger.getLogger(CoapClientForSpeedTest.class.getName()).setLevel(Level.INFO);
    }

    public static void main(String[] args) throws Exception {
        initializeLogging();
        CoapClientApplication clientApplication = new CoapClientApplication();

        for(int i = 1; i <= NUMBER_OF_PARALLEL_REQUESTS; i++){
            URI targetURI = new URI("coap://localhost/service" + i);
            CoapRequest coapRequest = new CoapRequest(MsgType.CON, Code.GET, targetURI);
            final int finalI = i;
            clientApplication.writeCoapRequest(coapRequest, new CoapResponseProcessor() {
                @Override
                public void processCoapResponse(CoapResponse coapResponse) {
                    assert(("The value is " + finalI + ".")
                            .equals(new String(coapResponse.getPayload().array(), Charset.forName("UTF-8"))));
                    log.info("Received: " + coapResponse);
                }
            });
        }
    }
}
