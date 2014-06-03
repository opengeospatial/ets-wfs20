<?xml version="1.0" encoding="UTF-8"?>
<iso:schema id="SoapFault" schemaVersion="${version}"
  xmlns:iso="http://purl.oclc.org/dsdl/schematron" 
  xml:lang="en"
  queryBinding="xslt2">
  
  <iso:title>Rules for W3C SOAP Fault messages.</iso:title>
  
  <iso:ns prefix="env" uri="http://www.w3.org/2003/05/soap-envelope" />
  <iso:ns prefix="ows" uri="http://www.opengis.net/ows/1.1" />

  <iso:p>This ISO Schematron schema specifies rules for validating the 
  content of W3C SOAP fault messages.</iso:p>
  
  <iso:let name="fault-code" value="'Sender'" />
  <iso:let name="expected-exception-code" value="'InvalidRequest'" />
  
  <iso:phase id="SoapFaultPhase">
    <iso:active pattern="FaultMessagePattern" />
    <iso:active pattern="FaultDetailsPattern" />
  </iso:phase>

  <iso:pattern id="FaultMessagePattern">
    <iso:rule context="/">
      <iso:assert test="env:Envelope" diagnostics="dmesg.root.en" see="http://www.w3.org/TR/soap12-part1/#soapfault">
	  The document element must have [local name] = "Envelope" and [namespace name] = "http://www.w3.org/2003/05/soap-envelope".
      </iso:assert>
    </iso:rule>
    <iso:rule context="/env:Envelope">
      <iso:assert test="env:Body">Missing required env:Body element.</iso:assert>
      <iso:assert test="count(env:Body/*) = 1">Body must have only 1 child element.</iso:assert>
      <iso:assert test="env:Body/env:Fault">Required env:Fault element is missing from env:Body.</iso:assert>
    </iso:rule>
    <iso:rule context="//env:Fault">
      <iso:assert test="env:Code/env:Value">Missing required fault code.</iso:assert>
      <iso:assert test="env:Reason/env:Text">Missing required reason.</iso:assert>
      <iso:assert test="env:Detail/ows:ExceptionReport">Missing detail entry (ows:ExceptionReport)</iso:assert>
    </iso:rule>
  </iso:pattern>
  
  <iso:pattern id="FaultDetailsPattern">
    <iso:p>Incorporates information from OWS service exception reports.</iso:p>
    <iso:rule context="//env:Fault/env:Code">
      <iso:let name="soap.code" value="substring-after(env:Value/text(),':')"/>
      <iso:let name="soap.subcode" value="substring-after(env:Subcode/env:Value/text(),':')"/>
      <iso:assert test="$fault-code = $soap.code" diagnostics="dmsg.soap.code">
	  Expected local name of fault code to be <iso:value-of select="$fault-code"/>.
      </iso:assert>
      <iso:assert test="$expected-exception-code = $soap.subcode" diagnostics="dmsg.soap.subcode">
	  Expected local name of fault subcode to be <iso:value-of select="$expected-exception-code"/>.
      </iso:assert>
    </iso:rule>
    <iso:rule context="//env:Fault/env:Reason">
      <iso:assert test="contains(env:Text/text(),../env:Detail/ows:ExceptionReport/ows:Exception[1]/ows:ExceptionText[1]/text())">
	  Expected explanatory text to include content from ows:ExceptionText. />.
      </iso:assert>
    </iso:rule>
    <iso:rule context="//env:Fault/env:Detail">
      <iso:let name="exception.code" value="ows:ExceptionReport/ows:Exception[1]/@exceptionCode"/>
      <iso:assert test="$expected-exception-code = $exception.code" diagnostics="dmsg.exception.code">
	  Expected reported exception code to be <iso:value-of select="$expected-exception-code"/>.
      </iso:assert>
    </iso:rule>
  </iso:pattern>

  <iso:diagnostics>
    <iso:diagnostic id="dmesg.root.en" xml:lang="en">
    The root element has [local name] = '"<iso:value-of select="local-name(/*[1])"/>" and [namespace name] = "<iso:value-of select="namespace-uri(/*[1])"/>".
    </iso:diagnostic>
    <iso:diagnostic id="dmsg.soap.code" xml:lang="en">
    The reported SOAP fault code is '<iso:value-of select="$soap.code"/>'.
    </iso:diagnostic>
    <iso:diagnostic id="dmsg.soap.subcode" xml:lang="en">
    The reported SOAP fault subcode is '<iso:value-of select="$soap.subcode"/>'.
    </iso:diagnostic>
    <iso:diagnostic id="dmsg.exception.code" xml:lang="en">
    The reported exception code is '<iso:value-of select="$exception.code"/>'.
    </iso:diagnostic>
  </iso:diagnostics>
  
</iso:schema>