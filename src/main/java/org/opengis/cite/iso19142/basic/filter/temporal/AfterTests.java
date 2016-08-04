package org.opengis.cite.iso19142.basic.filter.temporal;

import java.time.ZoneOffset;
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
import org.opengis.temporal.Instant;
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
 * predicate <em>After</em>. Either operand may represent an instant or a
 * period.
 * 
 * <p>
 * The following figure illustrates the relationship. A solid line denotes a
 * temporal property; a dashed line denotes a literal time value that specifies
 * the temporal extent of interest.
 * </p>
 *
 * <img src="doc-files/after.png" alt="After relationship">
 *
 */
public class AfterTests extends QueryFilterFixture {

    private static final String AFTER_OP = "After";

    /**
     * Checks if the temporal operator "After" is supported. If not, the
     * relevant tests are skipped.
     */
    @BeforeClass
    public void implementsAfterOperator() {
        if (!ServiceMetadataUtils.implementsTemporalOperator(this.wfsMetadata, AFTER_OP)) {
            throw new SkipException(ErrorMessage.format(ErrorMessageKeys.NOT_IMPLEMENTED, "After operator"));
        }
    }

    /**
     * [{@code Test}] Submits a GetFeature request containing the
     * <code>After</code> temporal predicate with a literal gml:TimePeriod
     * value. The response entity must contain only feature instances having a
     * temporal property value that is after the specified period.
     * 
     * @param binding
     *            The ProtocolBinding to use for this request.
     * @param featureType
     *            A QName representing the qualified name of some feature type.
     */
    @Test(description = "See ISO 19143: A.10", dataProvider = "protocol-featureType")
    public void afterPeriod(ProtocolBinding binding, QName featureType) {
        List<XSElementDeclaration> timeProps = findTemporalProperties(featureType);
        if (timeProps.isEmpty()) {
            throw new SkipException("Feature type has no temporal properties: " + featureType);
        }
        XSElementDeclaration timeProperty = timeProps.get(0);
        Period temporalExtent = this.dataSampler.getTemporalExtentOfProperty(this.model, featureType, timeProperty);
        List<Period> subIntervals = TemporalUtils.splitInterval(temporalExtent, 2);
        Period firstSubInterval = subIntervals.get(0);
        Document gmlTimeLiteral = TimeUtils.periodAsGML(firstSubInterval);
        WFSMessage.appendSimpleQuery(this.reqEntity, featureType);
        Element valueRef = WFSMessage.createValueReference(timeProperty);
        WFSMessage.addTemporalPredicate(this.reqEntity, AFTER_OP, gmlTimeLiteral, valueRef);
        ClientResponse rsp = wfsClient.getFeature(new DOMSource(this.reqEntity), binding);
        this.rspEntity = extractBodyAsDocument(rsp);
        Assert.assertEquals(rsp.getStatus(), ClientResponse.Status.OK.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        List<Node> temporalNodes = TemporalQuery.extractTemporalNodes(this.rspEntity, timeProperty, this.model);
        assertAfter(temporalNodes, timeProperty, gmlTimeLiteral);
    }

    /**
     * [{@code Test}] Submits a GetFeature request containing the
     * <code>After</code> temporal predicate with a literal gml:TimeInstant
     * value. The response entity must contain only feature instances having a
     * temporal property value that is after the specified (UTC) instant.
     * 
     * @param binding
     *            The ProtocolBinding to use for this request.
     * @param featureType
     *            A QName representing the qualified name of some feature type.
     */
    @Test(description = "See ISO 19143: A.10", dataProvider = "protocol-featureType")
    public void afterInstant(ProtocolBinding binding, QName featureType) {
        List<XSElementDeclaration> timeProps = findTemporalProperties(featureType);
        if (timeProps.isEmpty()) {
            throw new SkipException("Feature type has no temporal properties: " + featureType);
        }
        XSElementDeclaration timeProperty = timeProps.get(0);
        Period temporalExtent = this.dataSampler.getTemporalExtentOfProperty(this.model, featureType, timeProperty);
        List<Period> subIntervals = TemporalUtils.splitInterval(temporalExtent, 2);
        // end of first sub-interval
        Instant instant = subIntervals.get(0).getEnding();
        Document gmlTimeLiteral = TimeUtils.instantAsGML(instant, ZoneOffset.UTC);
        WFSMessage.appendSimpleQuery(this.reqEntity, featureType);
        Element valueRef = WFSMessage.createValueReference(timeProperty);
        WFSMessage.addTemporalPredicate(this.reqEntity, AFTER_OP, gmlTimeLiteral, valueRef);
        ClientResponse rsp = wfsClient.getFeature(new DOMSource(this.reqEntity), binding);
        this.rspEntity = extractBodyAsDocument(rsp);
        Assert.assertEquals(rsp.getStatus(), ClientResponse.Status.OK.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        List<Node> temporalNodes = TemporalQuery.extractTemporalNodes(this.rspEntity, timeProperty, this.model);
        assertAfter(temporalNodes, timeProperty, gmlTimeLiteral);
    }

    /**
     * [{@code Test}] Submits a GetFeature request containing the
     * <code>After</code> temporal predicate with a literal gml:TimeInstant
     * value that is offset from UTC. The response entity must contain only
     * feature instances having a temporal property value that is after the
     * specified instant.
     * 
     * @param binding
     *            The ProtocolBinding to use for this request.
     * @param featureType
     *            A QName representing the qualified name of some feature type.
     */
    @Test(description = "See ISO 19143: A.10", dataProvider = "protocol-featureType")
    public void afterInstantWithOffset(ProtocolBinding binding, QName featureType) {
        List<XSElementDeclaration> timeProps = findTemporalProperties(featureType);
        if (timeProps.isEmpty()) {
            throw new SkipException("Feature type has no temporal properties: " + featureType);
        }
        XSElementDeclaration timeProperty = timeProps.get(0);
        Period temporalExtent = this.dataSampler.getTemporalExtentOfProperty(this.model, featureType, timeProperty);
        List<Period> subIntervals = TemporalUtils.splitInterval(temporalExtent, 2);
        // end of first sub-interval with UTC offset +09:00 (Japan)
        Instant instant = subIntervals.get(0).getEnding();
        Document gmlTimeLiteral = TimeUtils.instantAsGML(instant, ZoneOffset.ofHours(9));
        WFSMessage.appendSimpleQuery(this.reqEntity, featureType);
        Element valueRef = WFSMessage.createValueReference(timeProperty);
        WFSMessage.addTemporalPredicate(this.reqEntity, AFTER_OP, gmlTimeLiteral, valueRef);
        ClientResponse rsp = wfsClient.getFeature(new DOMSource(this.reqEntity), binding);
        this.rspEntity = extractBodyAsDocument(rsp);
        Assert.assertEquals(rsp.getStatus(), ClientResponse.Status.OK.getStatusCode(),
                ErrorMessage.get(ErrorMessageKeys.UNEXPECTED_STATUS));
        List<Node> temporalNodes = TemporalQuery.extractTemporalNodes(this.rspEntity, timeProperty, this.model);
        assertAfter(temporalNodes, timeProperty, gmlTimeLiteral);
    }

    /**
     * Asserts that all temporal values in the given list occur after the
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
    void assertAfter(List<Node> temporalNodes, XSElementDeclaration propertyDecl, Document gmlTimeLiteral) {
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
            TemporalUtils.assertTemporalRelation(RelativePosition.AFTER, t1, t2);
        }
    }
}
