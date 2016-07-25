package org.opengis.cite.iso19142.basic.filter.temporal;

import java.util.List;

import org.apache.xerces.xs.XSTypeDefinition;
import org.opengis.cite.geomatics.gml.GmlUtils;
import org.opengis.cite.geomatics.time.TemporalUtils;
import org.opengis.cite.iso19142.ErrorMessage;
import org.opengis.cite.iso19142.ErrorMessageKeys;
import org.opengis.cite.iso19142.basic.filter.QueryFilterFixture;
import org.opengis.cite.iso19142.util.ServiceMetadataUtils;
import org.opengis.temporal.RelativePosition;
import org.opengis.temporal.TemporalGeometricPrimitive;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

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
     * Asserts that all temporal values in the given list occur before the
     * specified GML temporal value.
     * 
     * @param temporalNodes
     *            A list of simple or complex temporal values.
     * @param typeDef
     *            The relevant definition of the temporal property type.
     * @param gmlTimeLiteral
     *            A document that contains a GML representation of an instant or
     *            period.
     */
    void assertBefore(List<Node> temporalNodes, XSTypeDefinition typeDef, Document gmlTimeLiteral) {
        TemporalGeometricPrimitive t2 = GmlUtils.gmlToTemporalGeometricPrimitive(gmlTimeLiteral.getDocumentElement());
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
