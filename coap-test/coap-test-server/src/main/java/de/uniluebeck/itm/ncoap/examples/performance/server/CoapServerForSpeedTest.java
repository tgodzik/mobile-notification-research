package de.uniluebeck.itm.ncoap.examples.performance.server;

import de.uniluebeck.itm.ncoap.application.server.CoapServerApplication;
import org.apache.log4j.*;

/**
 * Created with IntelliJ IDEA.
 * User: olli
 * Date: 23.06.13
 * Time: 19:14
 * To change this template use File | Settings | File Templates.
 */
public class CoapServerForSpeedTest {

    public static final int NO_OF_SERVICES = 20;

    private static Logger log = Logger.getLogger(CoapServerForSpeedTest.class.getName());

    private static void initializeLogging(){
        //Output pattern
        String pattern = "%-23d{yyyy-MM-dd HH:mm:ss,SSS} | %-32.32t | %-35.35c{1} | %-5p | %m%n";
        PatternLayout patternLayout = new PatternLayout(pattern);

        //Appenders
        AsyncAppender appender = new AsyncAppender();
        appender.addAppender(new ConsoleAppender(patternLayout));
        Logger.getRootLogger().addAppender(appender);

        appender.setBufferSize(2000000);

        //Define loglevel
        Logger.getRootLogger().setLevel(Level.DEBUG);
    }

    public static void main(String[] args){
        initializeLogging();

        CoapServerApplication coapServerApplication =
                new CoapServerApplication();

//        for(int i = 1; i <= NO_OF_SERVICES; i++){
//            coapServerApplication.registerService(new SimpleNotObservableWebservice("/service" + i,
//                    new String[]{"Oliver", "Kleine"}));
//        }
    }
}
