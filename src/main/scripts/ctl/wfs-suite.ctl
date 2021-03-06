<?xml version="1.0" encoding="UTF-8"?>
<ctl:package xmlns:ctl="http://www.occamlab.com/ctl"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:tns="http://www.opengis.net/cite/iso19142"
  xmlns:saxon="http://saxon.sf.net/"
  xmlns:tec="java:com.occamlab.te.TECore"
  xmlns:tng="java:org.opengis.cite.iso19142.TestNGController">

  <ctl:function name="tns:run-ets-${ets-code}">
    <ctl:param name="testRunArgs">A Document node containing test run arguments (as XML properties).</ctl:param>
    <ctl:param name="outputDir">The directory in which the test results will be written.</ctl:param>
    <ctl:return>The test results as a Source object (root node).</ctl:return>
    <ctl:description>Runs the WFS 2.0 (${version}) test suite.</ctl:description>
    <ctl:code>
      <xsl:variable name="controller" select="tng:new($outputDir)" />
      <xsl:copy-of select="tng:doTestRun($controller, $testRunArgs)" />
    </ctl:code>
  </ctl:function>

  <ctl:suite name="tns:ets-${ets-code}-${version}">
    <ctl:title>WFS 2.0 Conformance Test Suite</ctl:title>
    <ctl:description>Checks WFS 2.0 implementations for conformance to OGC 09-025r2.</ctl:description>
    <ctl:starting-test>tns:Main</ctl:starting-test>
  </ctl:suite>

  <ctl:test name="tns:Main">
    <ctl:assertion>The test subject satisfies all applicable constraints.</ctl:assertion>
    <ctl:code>
      <xsl:variable name="form-data">
        <ctl:form method="POST" width="800" height="600" xmlns="http://www.w3.org/1999/xhtml">
          <h2>WFS 2.0 Conformance Test Suite</h2>
          <div style="background:#F0F8FF" bgcolor="#F0F8FF">
            <p>The WFS implementation under test (IUT) is checked against the following specifications:</p>
            <ul>
              <li style="list-style:square">[<a target="_blank" href="http://docs.opengeospatial.org/is/09-025r2/09-025r2.html">
              OGC 09-025r2</a>] OpenGIS Web Feature Service 2.0 Interface Standard - With Corrigendum, Version 2.0.2</li>
              <li style="list-style:square">[<a target="_blank" href="http://docs.opengeospatial.org/is/09-026r2/09-026r2.html">
              OGC 09-026r2</a>] OGC Filter Encoding 2.0 Standard - With Corrigendum, Version 2.0.2</li>
              <li style="list-style:square">[<a target="_blank" href="http://portal.opengeospatial.org/files/?artifact_id=20509">
              OGC 07-036</a>] OpenGIS Geography Markup Language (GML) Encoding Standard, Version 3.2.1</li>
            </ul>
            <p>Four fundamental conformance levels are defined. The content of the capabilities document 
            will determine which tests are run:</p>
            <ol>
              <li>Simple WFS</li>
              <li>Basic WFS</li>
              <li>Transactional WFS</li>
              <li>Locking WFS</li>
            </ol>
            <p>The test suite is "schema-aware" in the sense that the WFS under test does not need 
            to be loaded with specialized test data. However, the following preconditions must be 
            satisfied:</p>
            <ol>
              <li>The GML application schema meets the requirements of the conformance class concerned with 
            defining features and feature collections (ISO 19136, A.1.4).</li>
              <li>Data are available for at least one feature type advertised in the capabilities document.</li>
            </ol>
          </div>
          <fieldset style="background:#ccffff">
            <legend style="font-family: sans-serif; color: #000099; 
			   background-color:#F0F8FF; border-style: solid; border-width: medium; padding:4px">
			   Implementation under test</legend>
            <p>
              <label for="wfs-uri">
                <h4 style="margin-bottom: 0.5em">Location of WFS capabilities document (http: or file: URI)</h4>
              </label>
              <input id="wfs-uri" name="wfs-uri" size="96" type="text" value="" />
            </p>
            <p>
              <label for="wfs-doc">
                <h4 style="margin-bottom: 0.5em">Upload WFS capabilities document</h4>
              </label>
              <input name="wfs-doc" size="128" type="file" />
            </p>
          </fieldset>
          <p>
            <input class="form-button" type="submit" value="Start"/> | 
            <input class="form-button" type="reset" value="Clear"/>
          </p>
        </ctl:form>
      </xsl:variable>
      <xsl:variable name="wfs-file" select="$form-data//value[@key='wfs-doc']/ctl:file-entry/@full-path" />
      <xsl:variable name="test-run-props">
        <properties version="1.0">
          <entry key="wfs">
            <xsl:choose>
              <xsl:when test="empty($wfs-file)">
                <xsl:value-of select="normalize-space($form-data/values/value[@key='wfs-uri'])"/>
              </xsl:when>
              <xsl:otherwise>
                <xsl:copy-of select="concat('file:///', $wfs-file)" />
              </xsl:otherwise>
            </xsl:choose>
          </entry>
        </properties>
      </xsl:variable>
      <xsl:variable name="testRunDir">
        <xsl:value-of select="tec:getTestRunDirectory($te:core)"/>
      </xsl:variable>
      <xsl:variable name="test-results">
        <ctl:call-function name="tns:run-ets-${ets-code}">
          <ctl:with-param name="testRunArgs" select="$test-run-props"/>
          <ctl:with-param name="outputDir" select="$testRunDir" />
        </ctl:call-function>
      </xsl:variable>
      <xsl:call-template name="tns:testng-report">
        <xsl:with-param name="results" select="$test-results" />
        <xsl:with-param name="outputDir" select="$testRunDir" />
      </xsl:call-template>
      <xsl:variable name="summary-xsl" select="tec:findXMLResource($te:core, '/testng-summary.xsl')" />
      <ctl:message>
        <xsl:value-of select="saxon:transform(saxon:compile-stylesheet($summary-xsl), $test-results)"/>
