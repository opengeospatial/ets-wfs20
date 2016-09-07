<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:gml="http://www.opengis.net/gml/3.2">

  <db:abstract xmlns:db="http://docbook.org/ns/docbook">
    <db:para>Converts gml:Surface with a single patch to gml:Polygon.</db:para>
  </db:abstract>

  <xsl:output indent="no" method="xml" omit-xml-declaration="yes" encoding="UTF-8"/>

  <!-- identity template -->
  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="gml:Surface">
    <xsl:variable name="patches.count" select="count(gml:patches/gml:PolygonPatch)" />
    <xsl:if test="$patches.count eq 1">
      <gml:Polygon>
        <xsl:attribute name="gml:id">
          <xsl:value-of select="@gml:id" />
        </xsl:attribute>
        <xsl:attribute name="srsName">
          <xsl:value-of select="@srsName" />
        </xsl:attribute>
        <xsl:copy-of select="gml:patches/gml:PolygonPatch/*"/>
      </gml:Polygon>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>
