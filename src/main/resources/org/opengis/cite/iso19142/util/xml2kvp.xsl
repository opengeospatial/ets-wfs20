<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="2.0" 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:wfs="http://www.opengis.net/wfs/2.0"
  xmlns:fes="http://www.opengis.net/fes/2.0"
  xmlns:ows="http://www.opengis.net/ows/1.1"
  xmlns:saxon="http://saxon.sf.net/">

  <db:abstract xmlns:db="http://docbook.org/ns/docbook">
    <db:para>Transforms the XML representation of a WFS 2.0 request entity to 
    its corresponding KVP string format.</db:para>
  </db:abstract>

  <xsl:output method="text" />
  <xsl:output name="filter" method="xml" omit-xml-declaration="yes" indent="no" />

  <xsl:variable name="WFS_VERSION" select="'2.0.0'"/>
  <xsl:variable name="XMLNS_SEP" select="','"/>

  <xsl:template match="/wfs:GetCapabilities">
    <xsl:text>service=WFS&amp;request=</xsl:text>
    <xsl:value-of select="local-name()" />
    <xsl:if test="ows:AcceptVersions">
      <xsl:text>&amp;acceptversions=</xsl:text>
      <xsl:value-of select="ows:AcceptVersions/ows:Version" separator="," />
    </xsl:if>
    <xsl:if test="ows:Sections">
      <xsl:text>&amp;sections=</xsl:text>
      <xsl:value-of select="ows:Sections/ows:Section" separator="," />
    </xsl:if>
  </xsl:template>

  <xsl:template match="/wfs:ListStoredQueries">
    <xsl:call-template name="CommonParameters" />
  </xsl:template>

  <xsl:template match="/wfs:DescribeStoredQueries">
    <xsl:call-template name="CommonParameters" />
    <xsl:if test="wfs:StoredQueryId">
      <xsl:text>&amp;storedquery_id=</xsl:text>
      <xsl:value-of select="wfs:StoredQueryId" separator="," />
    </xsl:if>
  </xsl:template>

  <xsl:template match="/wfs:DescribeFeatureType">
    <xsl:call-template name="CommonParameters" />
    <xsl:if test="@outputFormat">
      <xsl:text>&amp;outputformat=</xsl:text>
      <xsl:value-of select="@outputFormat" />
    </xsl:if>
    <xsl:if test="wfs:TypeName">
      <xsl:call-template name="TypeNamesParam">
        <xsl:with-param name="typeNames" select="wfs:TypeName/text()" />
      </xsl:call-template>
      <xsl:call-template name="NamespacesParam">
        <!-- WARNING: ignores in-scope namespaces for wfs:TypeName  -->
        <xsl:with-param name="elem" select="." />
      </xsl:call-template>
    </xsl:if>
  </xsl:template>

  <xsl:template name="GetFeature" match="/wfs:GetFeature">
    <xsl:call-template name="CommonParameters" />
    <xsl:call-template name="PresentationParameters" />
    <xsl:call-template name="ResolveParameters" />
    <xsl:for-each select="wfs:StoredQuery">
      <xsl:call-template name="StoredQuery" />
    </xsl:for-each>
    <xsl:for-each select="wfs:Query">
      <xsl:if test="@srsName">
        <xsl:text>&amp;srsname=</xsl:text>
        <xsl:value-of select="@srsName" />
      </xsl:if>
      <xsl:call-template name="TypeNamesParam">
        <xsl:with-param name="typeNames" select="tokenize(./@typeNames, '\s')" />
      </xsl:call-template>
      <xsl:call-template name="NamespacesParam">
        <xsl:with-param name="elem" select="." />
      </xsl:call-template>
      <xsl:choose>
        <xsl:when test="fes:Filter[fes:ResourceId]">
          <xsl:call-template name="ResourceIdParam">
            <xsl:with-param name="identifiers" select="fes:Filter/fes:ResourceId/@rid" />
          </xsl:call-template>
        </xsl:when>
        <xsl:when test="fes:Filter[not(fes:ResourceId)]">
          <xsl:call-template name="Filter" />
        </xsl:when>
      </xsl:choose>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="ResourceIdParam">
    <xsl:param name="identifiers" as="xs:string*" />
    <xsl:text>&amp;resourceid=</xsl:text>
    <xsl:value-of select="$identifiers" separator="," />
  </xsl:template>

  <xsl:template name="GetFeatureWithLock" match="/wfs:GetFeatureWithLock">
    <xsl:call-template name="GetFeature" />
    <xsl:call-template name="LockParameters" />
  </xsl:template>

  <xsl:template name="LockFeature" match="/wfs:LockFeature">
    <xsl:call-template name="CommonParameters" />
    <xsl:call-template name="LockParameters" />
    <xsl:for-each select="wfs:StoredQuery">
      <xsl:call-template name="StoredQuery" />
    </xsl:for-each>
    <xsl:for-each select="wfs:Query">
      <xsl:if test="@srsName">
        <xsl:text>&amp;srsname=</xsl:text>
        <xsl:value-of select="@srsName" />
      </xsl:if>
      <xsl:call-template name="TypeNamesParam">
        <xsl:with-param name="typeNames" select="tokenize(./@typeNames, '\s')" />
      </xsl:call-template>
      <xsl:call-template name="NamespacesParam">
        <xsl:with-param name="elem" select="." />
      </xsl:call-template>
      <xsl:if test="fes:Filter">
        <xsl:call-template name="Filter" />
      </xsl:if>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="Filter">
    <xsl:variable name="filter" select="normalize-space(saxon:serialize(fes:Filter, 'filter'))" />
    <xsl:text>&amp;filter=</xsl:text>
    <xsl:value-of select="encode-for-uri(replace($filter,'>\s','>'))" />
  </xsl:template>

  <xsl:template match="/wfs:GetPropertyValue">
    <xsl:call-template name="GetFeature" />
    <xsl:text>&amp;valuereference=</xsl:text>
    <xsl:value-of select="@valueReference" />
    <xsl:if test="@resolvePath">
      <xsl:text>&amp;resolvepath=</xsl:text>
      <xsl:value-of select="@resolvePath" />
    </xsl:if>
  </xsl:template>

  <xsl:template name="CommonParameters">
    <xsl:text>service=WFS&amp;version=</xsl:text>
    <xsl:copy-of select="$WFS_VERSION" />
    <xsl:text>&amp;request=</xsl:text>
    <xsl:value-of select="local-name()" />
  </xsl:template>

  <xsl:template name="PresentationParameters">
    <xsl:if test="@count">
      <xsl:text>&amp;count=</xsl:text>
      <xsl:value-of select="@count" />
    </xsl:if>
    <xsl:if test="@startIndex">
      <xsl:text>&amp;startindex=</xsl:text>
      <xsl:value-of select="@startIndex" />
    </xsl:if>
    <xsl:if test="@outputFormat">
      <xsl:text>&amp;outputformat=</xsl:text>
      <xsl:value-of select="@outputFormat" />
    </xsl:if>
    <xsl:if test="@resultType">
      <xsl:text>&amp;resulttype=</xsl:text>
      <xsl:value-of select="@resultType" />
    </xsl:if>
  </xsl:template>

  <xsl:template name="ResolveParameters">
    <xsl:if test="@resolve">
      <xsl:text>&amp;resolve=</xsl:text>
      <xsl:value-of select="@resolve" />
    </xsl:if>
    <xsl:if test="@resolveDepth">
      <xsl:text>&amp;resolvedepth=</xsl:text>
      <xsl:value-of select="@resolveDepth" />
    </xsl:if>
    <xsl:if test="@resolveTimeout">
      <xsl:text>&amp;resolvetimeout=</xsl:text>
      <xsl:value-of select="@resolveTimeout" />
    </xsl:if>
  </xsl:template>

  <xsl:template name="LockParameters">
    <xsl:if test="@expiry">
      <xsl:text>&amp;expiry=</xsl:text>
      <xsl:value-of select="@expiry" />
    </xsl:if>
    <xsl:if test="@lockAction">
      <xsl:text>&amp;lockaction=</xsl:text>
      <xsl:value-of select="@lockAction" />
    </xsl:if>
    <xsl:if test="@lockId">
      <xsl:text>&amp;lockid=</xsl:text>
      <xsl:value-of select="@lockId" />
    </xsl:if>
  </xsl:template>

  <xsl:template name="StoredQuery">
    <xsl:variable name="queryId" select="./@id" />
    <xsl:text>&amp;storedquery_id=</xsl:text>
    <xsl:value-of select="$queryId" />
    <xsl:for-each select="wfs:Parameter">
      <xsl:text>&amp;</xsl:text>
      <xsl:value-of select="@name" />
      <xsl:text>=</xsl:text>
      <!-- WARNING: Ignores complex parameter values -->
      <xsl:copy-of select="text()" />
    </xsl:for-each>
    <!-- Proposed GetFeatureByType stored query -->
    <xsl:if test="ends-with($queryId,'GetFeatureByType')">
      <xsl:call-template name="NamespacesParam">
        <xsl:with-param name="elem" select="*[1]" />
      </xsl:call-template>
    </xsl:if>
  </xsl:template>

  <xsl:template name="TypeNamesParam">
    <xsl:param name="typeNames" as="xs:string*" />
    <xsl:if test="exists($typeNames)">
      <xsl:text>&amp;typenames=</xsl:text>
      <xsl:value-of select="$typeNames" separator="," />
    </xsl:if>
  </xsl:template>

  <xsl:template name="NamespacesParam">
    <!-- Examines $elem for in-scope namespaces -->
    <xsl:param name="elem" as="element()" />
    <xsl:text>&amp;namespaces=</xsl:text>
    <xsl:variable name="prefixes" select="in-scope-prefixes($elem)" />
    <xsl:for-each select="$prefixes">
      <xsl:text>xmlns(</xsl:text>
      <xsl:value-of select="."/>
      <xsl:value-of select="$XMLNS_SEP"/>
      <xsl:value-of select="namespace-uri-for-prefix(.,$elem)"/>
      <xsl:text>)</xsl:text>
      <xsl:if test="(last() gt 1) and (position() ne last())">
        <xsl:text>,</xsl:text>
      </xsl:if>
    </xsl:for-each>
  </xsl:template>

</xsl:stylesheet>
