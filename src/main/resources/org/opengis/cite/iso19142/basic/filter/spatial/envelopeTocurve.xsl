<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:gml="http://www.opengis.net/gml/3.2">

  <db:abstract xmlns:db="http://docbook.org/ns/docbook">
    <db:para>Converts a gml:Envelope to a gml:LineString or gml:Curve. The resulting 
    curve is the diagonal delimited by the two corners of the envelope.</db:para>
  </db:abstract>

  <xsl:output indent="no" method="xml" omit-xml-declaration="yes" encoding="UTF-8"/>
  <xsl:variable name="SP" select="'&#x20;'"/>
  <xsl:param name="curveType">LineString</xsl:param>

  <xsl:template match="/gml:Envelope">
    <xsl:variable name="lowerCoords" select="tokenize(normalize-space(gml:lowerCorner),'\s')" />
    <xsl:variable name="upperCoords" select="tokenize(normalize-space(gml:upperCorner),'\s')" />
    <xsl:choose>
      <xsl:when test="$curveType eq 'LineString'">
        <gml:LineString>
          <xsl:attribute name="gml:id">
            <xsl:value-of select="generate-id()" />
          </xsl:attribute>
          <xsl:attribute name="srsName">
            <xsl:value-of select="@srsName" />
          </xsl:attribute>
          <gml:posList>
            <xsl:value-of select="string-join($lowerCoords, $SP)" />
            <xsl:value-of select="$SP"/>
            <xsl:value-of select="string-join($upperCoords, $SP)" />
          </gml:posList>
        </gml:LineString>
      </xsl:when>
      <xsl:otherwise>
        <gml:Curve>
          <xsl:attribute name="gml:id">
            <xsl:value-of select="generate-id()" />
          </xsl:attribute>
          <xsl:attribute name="srsName">
            <xsl:value-of select="@srsName" />
          </xsl:attribute>
          <gml:segments>
            <gml:LineStringSegment>
              <gml:posList>
                <xsl:value-of select="string-join($lowerCoords, $SP)" />
                <xsl:value-of select="$SP"/>
                <xsl:value-of select="string-join($upperCoords, $SP)" />
              </gml:posList>
            </gml:LineStringSegment>
          </gml:segments>
        </gml:Curve>
      </xsl:otherwise>
    </xsl:choose>

  </xsl:template>

</xsl:stylesheet>
