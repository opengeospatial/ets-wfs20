package org.opengis.cite.iso19142.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

import org.geotoolkit.temporal.factory.DefaultTemporalFactory;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opengis.cite.iso19142.Namespaces;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.opengis.temporal.TemporalFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class VerifyTimeUtils {

	private static TemporalFactory tmFactory;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@BeforeClass
	public static void initFixture() throws Exception {
		tmFactory = new DefaultTemporalFactory();
	}

	@Test
	public void periodInUTCAsGML() {
		ZonedDateTime t1 = ZonedDateTime.of(2016, 05, 3, 10, 15, 30, 0, ZoneId.of("Z"));
		Instant startPeriod = tmFactory.createInstant(Date.from(t1.minusMonths(1).toInstant()));
		Instant endPeriod = tmFactory.createInstant(Date.from(t1.plusMonths(1).toInstant()));
		Period period = tmFactory.createPeriod(startPeriod, endPeriod);
		Document doc = TimeUtils.periodAsGML(period, null);
		Node endPosition = doc.getElementsByTagNameNS(Namespaces.GML, "endPosition").item(0);
		assertTrue("Expected end date 2016-06-03", endPosition.getTextContent().startsWith("2016-06-03"));
	}

	@Test
	public void periodWithOffsetAsGML() {
		ZonedDateTime t1 = ZonedDateTime.of(2016, 05, 3, 10, 15, 30, 0, ZoneOffset.of("-07:00"));
		Instant startPeriod = tmFactory.createInstant(Date.from(t1.minusMonths(1).toInstant()));
		Instant endPeriod = tmFactory.createInstant(Date.from(t1.plusMonths(1).toInstant()));
		Period period = tmFactory.createPeriod(startPeriod, endPeriod);
		Document doc = TimeUtils.periodAsGML(period, ZoneOffset.of("-07:00"));
		Node beginPosition = doc.getElementsByTagNameNS(Namespaces.GML, "beginPosition").item(0);
		assertTrue("Expected begin time 17:15:30Z", beginPosition.getTextContent().endsWith("17:15:30Z"));
	}

	@Test
	public void instantInUTCAsGML() {
		ZonedDateTime t1 = ZonedDateTime.of(2016, 05, 3, 10, 15, 30, 0, ZoneId.of("Z"));
		Instant instant = tmFactory.createInstant(Date.from(t1.minusMonths(1).toInstant()));
		Document doc = TimeUtils.instantAsGML(instant, ZoneOffset.UTC);
		Node timePosition = doc.getElementsByTagNameNS(Namespaces.GML, "timePosition").item(0);
		assertEquals("Unexpected date-time", timePosition.getTextContent().trim(), "2016-04-03T10:15:30Z");
	}

	@Test
	public void instantWithOffsetAsGML() {
		ZonedDateTime t1 = ZonedDateTime.of(2016, 05, 3, 10, 15, 30, 0, ZoneOffset.of("-07:00"));
		Instant instant = tmFactory.createInstant(Date.from(t1.toInstant()));
		Document doc = TimeUtils.instantAsGML(instant, ZoneOffset.of("-0700"));
		Node timePosition = doc.getElementsByTagNameNS(Namespaces.GML, "timePosition").item(0);
		assertEquals("Unexpected date-time", timePosition.getTextContent().trim(), "2016-05-03T10:15:30-07:00");
	}

	@Test
	public void instantDateTimeWithoutSecond() {
		ZonedDateTime t1 = ZonedDateTime.of(2016, 05, 3, 0, 0, 0, 0, ZoneId.of("Z"));
		Instant instant = tmFactory.createInstant(Date.from(t1.minusMonths(1).toInstant()));
		Document doc = TimeUtils.instantAsGML(instant, ZoneOffset.UTC);
		Node timePosition = doc.getElementsByTagNameNS(Namespaces.GML, "timePosition").item(0);
		assertEquals("Unexpected date-time", timePosition.getTextContent().trim(), "2016-04-03T00:00:00Z");
	}

	@Test
	public void intervalAsGML() {
		ZonedDateTime t1 = ZonedDateTime.of(2016, 05, 3, 10, 20, 30, 0, ZoneId.of("Z"));
		ZonedDateTime t2 = ZonedDateTime.of(2017, 05, 3, 0, 0, 0, 0, ZoneId.of("Z"));
		Document doc = TimeUtils.intervalAsGML(t1, t2);
		Node beginPosition = doc.getElementsByTagNameNS(Namespaces.GML, "beginPosition").item(0);
		Node endPosition = doc.getElementsByTagNameNS(Namespaces.GML, "endPosition").item(0);

		assertEquals("Unexpected date-time", beginPosition.getTextContent().trim(), "2016-05-03T10:20:30Z");
		assertEquals("Unexpected date-time", endPosition.getTextContent().trim(), "2017-05-03T00:00:00Z");
	}

}
