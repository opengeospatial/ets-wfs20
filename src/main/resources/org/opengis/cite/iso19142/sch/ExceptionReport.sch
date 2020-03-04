<?xml version="1.0" encoding="UTF-8"?>
<sch:schema id="ExceptionReport" 
  xmlns:sch="http://purl.oclc.org/dsdl/schematron" 
  xml:lang="en"
  queryBinding="xslt2"
  see="http://portal.opengeospatial.org/files/?artifact_id=20040">

  <sch:title>ISO Schematron schema for WFS 2.0 service exception reports.</sch:title>

  <sch:ns prefix="ows" uri="http://www.opengis.net/ows/1.1" />

  <sch:p>This schema specifies constraints concerning the structure and content 
  of OGC service exception reports (as defined in OGC 06-121r3, cl. 8).</sch:p>

  <sch:phase id="DefaultPhase">
    <sch:active pattern="ExceptionReportPattern" />
  </sch:phase>
  <sch:phase id="MissingParameterValuePhase">
    <sch:active pattern="ExceptionReportPattern" />
    <sch:active pattern="MissingParameterValuePattern" />
  </sch:phase>
  <sch:phase id="InvalidParameterValuePhase">
    <sch:active pattern="ExceptionReportPattern" />
    <sch:active pattern="InvalidParameterValuePattern" />
  </sch:phase>
  <sch:phase id="OperationParsingFailedPhase">
    <sch:active pattern="ExceptionReportPattern" />
    <sch:active pattern="OperationParsingFailedPattern" />
  </sch:phase>

  <sch:let name="version" value="'2.0.0'" />

  <sch:pattern id="ExceptionReportPattern">
    <sch:title>Rules for OWS exception reports.</sch:title>
    <sch:rule context="/">    
      <sch:assert test="//ows:ExceptionReport" 
        diagnostics="msg.root.en">
	Expected exception report with [local name] = "ExceptionReport" and [namespace name] = "http://www.opengis.net/ows/1.1".
      </sch:assert>
      <sch:assert test="//ows:ExceptionReport/@version = $version" 
        diagnostics="msg.version.en">
	The exception report must have @version = '<sch:value-of select="$version"/>'.
      </sch:assert>
    </sch:rule>
    <sch:rule context="//ows:Exception">
      <sch:report test="ows:ExceptionText[not(text())]">
	Found empty ows:ExceptionText element. A detail message should be provided.
      </sch:report>
    </sch:rule>
  </sch:pattern>

  <sch:pattern id="MissingParameterValuePattern">
    <sch:p xml:lang="en">Checks for a MissingParameterValue exception.</sch:p>
    <sch:rule context="//ows:ExceptionReport">
      <sch:assert test="ows:Exception/@exceptionCode = 'MissingParameterValue'"
        diagnostics="msg.code.en">
	The @exceptionCode attribute must have the value 'MissingParameterValue'.
      </sch:assert>
      <sch:assert test="string-length(ows:Exception/@locator) > 0">
	The @locator attribute should provide the name of a missing parameter.
      </sch:assert>
    </sch:rule>
  </sch:pattern>
  
  <sch:pattern id="InvalidParameterValuePattern">
    <sch:p xml:lang="en">Checks for an InvalidParameterValue exception.</sch:p>
    <sch:rule context="//ows:ExceptionReport">
      <sch:assert test="ows:Exception/@exceptionCode = 'InvalidParameterValue'"
        diagnostics="msg.code.en">
	The @exceptionCode attribute must have the value 'InvalidParameterValue'.
      </sch:assert>
      <sch:assert test="string-length(ows:Exception/@locator) > 0">
	The @locator attribute should provide the name of an invalid parameter.
      </sch:assert>
    </sch:rule>
  </sch:pattern>
  
  <sch:pattern id="OperationParsingFailedPattern">
    <sch:p xml:lang="en">Checks for an OperationParsingFailed exception.</sch:p>
    <sch:rule context="//ows:ExceptionReport">
      <sch:assert test="ows:Exception/@exceptionCode = 'OperationParsingFailed'"
        diagnostics="msg.code.en">
	The @exceptionCode attribute must have the value 'OperationParsingFailed'.
      </sch:assert>
      <sch:assert test="string-length(ows:Exception/@locator) > 0">
	The @locator attribute should provide the name of an invalid parameter.
      </sch:assert>
    </sch:rule>
  </sch:pattern>

  <sch:diagnostics>
    <sch:diagnostic id="msg.root.en" xml:lang="en">
    The document element has [local name] = <sch:value-of select="local-name(/*[1])"/> and [namespace name] = <sch:value-of select="namespace-uri(/*[1])"/>.
    </sch:diagnostic>
    <sch:diagnostic id="msg.code.en" xml:lang="en">
    The included exception code is: <sch:value-of select="ows:Exception/@exceptionCode"/>.
    </sch:diagnostic>
    <sch:diagnostic id="msg.locator.en" xml:lang="en">
    The included locator is: <sch:value-of select="ows:Exception/@locator"/>.
    </sch:diagnostic>
    <sch:diagnostic id="msg.version.en" xml:lang="en">
    The reported version is '<sch:value-of select="ows:ExceptionReport/@version"/>'.
    </sch:diagnostic>    
  </sch:diagnostics>
  
</sch:schema>
