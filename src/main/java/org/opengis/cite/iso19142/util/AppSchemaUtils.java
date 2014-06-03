package org.opengis.cite.iso19142.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import org.apache.xerces.xs.XSComplexTypeDefinition;
import org.apache.xerces.xs.XSConstants;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSParticle;
import org.apache.xerces.xs.XSSimpleTypeDefinition;
import org.apache.xerces.xs.XSTypeDefinition;
import org.opengis.cite.iso19136.util.XMLSchemaModelUtils;
import org.opengis.cite.iso19142.Namespaces;

/**
 * Utility methods for accessing and analyzing components of GML application
 * schemas.
 * 
 */
public class AppSchemaUtils {

    /**
     * Produces a list of feature properties where the property value has a type
     * derived from the given (simple or complex) type definition.
     * 
     * @param model
     *            An XSModel object representing an application schema.
     * @param featureTypeName
     *            A qualified feature type name.
     * @param typeDef
     *            A (simple or complex) type definition that characterizes the
     *            property value domain.
     * @return A {@literal List<XSElementDeclaration>} containing matching
     *         feature properties; the list may be empty.
     */
    public static List<XSElementDeclaration> getFeaturePropertiesByType(
            XSModel model, QName featureTypeName, XSTypeDefinition typeDef) {
        XSElementDeclaration elemDecl = model.getElementDeclaration(
                featureTypeName.getLocalPart(),
                featureTypeName.getNamespaceURI());
        XSComplexTypeDefinition featureTypeDef = (XSComplexTypeDefinition) elemDecl
                .getTypeDefinition();
        List<XSElementDeclaration> featureProps = XMLSchemaModelUtils
                .getAllElementsInParticle(featureTypeDef.getParticle());
        removeDeprecatedGMLElements(featureProps, model);
        List<XSElementDeclaration> props = new ArrayList<XSElementDeclaration>();
        // set bit mask to indicate acceptable derivation mechanisms
        short extendOrRestrict = XSConstants.DERIVATION_EXTENSION
                | XSConstants.DERIVATION_RESTRICTION;
        for (XSElementDeclaration featureProp : featureProps) {
            XSTypeDefinition propType = featureProp.getTypeDefinition();
            switch (propType.getTypeCategory()) {
            case XSTypeDefinition.SIMPLE_TYPE:
                if ((typeDef.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE)
                        && propType.derivedFromType(typeDef, extendOrRestrict)) {
                    props.add(featureProp);
                }
                break;
            case XSTypeDefinition.COMPLEX_TYPE:
                if (typeDef.getTypeCategory() == XSTypeDefinition.COMPLEX_TYPE) {
                    // check type of child element(s)
                    XSComplexTypeDefinition complexPropType = (XSComplexTypeDefinition) propType;
                    List<XSElementDeclaration> propValues = XMLSchemaModelUtils
                            .getAllElementsInParticle(complexPropType
                                    .getParticle());
                    for (XSElementDeclaration propValue : propValues) {
                        if (propValue.getTypeDefinition().derivedFromType(
                                typeDef, extendOrRestrict)) {
                            props.add(featureProp);
                        }
                    }
                } else {
                    // complex type may derive from simple type
                    if (propType.derivedFromType(typeDef, extendOrRestrict)) {
                        props.add(featureProp);
                    }
                }
                break;
            }
        }
        if (TestSuiteLogger.isLoggable(Level.FINER)) {
            TestSuiteLogger.log(
                    Level.FINER,
                    new StringBuilder("In feature type defn ")
                            .append(featureTypeDef.getName())
                            .append(", found properties with value of type ")
                            .append(typeDef.getName()).append("\n")
                            .append(props).toString());
        }
        return props;
    }

    /**
     * Produces a list of nillable properties for the specified feature type.
     * The absence of such a property value is explicitly indicated by setting
     * the attribute xsi:nil="true".
     * 
     * @param model
     *            An XSModel object representing an application schema.
     * @param featureTypeName
     *            A qualified feature type name.
     * @return A {@literal List<XSElementDeclaration>} containing elements that
     *         may have nil values.
     */
    public static List<XSElementDeclaration> getNillableProperties(
            XSModel model, QName featureTypeName) {
        List<XSElementDeclaration> featureProps = getAllFeatureProperties(
                model, featureTypeName);
        Iterator<XSElementDeclaration> itr = featureProps.iterator();
        while (itr.hasNext()) {
            XSElementDeclaration prop = itr.next();
            if (!prop.getNillable()) {
                itr.remove();
            }
        }
        return featureProps;
    }

    /**
     * Produces a list of all properties for a specified feature type.
     * 
     * @param model
     *            An XSModel object representing an application schema.
     * @param featureTypeName
     *            A qualified feature type name.
     * @return A {@literal List<XSElementDeclaration>} containing one or more
     *         element declarations defining feature properties.
     */
    public static List<XSElementDeclaration> getAllFeatureProperties(
            XSModel model, QName featureTypeName) {
        XSElementDeclaration elemDecl = model.getElementDeclaration(
                featureTypeName.getLocalPart(),
                featureTypeName.getNamespaceURI());
        XSComplexTypeDefinition featureTypeDef = (XSComplexTypeDefinition) elemDecl
                .getTypeDefinition();
        return XMLSchemaModelUtils.getAllElementsInParticle(featureTypeDef
                .getParticle());
    }

