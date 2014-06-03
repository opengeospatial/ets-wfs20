package org.opengis.cite.iso19142.locking;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.cite.iso19142.FeatureTypeInfo;
import org.opengis.cite.iso19142.util.WFSClient;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class VerifyLockFeatureTests {

    private static final QName RIVER = new QName("http://example.org/cities",
            "River");
    private static WFSClient mockClient;
    private static DocumentBuilder docBuilder;

    @BeforeClass
    public static void initFixture() throws ParserConfigurationException {
        mockClient = mock(WFSClient.class);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        docBuilder = dbf.newDocumentBuilder();
    }

    @Test
    public void selectAvailableFeatureTypeIsNull() {
        FeatureTypeInfo typeInfo = new FeatureTypeInfo();
        typeInfo.setTypeName(RIVER);
        typeInfo.setInstantiated(false);
        Map<QName, FeatureTypeInfo> featureInfo = new HashMap<QName, FeatureTypeInfo>();
        featureInfo.put(typeInfo.getTypeName(), typeInfo);
        QName typeName = LockFeatureTests.selectRandomFeatureType(featureInfo);
        assertNull(typeName);
    }

    @Test
    public void selectAvailableFeatureTypeIsNotNull() {
        FeatureTypeInfo typeInfo = new FeatureTypeInfo();
        typeInfo.setTypeName(RIVER);
        typeInfo.setInstantiated(true);
        Map<QName, FeatureTypeInfo> featureInfo = new HashMap<QName, FeatureTypeInfo>();
        featureInfo.put(typeInfo.getTypeName(), typeInfo);
        QName typeName = LockFeatureTests.selectRandomFeatureType(featureInfo);
        assertNotNull(typeName);
        assertEquals("Unexpected namespace name.", RIVER.getNamespaceURI(),
                typeName.getNamespaceURI());
    }

    @Test
    public void fetchRandomFeatureIdentifier_isEmpty() {
        FeatureTypeInfo typeInfo = new FeatureTypeInfo();
        typeInfo.setTypeName(RIVER);
        typeInfo.setInstantiated(false);
        Map<QName, FeatureTypeInfo> featureInfo = new HashMap<QName, FeatureTypeInfo>();
        featureInfo.put(typeInfo.getTypeName(), typeInfo);
        LockFeatureTests iut = new LockFeatureTests();
        Map<String, QName> feature = iut
                .fetchRandomFeatureIdentifier(featureInfo);
        assertTrue(feature.isEmpty());
    }

    @Test
    public void fetchRandomFeatureIdentifier_notEmpty() throws SAXException,
            IOException {
        FeatureTypeInfo typeInfo = new FeatureTypeInfo();
        typeInfo.setTypeName(RIVER);
        typeInfo.setInstantiated(true);
        Map<QName, FeatureTypeInfo> featureInfo = new HashMap<QName, FeatureTypeInfo>();
        featureInfo.put(typeInfo.getTypeName(), typeInfo);
        Document rspEntity = docBuilder
                .parse(this.getClass().getResourceAsStream(
                        "/GetFeature/FeatureCollection-River.xml"));
        when(mockClient.getFeatureByType(RIVER, 10, null))
                .thenReturn(rspEntity);
        LockFeatureTests iut = new LockFeatureTests();
        iut.setWfsClient(mockClient);
        Map<String, QName> feature = iut
                .fetchRandomFeatureIdentifier(featureInfo);
        assertEquals("Unexpected number of identifiers.", 1, feature.size());
        String featureId = feature.keySet().iterator().next();
        assertTrue(featureId.matches("River\\.\\d"));
    }
}
