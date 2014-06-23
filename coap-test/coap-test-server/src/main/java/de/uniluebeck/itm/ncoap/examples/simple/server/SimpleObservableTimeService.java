package de.uniluebeck.itm.ncoap.examples.simple.server;

import com.google.common.util.concurrent.SettableFuture;
import de.uniluebeck.itm.ncoap.application.server.webservice.MediaTypeNotSupportedException;
import de.uniluebeck.itm.ncoap.application.server.webservice.ObservableWebService;
import de.uniluebeck.itm.ncoap.message.CoapRequest;
import de.uniluebeck.itm.ncoap.message.CoapResponse;
import de.uniluebeck.itm.ncoap.message.header.Code;
import de.uniluebeck.itm.ncoap.message.options.OptionRegistry.MediaType;
import org.apache.log4j.Logger;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This is an observable webservice providing the UTC time in different serialization types.
 * Supported types are {@link MediaType#TEXT_PLAIN_UTF8} and {@link MediaType#APP_XML}.
 *
 * @author Oliver Kleine
 */
public class SimpleObservableTimeService extends ObservableWebService<Long> {

    private Logger log = Logger.getLogger(SimpleObservableTimeService.class.getName());

    public static int RESOURCE_UPDATE_INTERVAL_MILLIS = 1000;

    public SimpleObservableTimeService(String path) {
        super(path, System.currentTimeMillis());
        setMaxAge(1);
    }

    @Override
    public void setScheduledExecutorService(ScheduledExecutorService executorService) {
        super.setScheduledExecutorService(executorService);
        schedulePeriodicResourceUpdate();
    }


    private void schedulePeriodicResourceUpdate() {

        getScheduledExecutorService().scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                setResourceStatus(System.currentTimeMillis());
            }
        }, 0, RESOURCE_UPDATE_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void processCoapRequest(SettableFuture<CoapResponse> responseFuture, CoapRequest coapRequest,
                                   InetSocketAddress remoteAddress) {
        try {
            if (coapRequest.getCode() == Code.GET) {
                responseFuture.set(processGet(coapRequest));
            } else
                responseFuture.set(new CoapResponse(Code.METHOD_NOT_ALLOWED_405));
        } catch (Exception e) {
            log.error("Exception", e);
            responseFuture.set(new CoapResponse(Code.INTERNAL_SERVER_ERROR_500));
        }
    }

    private CoapResponse processGet(CoapRequest request) throws Exception {

        //Try to get the payload according to the requested media type
        MediaType contentType = null;
        byte[] payload = null;
        if (request.getAcceptedMediaTypes().isEmpty()) {
            payload = getSerializedResourceStatus(MediaType.TEXT_PLAIN_UTF8);
            contentType = MediaType.TEXT_PLAIN_UTF8;
        } else {
            for (MediaType mediaType : request.getAcceptedMediaTypes()) {
                payload = getSerializedResourceStatus(mediaType);
                if (payload != null) {
                    contentType = mediaType;
                    break;
                }
            }
        }

        //Prepare the respons getRootLoggere
        CoapResponse coapResponse;
        if (payload != null && contentType != null) {
            coapResponse = new CoapResponse(Code.CONTENT_205);
            coapResponse.setContentType(contentType);
            coapResponse.setPayload(payload);
        } else {
            coapResponse = new CoapResponse(Code.UNSUPPORTED_MEDIA_TYPE_415);
            String text = "Requested media type(s) not supported:";
            for (MediaType mediaType : request.getAcceptedMediaTypes()) {
                text = text + "\n" + mediaType;
            }
            coapResponse.setPayload(text.getBytes(Charset.forName("UTF-8")));
        }

        return coapResponse;
    }

    @Override
    public void shutdown() {
        //Nothing to do here...
    }


    @Override
    public byte[] getSerializedResourceStatus(MediaType mediaType) throws MediaTypeNotSupportedException {
        log.info("Got it");
        return ("" + System.currentTimeMillis()).getBytes();
    }
}