    /**
     * Produces a list of properties for a specified feature type that have
     * either (a) a simple type definition or (b) a complex type definition with
     * a simple content model. The standard GML properties are ignored.
     * 
     * @param model
     *            An XSModel object representing an application schema.
     * @param featureTypeName
     *            A qualified feature type name.
     * @return A {@literal List<XSElementDeclaration>} containing one or more
     *         element declarations defining properties with a simple content
     *         model.
     */
    public static List<XSElementDeclaration> getSimpleFeatureProperties(
            XSModel model, QName featureTypeName) {
        XSElementDeclaration elemDecl = model.getElementDeclaration(
                featureTypeName.getLocalPart(),
                featureTypeName.getNamespaceURI());
        XSComplexTypeDefinition featureTypeDef = (XSComplexTypeDefinition) elemDecl
                .getTypeDefinition();
        List<XSElementDeclaration> props = XMLSchemaModelUtils
                .getAllElementsInParticle(featureTypeDef.getParticle());
        Iterator<XSElementDeclaration> propsItr = props.iterator();
        while (propsItr.hasNext()) {
            XSElementDeclaration prop = propsItr.next();
            if (prop.getNamespace().equals(Namespaces.GML)) {
                propsItr.remove();
                continue;
            }
            if (prop.getTypeDefinition().getTypeCategory() == XSTypeDefinition.COMPLEX_TYPE) {
                XSComplexTypeDefinition typeDef = (XSComplexTypeDefinition) prop
                        .getTypeDefinition();
                if (null == typeDef.getSimpleType()) {
                    propsItr.remove();
                }
            }
        }
        return props;
    }

    /**
     * Produces a list of all required properties for a given feature type.
     * 
     * @param model
     *            An XSModel object representing an application schema.
     * @param featureTypeName
     *            A qualified feature type name.
     * @return A {@literal List<XSElementDeclaration>} defining zero or more
     *         elements which must occur in a valid instance.
     */
    public static List<XSElementDeclaration> getRequiredProperties(
            XSModel model, QName featureTypeName) {
        XSElementDeclaration elemDecl = model.getElementDeclaration(
                featureTypeName.getLocalPart(),
                featureTypeName.getNamespaceURI());
        XSComplexTypeDefinition typeDef = (XSComplexTypeDefinition) elemDecl
                .getTypeDefinition();
        List<XSParticle> particles = XMLSchemaModelUtils
                .getAllElementParticles(typeDef.getParticle());
        List<XSElementDeclaration> requiredElems = new ArrayList<XSElementDeclaration>();
        for (XSParticle particle : particles) {
            if (particle.getMinOccurs() > 0) {
                XSElementDeclaration elem = (XSElementDeclaration) particle
                        .getTerm();
                requiredElems.add(elem);
            }
        }
        return requiredElems;
    }

    /**
     * Removes deprecated GML feature properties from a given list of element
     * declarations. The currently deprecated elements are listed below.
     * 
     * <ul>
     * <li>gml:metaDataProperty</li>
     * <li>gml:location</li>
     * </ul>
     * 
     * @param elemDecls
     *            A List of XSElementDeclaration objects.
     * @param model
     *            An XSModel object representing a GML application schema.
     */
    public static void removeDeprecatedGMLElements(
            List<XSElementDeclaration> elemDecls, XSModel model) {
        XSElementDeclaration elemDecl = model.getElementDeclaration("location",
                Namespaces.GML);
        elemDecls.remove(elemDecl);
        elemDecl = model.getElementDeclaration("metaDataProperty",
                Namespaces.GML);
        elemDecls.remove(elemDecl);
    }

    /**
     * Determines the built-in XML Schema datatype from which the given element
     * declaration is derived. The corresponding type definition may be either a
     * simple type or a complex type with a simple content model.
     * 
     * @param propDecl
     *            An element declaration (simple content).
     * @return A QName specifying an XML Schema datatype.
     */
    public static QName getBuiltInDatatype(XSElementDeclaration propDecl) {
        XSTypeDefinition typeDef = propDecl.getTypeDefinition();
        int builtInType;
        if (typeDef.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE) {
            XSSimpleTypeDefinition simpleType = (XSSimpleTypeDefinition) typeDef;
            builtInType = simpleType.getBuiltInKind();
        } else {
            XSComplexTypeDefinition complexType = (XSComplexTypeDefinition) typeDef;
            builtInType = complexType.getSimpleType().getBuiltInKind();
        }
        QName datatype = null;
        switch (builtInType) {
        case XSConstants.DOUBLE_DT:
            datatype = new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "double");
            break;
        case XSConstants.FLOAT_DT:
            datatype = new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "float");
            break;
        case XSConstants.DECIMAL_DT:
            datatype = new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "decimal");
            break;
        case XSConstants.INTEGER_DT:
            datatype = new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "integer");
            break;
        case XSConstants.STRING_DT:
            datatype = new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "string");
            break;
        case XSConstants.DATETIME_DT:
            datatype = new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "dateTime");
            break;
        case XSConstants.DATE_DT:
            datatype = new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "date");
            break;
        case XSConstants.BOOLEAN_DT:
            datatype = new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI, "boolean");
            break;
        default:
            datatype = new QName(XMLConstants.W3C_XML_SCHEMA_NS_URI,
                    "anySimpleType");
        }
        return datatype;
    }
}
