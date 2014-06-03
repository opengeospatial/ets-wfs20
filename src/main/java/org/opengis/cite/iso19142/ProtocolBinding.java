package org.opengis.cite.iso19142;

/**
 * An enumerated type that indicates how a request message is bound to an
 * application protocol. In effect, a binding prescribes how the message content
 * is mapped into a concrete exchange format. The WFS v2 specification defines
 * three conformance classes pertaining to protocol bindings; a conforming
 * service must implement at least one of these and advertise all supported
 * bindings in the service capabilities document.
 * 
 * <ul>
 * <li>HTTP GET (constraint name: 'KVPEncoding')</li>
 * <li>HTTP POST (constraint name : 'XMLEncoding')</li>
 * <li>SOAP (constraint name: 'SOAPEncoding')</li>
 * </ul>
 * 
 * @see "ISO 19142:2010, Geographic information -- Web Feature Service"
 * @see <a href="http://tools.ietf.org/html/rfc2616">RFC 2616</a>
 * @see <a href="http://www.w3.org/TR/soap12-part2/#soapinhttp">SOAP HTTP
 *      Binding</a>
 * 
 */
public enum ProtocolBinding {
    /** HTTP GET method */
    GET(WFS2.KVP_ENC),
    /** HTTP POST method */
    POST(WFS2.XML_ENC),
    /** SOAP HTTP binding */
    SOAP(WFS2.SOAP_ENC),
    /** Any supported binding */
    ANY("");

    private final String constraintName;

    private ProtocolBinding(String constraintName) {
        this.constraintName = constraintName;
    }

    public String getConstraintName() {
        return constraintName;
    }
}
