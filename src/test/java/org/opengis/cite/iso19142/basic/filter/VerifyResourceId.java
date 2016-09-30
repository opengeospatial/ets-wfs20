package org.opengis.cite.iso19142.basic.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.opengis.cite.iso19142.Namespaces;
import org.w3c.dom.Element;

public class VerifyResourceId {

    @Test
    public void createWithValidTemporalInterval() {
        ResourceId id = new ResourceId("id-01");
        id.setEnd("2016-09-30T11:31:00-07:00");
        id.setStart("2016-07-01T00:00:00-07:00");
        assertNull(id.getPreviousRid());
        assertEquals("2016-07-01T00:00:00-07:00", id.getStart());
    }

    @Test
    public void createWithInvalidTimestamps() {
        ResourceId id = new ResourceId("id-01");
        id.setStart("2016-07-01");
        id.setEnd("2016-09-30");
        assertNull(id.getStart());
    }

    @Test
    public void toElement() {
        ResourceId id = new ResourceId("id-02");
        id.setVersion("1");
        Element idElem = id.toElement();
        assertTrue(idElem.getNamespaceURI().equals(Namespaces.FES));
        assertTrue("Found @previousRid", idElem.getAttribute("previousRid").isEmpty());
    }
}
