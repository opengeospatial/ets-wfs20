package org.opengis.cite.iso19142.util;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.soap.SOAPPart;
import javax.xml.transform.stream.StreamSource;

/**
 * A W3C SOAP message consumer that converts an input stream to a
 * {@link SOAPMessage} object. It is suitable for reading SOAP 1.2 entities
 * (media type "application/soap+xml").
 *
 * @see <a href="http://tools.ietf.org/html/rfc3902" target="_blank">RFC 3902:
 *      The "application/soap+xml" media type</a>
 */
@Consumes("application/soap+xml")
public class SOAPMessageConsumer implements MessageBodyReader<SOAPMessage> {

    private static final Logger LOGR = Logger
            .getLogger(SOAPMessageConsumer.class.getPackage().getName());

    @Override
    public boolean isReadable(Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        return type.isAssignableFrom(SOAPMessage.class);
    }

    @Override
    public SOAPMessage readFrom(Class<SOAPMessage> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws IOException, WebApplicationException {
        SOAPMessage message = null;
        try {
            MessageFactory messageFactory = MessageFactory.newInstance();
            message = messageFactory.createMessage();
            SOAPPart soapPart = message.getSOAPPart();
            StreamSource messageSource = new StreamSource(entityStream);
            soapPart.setContent(messageSource);
        } catch (SOAPException se) {
            LOGR.warning("Unable to create SOAPMessage.\n" + se.getMessage());
        }
        return message;
    }

}
