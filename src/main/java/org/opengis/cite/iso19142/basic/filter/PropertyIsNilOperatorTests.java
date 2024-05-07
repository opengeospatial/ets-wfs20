package org.opengis.cite.iso19142.basic.filter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import org.opengis.cite.iso19142.ETSAssert;
import org.opengis.cite.iso19142.ErrorMessage;
import org.opengis.cite.iso19142.ErrorMessageKeys;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.cite.iso19142.ProtocolBinding;
import org.opengis.cite.iso19142.WFS2;
import org.opengis.cite.iso19142.util.WFSMessage;
import org.opengis.cite.iso19142.util.XMLUtils;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

/**
 * Tests the response to a GetFeature request that includes a {@code PropertyIsNil} filter predicate that tests the
 * content of a specified property and evaluates if it is nil. It is also possible to check the reason for a missing
 * value by matching on the nilReason parameter. While the gml:boundedBy property is nillable, it is ignored; the
 * application schema must define at least one nillable feature property.
 * 
 * <blockquote> In the GML schema and in GML application schemas, the "nillable" and "nilReason" construction may be
 * used on elements representing GML properties (see 7.2.3). This allows properties that are part of the content of
 * objects and features in GML and GML application languages to be declared to be mandatory, while still permitting them
 * to appear in an instance document with no value. [ISO 19136/OGC 07-036, 8.2.3.2]</blockquote>
 * 
 * <p style="margin-bottom: 0.5em">
 * <strong>Sources</strong>
 * </p>
 * <ul>
 * <li>ISO 19142:2010, cl. A.1.2: Basic WFS</li>
 * <li>ISO 19143:2010, cl. 7.7.3.6: PropertyIsNil operator</li>
 * <li>ISO 19143:2010, cl. A.6: Test cases for standard filter</li>
 * <li>ISO 19136:2007, cl. 8.2.3.2: Elements declared to be "nillable"</li>
 * </ul>
 * 
 * @see <a href="http://portal.opengeospatial.org/files/?artifact_id=20509" target="_blank">OGC 07-036: GML 3.2.1</a>
 */
public class PropertyIsNilOperatorTests extends QueryFilterFixture {

    /**
     * [{@code Test}] Submits a GetFeature request containing a {@code PropertyIsNil} predicate designating a nillable
     * feature property (one per feature type). The response entity must include only feature instances that include the
     * specified property with {@literal @xsi:nil="true"}.
     * 
     * <p>
     * All <strong>Basic</strong> WFS implementations must support the <strong>Standard Filter</strong> conformance
     * class defined in <em>OpenGIS Filter Encoding 2.0 Encoding Standard</em> (ISO 19143).
     * </p>
     * 
     * @param binding
     *            The ProtocolBinding to use for this request.
     * @param featureType
     *            The FeatureType to use for this test.
     */
    @Test(description = "See ISO 19143: 7.7.3.6, A.6", dataProvider = "protocol-featureType")
    public void propertyIsNil( ProtocolBinding binding, QName featureType ) {
        List<QName> nillables = this.dataSampler.getNillableProperties(getModel(), featureType);
        if ( nillables.isEmpty() ) {
            throw new SkipException( "FeatureType " + featureType + " does not contain at least one nillable property" );
        }
        this.reqEntity = WFSMessage.createRequestEntity( GET_FEATURE_MINIMAL, this.wfsVersion );
        WFSMessage.appendSimpleQuery( this.reqEntity, featureType );
        // get last nillable property for this feature type
        QName propName = nillables.get( nillables.size() - 1 );
        addPropertyIsNilPredicate( this.reqEntity, propName, null, false );
        Response rsp = wfsClient.submitRequest( reqEntity, binding );
        this.rspEntity = extractBodyAsDocument( rsp );
        Assert.assertEquals( rsp.getStatus(), Status.OK.getStatusCode(),
                             ErrorMessage.get( ErrorMessageKeys.UNEXPECTED_STATUS ) );
        NodeList features = this.rspEntity.getElementsByTagNameNS(Namespaces.WFS, WFS2.MEMBER);
        
        String xpath = String.format( "ns1:%s[@xsi:nil='true']", propName.getLocalPart() );
        Map<String, String> nsBindings = new HashMap<String, String>();
        nsBindings.put( propName.getNamespaceURI(), "ns1" );
        Node featureNode = null;
        for ( int i = 0; i < features.getLength(); i++ ) {
        	featureNode = XMLUtils.getElementByNamespaceURIandLocalPart(features.item( i ), featureType.getNamespaceURI(), featureType.getLocalPart());
        	if(featureNode != null) {
                ETSAssert.assertXPath( xpath, featureNode, nsBindings );
        	} else {
        		//TODO
        		Assert.fail("No node found.");
        	}
        }
    }

    /**
     * Adds a {@code PropertyIsNil} predicate to a GetFeature request entity with the given property name.
     * 
     * <pre>
     * {@code
     * <Filter xmlns="http://www.opengis.net/fes/2.0">
     *   <PropertyIsNil nilReason="withheld">
     *     <ValueReference>tns:featureProperty</ValueReference>
     *   </PropertyIsNil>
     * </Filter>
     * }
     * </pre>
     * 
     * @param request
     *            The request entity (/wfs:GetFeature).
     * @param propertyName
     *            A QName that specifies the feature property to check.
     * @param nilReason
     *            A String that specifies a reason for the missing value; it may be a standard value or an absolute URI
     *            in accord with the gml:NilReasonType type definition. Supply an empty string or null value if the
     *            reason does not matter.
     * @param negate
     *            Negates the predicate by inserting a {@code <Not>} operator (logical complement).
     */
    void addPropertyIsNilPredicate( Document request, QName propertyName, String nilReason, boolean negate ) {
        if ( !request.getDocumentElement().getLocalName().equals( WFS2.GET_FEATURE ) ) {
            throw new IllegalArgumentException( "Not a GetFeature request: "
                                                + request.getDocumentElement().getNodeName() );
        }
        if ( null == propertyName ) {
            throw new IllegalArgumentException( "propertyName is required." );
        }
        Element queryElem = (Element) request.getElementsByTagNameNS( Namespaces.WFS, WFS2.QUERY_ELEM ).item( 0 );
        Element filter = request.createElementNS( Namespaces.FES, "Filter" );
        queryElem.appendChild( filter );
        Element predicate = request.createElementNS( Namespaces.FES, "PropertyIsNil" );
        if ( null != nilReason && !nilReason.isEmpty() ) {
            predicate.setAttribute( "nilReason", nilReason );
        }
        if ( negate ) {
            Element not = request.createElementNS( Namespaces.FES, "Not" );
            filter.appendChild( not );
            not.appendChild( predicate );
        } else {
            filter.appendChild( predicate );
        }
        Element valueRef = request.createElementNS( Namespaces.FES, "ValueReference" );
        predicate.appendChild( valueRef );
        String prefix = ( propertyName.getPrefix().length() > 0 ) ? propertyName.getPrefix() : TNS_PREFIX;
        String nsURI = request.lookupNamespaceURI( prefix );
        if ( null == nsURI ) {
            valueRef.setAttribute( XMLConstants.XMLNS_ATTRIBUTE + ":" + prefix, propertyName.getNamespaceURI() );
        }
        valueRef.setTextContent( prefix + ":" + propertyName.getLocalPart() );
    }
}
