package org.opengis.cite.iso19142.basic.filter.temporal;

import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;

import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSTypeDefinition;
import org.opengis.cite.geomatics.gml.GmlUtils;
import org.opengis.cite.geomatics.time.TemporalUtils;
import org.opengis.cite.iso19142.ErrorMessage;
import org.opengis.cite.iso19142.ErrorMessageKeys;
import org.opengis.cite.iso19142.ProtocolBinding;
import org.opengis.cite.iso19142.basic.filter.QueryFilterFixture;
import org.opengis.cite.iso19142.util.TimeUtils;
import org.opengis.cite.iso19142.util.WFSMessage;
import org.opengis.temporal.Period;
import org.opengis.temporal.RelativePosition;
import org.opengis.temporal.TemporalGeometricPrimitive;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.sun.jersey.api.client.ClientResponse;

/**
 * Tests the response to a GetFeature request that includes the temporal
 * predicate <em>During</em>. The relation can be expressed as follows when
 * comparing a temporal instant to a temporal period:
 * 
 * <pre>
 * self.position &gt; other.begin.position AND self.position &lt; other.end.position
 * </pre>
 * 
 * <p>
 * If both operands are periods then the following must hold:
 * </p>
 * 
 * <pre>
 * self.begin.position &gt; other.begin.position AND self.end.position &lt; other.end.position
 * </pre>
 * 
 * <p>
 * The following figure illustrates the relationship. A solid line denotes a
 * temporal property; a dashed line denotes a literal time value that specifies
 * the temporal extent of interest.
 * </p>
 *
 * <img src="doc-files/during.png" alt="During relationship">
 *
 * <p style="margin-bottom: 0.5em">
 * <strong>Sources</strong>
 * </p>
 * <ul>
 * <li>ISO 19108, 5.2.3.5: TM_RelativePosition</li>
 * </ul>
 */
public class DuringTests extends QueryFilterFixture {

    private static final String DURING_OP = "During";

    /**
     * [{@code Test}] Submits a GetFeature request containing a During temporal
     * predicate with a gml:TimePeriod operand spanning some time interval. The
     * response entity must contain only instances of the requested type that
     * satisfy the temporal relation.
     * 
     * @param binding
     *            The ProtocolBinding to use for this request.
     * @param featureType
     *            A QName representing the qualified name of some feature type.
     */
    @Test(description = "See ISO 19143: 7.14.6, A.9", dataProvider = "protocol-featureType")
    public void duringPeriod(ProtocolBinding binding, QName featureType) {
        List<XSElementDeclaration> timeProps = findTemporalProperties(featureType);
        if (timeProps.isEmpty()) {
            throw new SkipException("Feature type has no temporal properties: " + featureType);
        }
        Period temporalExtent = null;
        XSElementDeclaration tmProperty;
        Iterator<XSElementDeclaration> propsItr = timeProps.iterator();
        do {
            tmProperty = propsItr.next();
            temporalExtent = this.dataSampler.getTemporalExtentOfProperty(this.model, featureType, tmProperty);
            if (null != temporalExtent) {
                break;
            }
        } while (propsItr.hasNext());
        Document gmlTimeLiteral = TimeUtils.periodAsGML(temporalExtent);
        WFSMessage.appendSimpleQuery(this.reqEntity, featureType);
        Element valueRef = WFSMessage.createValueReference(tmProperty);
        WFSMessage.addTemporalPredicate(this.reqEntity, DURING_OP, gmlTimeLiteral, valueRef);
        ClientResponse rsp = wfsClient.getFeature(new DOMSource(reqEntity), binding);
        this.rspEntity = extractBodyAsDocument(rsp);
        Assert.assertEquals(rsp.getStatus(), ClientResponse.Status.OK.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        List<Node> temporalNodes = TemporalQuery.extractTemporalNodes(this.rspEntity, tmProperty, this.model);
        assertDuring(temporalNodes, tmProperty, gmlTimeLiteral);
    }

    /**
     * Asserts that all temporal values in the given list occur during the
     * specified GML temporal value (gml:TimePeriod).
     * 
     * @param temporalNodes
     *            A list of simple or complex temporal values.
     * @param propertyDecl
     *            An element declaration for a temporal property.
     * @param gmlTimeLiteral
     *            A document that contains a GML representation of a period.
     */
    void assertDuring(List<Node> temporalNodes, XSElementDeclaration propertyDecl, Document gmlTimeLiteral) {
        Assert.assertFalse(temporalNodes.isEmpty(),
                String.format("No temporal values found in results: property is %s.", propertyDecl));
        TemporalGeometricPrimitive t2 = GmlUtils.gmlToTemporalGeometricPrimitive(gmlTimeLiteral.getDocumentElement());
        XSTypeDefinition typeDef = propertyDecl.getTypeDefinition();
        for (Node timeNode : temporalNodes) {
            TemporalGeometricPrimitive t1 = null;
            if (typeDef.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE) {
                t1 = TemporalQuery.parseTemporalValue(timeNode.getTextContent(), typeDef);
            } else {
                t1 = GmlUtils.gmlToTemporalGeometricPrimitive((Element) timeNode);
            }
            TemporalUtils.assertTemporalRelation(RelativePosition.DURING, t1, t2);
        }
    }

}
