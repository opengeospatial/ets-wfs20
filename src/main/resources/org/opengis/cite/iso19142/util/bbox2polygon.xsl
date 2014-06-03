<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:gml="http://www.opengis.net/gml/3.2">

  <db:abstract xmlns:db="http://docbook.org/ns/docbook">
    <db:para>Converts gml:Envelope to gml:Polygon (2D).</db:para>
  </db:abstract>

  <xsl:output indent="no" method="xml" omit-xml-declaration="yes" encoding="UTF-8"/>
  <xsl:variable name="SP" select="'&#x20;'"/>

  <xsl:template match="/gml:Envelope">
    <xsl:variable name="lowerCoords" select="tokenize(normalize-space(gml:lowerCorner),'\s')" />
    <xsl:variable name="upperCoords" select="tokenize(normalize-space(gml:upperCorner),'\s')" />
    <gml:Polygon>
      <xsl:attribute name="gml:id">
        <xsl:value-of select="generate-id()" />
      </xsl:attribute>
      <xsl:attribute name="srsName">
        <xsl:value-of select="@srsName" />
      </xsl:attribute>
      <gml:exterior>
        <gml:LinearRing>
          <gml:posList>
            <xsl:value-of select="string-join($lowerCoords, $SP)" />
            <xsl:value-of select="$SP"/>
            <xsl:value-of select="concat($lowerCoords[1], $SP, $upperCoords[2], $SP)" />
            <xsl:value-of select="string-join($upperCoords, $SP)" />
            <xsl:value-of select="$SP"/>
            <xsl:value-of select="concat($upperCoords[1], $SP, $lowerCoords[2], $SP)" />
            <xsl:value-of select="string-join($lowerCoords, $SP)" />
          </gml:posList>
        </gml:LinearRing>
      </gml:exterior>
    </gml:Polygon>
  </xsl:template>

</xsl:stylesheet>
