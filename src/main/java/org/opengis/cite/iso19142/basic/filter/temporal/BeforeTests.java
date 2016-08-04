package org.opengis.cite.iso19142.basic.filter.temporal;

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
import org.opengis.cite.iso19142.util.ServiceMetadataUtils;
import org.opengis.cite.iso19142.util.TimeUtils;
import org.opengis.cite.iso19142.util.WFSMessage;
import org.opengis.temporal.Period;
import org.opengis.temporal.RelativePosition;
import org.opengis.temporal.TemporalGeometricPrimitive;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.sun.jersey.api.client.ClientResponse;

/**
 * Tests the response to a GetFeature request that includes the temporal
 * predicate <em>Before</em>. Either operand may represent an instant or a
 * period.
 *
 * <p>
 * The following figure illustrates the relationship. A solid line denotes a
 * temporal property; a dashed line denotes a literal time value that specifies
 * the temporal extent of interest.
 * </p>
 *
 * <img src="doc-files/before.png" alt="Before relationship">
 */
public class BeforeTests extends QueryFilterFixture {

    private static final String BEFORE_OP = "Before";

    /**
     * Checks if the temporal operator "Before" is supported. If not, the
     * relevant tests are skipped.
     */
    @BeforeClass
    public void implementsBeforeOperator() {
        if (!ServiceMetadataUtils.implementsTemporalOperator(this.wfsMetadata, BEFORE_OP)) {
            throw new SkipException(ErrorMessage.format(ErrorMessageKeys.NOT_IMPLEMENTED, "Before operator"));
        }
    }

    /**
     * [{@code Test}] Submits a GetFeature request containing the
     * <code>Before</code> temporal predicate with a literal gml:TimePeriod
     * value. The response entity must contain only feature instances having a
     * temporal property value that is before the specified period.
     * 
     * @param binding
     *            The ProtocolBinding to use for this request.
     * @param featureType
     *            A QName representing the qualified name of some feature type.
     */
    @Test(description = "See ISO 19143: A.10", dataProvider = "protocol-featureType")
    public void beforePeriod(ProtocolBinding binding, QName featureType) {
        List<XSElementDeclaration> timeProps = findTemporalProperties(featureType);
        if (timeProps.isEmpty()) {
            throw new SkipException("Feature type has no temporal properties: " + featureType);
        }
        XSElementDeclaration timeProperty = timeProps.get(0);
        Period temporalExtent = this.dataSampler.getTemporalExtentOfProperty(this.model, featureType, timeProperty);
        List<Period> subIntervals = TemporalUtils.splitInterval(temporalExtent, 2);
        Period lastSubInterval = subIntervals.get(1);
        Document gmlTimeLiteral = TimeUtils.periodAsGML(lastSubInterval);
        WFSMessage.appendSimpleQuery(this.reqEntity, featureType);
        Element valueRef = WFSMessage.createValueReference(timeProperty);
        WFSMessage.addTemporalPredicate(this.reqEntity, BEFORE_OP, gmlTimeLiteral, valueRef);
        ClientResponse rsp = wfsClient.getFeature(new DOMSource(this.reqEntity), binding);
        this.rspEntity = extractBodyAsDocument(rsp);
        Assert.assertEquals(rsp.getStatus(), ClientResponse.Status.OK.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        List<Node> temporalNodes = TemporalQuery.extractTemporalNodes(this.rspEntity, timeProperty, this.model);
        assertBefore(temporalNodes, timeProperty, gmlTimeLiteral);
    }

    /**
     * Asserts that all temporal values in the given list occur before the
     * specified GML temporal value.
     * 
     * @param temporalNodes
     *            A list of simple or complex temporal values.
     * @param propertyDecl
     *            An element declaration for a temporal property.
     * @param gmlTimeLiteral
     *            A document that contains a GML representation of an instant or
     *            period.
     */
    void assertBefore(List<Node> temporalNodes, XSElementDeclaration propertyDecl, Document gmlTimeLiteral) {
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
            TemporalUtils.assertTemporalRelation(RelativePosition.BEFORE, t1, t2);
        }
    }
}
