<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"  
  xmlns:gml="http://www.opengis.net/gml/3.2" 
  xmlns:ows="http://www.opengis.net/ows/1.1"
  exclude-result-prefixes="ows">

  <db:abstract xmlns:db="http://docbook.org/ns/docbook">
    <db:para>Transforms ows:WGS84BoundingBox to gml:Envelope. The coordinate 
    tuples are reversed since the axis order of the bounding box is lon, lat 
    (urn:ogc:def:crs:OGC:1.3:CRS84).</db:para>
  </db:abstract>

  <xsl:output method="xml" indent="no" omit-xml-declaration="yes" encoding="UTF-8"/>

  <xsl:variable name="EPSG_4326" select="'urn:ogc:def:crs:EPSG::4326'"/>

  <xsl:template match="ows:WGS84BoundingBox">
    <gml:Envelope>
      <xsl:attribute name="srsName">
        <xsl:value-of select="$EPSG_4326" />
      </xsl:attribute>
      <gml:lowerCorner>
        <xsl:value-of select="reverse(tokenize(ows:LowerCorner,'\s'))"/>
      </gml:lowerCorner>
      <gml:upperCorner>
        <xsl:value-of select="reverse(tokenize(ows:UpperCorner,'\s'))"/>
      </gml:upperCorner>
    </gml:Envelope>
  </xsl:template>
</xsl:stylesheet>
