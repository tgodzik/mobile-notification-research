/**
* Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
* following conditions are met:
*
* - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
* disclaimer.
* - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
* following disclaimer in the documentation and/or other materials provided with the distribution.
* - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote
* products derived from this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
* INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
* ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
* INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
* GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
* LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
* OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package de.uniluebeck.itm.ncoap.examples.simple.server;

import de.uniluebeck.itm.ncoap.application.server.CoapServerApplication;
import org.apache.log4j.*;

/**
 * This is a very simple server application providing a .well-known/core resource and some resources, i.e.
 * <ul>
 *     <li>
 *         <code>/not-observable/not-delayed</code>: an instance of {@link SimpleNotObservableWebservice}
 *     </li>
 *     <li>
 *         <code>/not-observable/delayed-by-3sec</code> an instance of {@link SimpleNotObservableWebServiceWithDelay}
 *     </li>
 *     <li>
 *         <code>/observable/utc-time</code> an instance of {@link SimpleObservableTimeService}
 *     </li>
 * </ul>
 *
 *
 * @author Oliver Kleine
 */
public class SimpleCoapServer {

    private static Logger log = Logger.getLogger(SimpleCoapServer.class.getName());

    private static void initializeLogging(){
        //String pattern = "%r ms: [%C{1}] %m %n";
        String pattern = "%-23d{yyyy-MM-dd HH:mm:ss,SSS} | %-32.32t | %-35.35c{1} | %-5p | %m%n";
        PatternLayout patternLayout = new PatternLayout(pattern);

        AsyncAppender appender = new AsyncAppender();
        appender.addAppender(new ConsoleAppender(patternLayout));
        Logger.getRootLogger().addAppender(appender);

        Logger.getRootLogger().setLevel(Level.INFO);
        Logger.getLogger("de.uniluebeck.itm.ncoap.examples.simple.server").setLevel(Level.DEBUG);
    }

    /**
     * @param args all arguments (if any) are ignored
     */
    public static void main(String[] args){

        initializeLogging();

        //start the server
        CoapServerApplication server = new CoapServerApplication();
        log.info("Server started and listening on port " + server.getServerPort());

        //Time service
        SimpleObservableTimeService timeService = new SimpleObservableTimeService("/observable/utc-time");
        server.registerService(timeService);

        //That's it!
    }
}
