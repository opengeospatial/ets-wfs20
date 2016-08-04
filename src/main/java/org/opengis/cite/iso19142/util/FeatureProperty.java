package org.opengis.cite.iso19142.util;

import javax.xml.namespace.QName;

import org.apache.xerces.xs.XSComplexTypeDefinition;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSTypeDefinition;
import org.opengis.cite.iso19136.util.XMLSchemaModelUtils;

/**
 * An immutable description of a feature property. The property type may be
 * simple (e.g. a string) or complex (e.g. a GML geometry element).
 */
final public class FeatureProperty {

    final private QName name;
    final private QName featureType;
    final private QName valueType;
    final private XSElementDeclaration declaration;

    /**
     * Constructor specifying the feature type and property declaration. The
     * property name and value type are derived from the element declaration.
     * 
     * @param featureType
     *            A QName specifying the feature type to which the property
     *            belongs.
     * @param declaration
     *            A schema component representing an element declaration for the
     *            property.
     */
    public FeatureProperty(QName featureType, XSElementDeclaration declaration) {
        this.featureType = featureType;
        this.declaration = declaration;
        this.name = new QName(declaration.getNamespace(), declaration.getName());
        this.valueType = getTypeName(declaration);
    }

    /**
     * Gets the qualified name of the feature property.
     * 
     * @return A QName object.
     */
    public QName getName() {
        return name;
    }

    /**
     * Gets the qualified name of the feature type to which this property
     * belongs.
     * 
     * @return A QName object.
     */
    public QName getFeatureType() {
        return featureType;
    }

    /**
     * Gets the qualified name of the property value type. This is either the
     * name of a simple datatype (e.g. xsd:decimal) or the name of an acceptable
     * child element (e.g. gml:Point).
     * 
     * @return A QName object.
     */
    public QName getValueType() {
        return valueType;
    }

    /**
     * Gets the element declaration for this feature property.
     * 
     * @return A schema component representing an element declaration from an
     *         XML Schema.
     */
    public XSElementDeclaration getDeclaration() {
        return declaration;
    }

    /**
     * Returns the qualified name of the property value type. If the property
     * has a complex type, this is the type name of the expected value. Since
     * the use of a choice compositor is very unconventional in this context (an
     * abstract element is generally preferred), only one element is assumed to
     * appear as an allowed value of a complex type.
     * 
     * @param elementDecl
     *            A schema component representing an element declaration.
     * @return A QName denoting the name of a simple or complex type.
     */
    QName getTypeName(XSElementDeclaration elementDecl) {
        XSTypeDefinition typeDef = elementDecl.getTypeDefinition();
        QName typeName = null;
        if (typeDef.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE) {
            typeName = new QName(typeDef.getNamespace(), typeDef.getName());
        } else {
            XSComplexTypeDefinition complexTypeDef = (XSComplexTypeDefinition) typeDef;
            XSElementDeclaration elemDecl = XMLSchemaModelUtils.getAllElementsInParticle(complexTypeDef.getParticle())
                    .get(0);
            typeName = new QName(elemDecl.getNamespace(), elemDecl.getName());
        }
        return typeName;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append(getFeatureType()).append('/');
        str.append(getName()).append('/');
        str.append(getValueType());
        return str.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((featureType == null) ? 0 : featureType.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((valueType == null) ? 0 : valueType.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FeatureProperty other = (FeatureProperty) obj;
        if (featureType == null) {
            if (other.featureType != null)
                return false;
        } else if (!featureType.equals(other.featureType))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (valueType == null) {
            if (other.valueType != null)
                return false;
        } else if (!valueType.equals(other.valueType))
            return false;
        return true;
    }

}
