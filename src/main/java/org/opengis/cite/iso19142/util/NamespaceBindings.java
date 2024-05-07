package org.opengis.cite.iso19142.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

import org.opengis.cite.iso19142.Namespaces;

/**
 * Provides namespace bindings for evaluating XPath 1.0 expressions using the
 * JAXP XPath API. A namespace name (URI) may be bound to only one prefix.
 */
public class NamespaceBindings implements NamespaceContext {

    private Map<String, String> bindings = new HashMap<String, String>();

    @Override
    public String getNamespaceURI(String prefix) {
        String nsName = null;
        for (Map.Entry<String, String> binding : bindings.entrySet()) {
            if (binding.getValue().equals(prefix)) {
                nsName = binding.getKey();
                break;
            }
        }
        return nsName;
    }

    @Override
    public String getPrefix(String namespaceURI) {
        return bindings.get(namespaceURI);
    }

    @Override
    public Iterator<String> getPrefixes(String namespaceURI) {
        return Arrays.asList(getPrefix(namespaceURI)).iterator();
    }

    /**
     * Adds a namespace binding that associates a namespace name with a prefix.
     * If a binding for a given namespace name already exists it will be
     * replaced.
     * 
     * @param namespaceURI
     *            A String denoting a namespace name (an absolute URI value).
     * @param prefix
     *            A prefix associated with the namespace name.
     */
    public void addNamespaceBinding(String namespaceURI, String prefix) {
        bindings.put(namespaceURI, prefix);
    }

    /**
     * Adds all of the supplied namespace bindings to the existing set of
     * entries.
     * 
     * @param nsBindings
     *            A Map containing a collection of namespace bindings where the
     *            key is an absolute URI specifying the namespace name and the
     *            value denotes the associated prefix.
     */
    public void addAllBindings(Map<String, String> nsBindings) {
        if (null != nsBindings)
            bindings.putAll(nsBindings);
    }

    /**
     * Returns an unmodifiable view of the declared namespace bindings.
     * 
     * @return An immutable Map containing zero or more namespace bindings where
     *         the key is an absolute URI specifying the namespace name and the
     *         value is the associated prefix.
     */
    public Map<String, String> getAllBindings() {
        return Collections.unmodifiableMap(this.bindings);
    }

    /**
     * Creates a NamespaceBindings object that declares the following namespace
     * bindings:
     * 
     * <ul>
     * <li>wfs: {@value org.opengis.cite.iso19142.Namespaces#WFS}</li>
     * <li>fes: {@value org.opengis.cite.iso19142.Namespaces#FES}</li>
     * <li>ows: {@value org.opengis.cite.iso19142.Namespaces#OWS}</li>
     * <li>xlink: {@value org.opengis.cite.iso19142.Namespaces#XLINK}</li>
     * <li>gml: {@value org.opengis.cite.iso19142.Namespaces#GML}</li>
     * <li>soap: {@value org.opengis.cite.iso19142.Namespaces#SOAP_ENV}</li>
     * <li>soap11: {@value org.opengis.cite.iso19142.Namespaces#SOAP11}</li>
     * <li>xsi: {@value javax.xml.XMLConstants#W3C_XML_SCHEMA_INSTANCE_NS_URI}</li>
     * </ul>
     * 
     * @return A NamespaceBindings object.
     */
    public static NamespaceBindings withStandardBindings() {
        NamespaceBindings nsBindings = new NamespaceBindings();
        nsBindings.addNamespaceBinding(Namespaces.WFS, "wfs");
        nsBindings.addNamespaceBinding(Namespaces.FES, "fes");
        nsBindings.addNamespaceBinding(Namespaces.OWS, "ows");
        nsBindings.addNamespaceBinding(Namespaces.XLINK, "xlink");
        nsBindings.addNamespaceBinding(Namespaces.GML, "gml");
        nsBindings.addNamespaceBinding(Namespaces.SOAP_ENV, "soap");
        nsBindings.addNamespaceBinding(Namespaces.SOAP11, "soap11");
        nsBindings.addNamespaceBinding(
                XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "xsi");
        return nsBindings;
    }

    @Override
    public String toString() {
        return "NamespaceBindings:\n" + bindings;
    }
}