See detailed test report in the TE_BASE/users/<xsl:value-of 
select="concat(substring-after($testRunDir, 'users/'), '/html/')" /> directory.
      </ctl:message>
      <xsl:if test="xs:integer($test-results/testng-results/@failed) gt 0">
        <xsl:for-each select="$test-results//test-method[@status='FAIL' and @description='prerequisite']">
          <ctl:message>
Test prerequisite <xsl:value-of select="./@name"/> failed: <xsl:value-of select=".//message"/>
          </ctl:message>
        </xsl:for-each>
        <xsl:for-each select="$test-results//test-method[@status='FAIL' and not(@is-config='true')]">
          <ctl:message>
Test method <xsl:value-of select="./@name"/>: <xsl:value-of select=".//message"/>
          </ctl:message>
        </xsl:for-each>
        <ctl:fail/>
      </xsl:if>
	  <xsl:if test="xs:integer($test-results/testng-results/@skipped) eq xs:integer($test-results/testng-results/@total)">
        <ctl:message>All tests were skipped. One or more preconditions were not satisfied.</ctl:message>
        <xsl:for-each select="$test-results//test-method[@status='FAIL' and @is-config='true']">
          <ctl:message>
            <xsl:value-of select="./@name"/>: <xsl:value-of select=".//message"/>
          </ctl:message>
        </xsl:for-each>
        <ctl:skipped />
      </xsl:if>
    </ctl:code>
  </ctl:test>

  <xsl:template name="tns:testng-report">
    <xsl:param name="results" />
    <xsl:param name="outputDir" />
    <xsl:variable name="stylesheet" select="tec:findXMLResource($te:core, '/testng-report.xsl')" />
    <xsl:variable name="reporter" select="saxon:compile-stylesheet($stylesheet)" />
    <xsl:variable name="report-params" as="node()*">
      <xsl:element name="testNgXslt.outputDir">
        <xsl:value-of select="concat($outputDir, '/html')" />
      </xsl:element>
    </xsl:variable>
    <xsl:copy-of select="saxon:transform($reporter, $results, $report-params)" />
  </xsl:template>
</ctl:package>
