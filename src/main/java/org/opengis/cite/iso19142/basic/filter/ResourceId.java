package org.opengis.cite.iso19142.basic.filter;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import javax.xml.namespace.QName;

import org.opengis.cite.iso19142.FES2;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.cite.iso19142.util.XMLUtils;
import org.w3c.dom.Element;

/**
 * A resource identifier as defined in ISO 19143 (<em>OGC Filter Encoding 2.0
 * Encoding Standard</em>). It may have additional information about a specific
 * version of a resource, and can be used to request specific versions in a
 * filter expression.
 * 
 * @see <a target="_blank" href=
 *      "http://docs.opengeospatial.org/is/09-026r2/09-026r2.html#71">Object
 *      identifiers</a>
 */
public class ResourceId {

    private final String rid;
    private String previousRid;
    private String version;
    private String start;
    private String end;
    DateTimeFormatter dateTimeFormatter;

    /**
     * Constructs a new identifier for the resource version. All members of the
     * version chain have a unique rid value.
     * 
     * @param rid
     *            A String that is a legal XML Schema ID (xsd:ID) value.
     */
    public ResourceId(String rid) {
        // check rid with regular expression?
        this.rid = rid;
        this.dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSS][XXX]");
    }

    /**
     * Get the identifier for this resource version.
     * 
     * @return A String that is a legal xsd:ID value.
     */
    public String getRid() {
        return rid;
    }

    /**
     * Get the identifier of the previous version.
     * 
     * @return A String that identifies the predecessor, or null if there isn't
     *         one.
     */
    public String getPreviousRid() {
        return previousRid;
    }

    /**
     * Set identifier of the previous version.
     * 
     * @param previousRid
     *            A valid xsd:ID value.
     */
    public void setPreviousRid(String previousRid) {
        this.previousRid = previousRid;
    }

    /**
     * Get the version designation for this resource version.
     * 
     * @return A version designation, or null it no value was set.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Set the version designation for this resource version. It must adhere to
     * one of the following schemes:
     * <ul>
     * <li>A positive integer value (1,2,...);</li>
     * <li>A time instant (xsd:dateTime value) indicating when the version was
     * created;</li>
     * <li>One of the following tokens: "FIRST", "LAST", "PREVIOUS", "NEXT",
     * "ALL" (primarily used to select resource versions).</li>
     * </ul>
     * 
     * @param version
     *            A version designator that adheres to a recognized scheme.
     */
    public void setVersion(String version) {
        this.version = version;
    }

    public String getStart() {
        return start;
    }

    /**
     * Set the starting instant of a temporal interval for selecting resource
     * versions. An ending instant must also be specified to define a closed
     * interval.
     * 
     * @param startDateTime
     *            A lexical representation of an xsd:dateTime value.
     */
    public void setStart(String startDateTime) {
        try {
            dateTimeFormatter.parse(startDateTime);
            this.start = startDateTime;
        } catch (DateTimeParseException e) {
        }
    }

    public String getEnd() {
        return end;
    }

    /**
     * Set the ending instant of a temporal interval for selecting resource
     * versions.
     * 
     * @param endDateTime
     *            A lexical representation of an xsd:dateTime value.
     */
    public void setEnd(String endDateTime) {
        try {
            dateTimeFormatter.parse(endDateTime);
            this.end = endDateTime;
        } catch (DateTimeParseException e) {
        }
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("ResourceId [rid=").append(rid);
        str.append(", previousRid=").append(previousRid);
        str.append(", version=").append(version);
        str.append(", start=").append(start);
        str.append(", end=").append(end).append(']');
        return str.toString();
    }

    /**
     * Creates a DOM Element representing this resource identifier.
     * 
     * @return An empty fes:ResourceId element with the attributes set
     *         accordingly.
     */
    public Element toElement() {
        Element resourceId = XMLUtils.createElement(new QName(Namespaces.FES, FES2.RESOURCE_ID));
        resourceId.setAttribute("rid", rid);
        if (null != previousRid) {
            resourceId.setAttribute("previousRid", previousRid);
        }
        if (null != version) {
            resourceId.setAttribute("version", version);
        }
        if (null != start) {
            resourceId.setAttribute("startDate", start);
        }
        if (null != end) {
            resourceId.setAttribute("endDate", end);
        }
        return resourceId;
    }

}
