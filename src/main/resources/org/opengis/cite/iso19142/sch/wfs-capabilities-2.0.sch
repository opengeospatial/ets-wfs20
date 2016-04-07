<?xml version="1.0" encoding="UTF-8"?>
<iso:schema id="wfs-capabilities-2.0" 
  schemaVersion="2.0"
  xmlns:iso="http://purl.oclc.org/dsdl/schematron" 
  xml:lang="en"
  queryBinding="xslt2">

  <iso:title>Constraints on WFS 2.0 service descriptions.</iso:title>

  <iso:ns prefix="ows" uri="http://www.opengis.net/ows/1.1" />
  <iso:ns prefix="wfs" uri="http://www.opengis.net/wfs/2.0" />
  <iso:ns prefix="fes" uri="http://www.opengis.net/fes/2.0" />
  <iso:ns prefix="xlink" uri="http://www.w3.org/1999/xlink" />

  <iso:p>This Schematron (ISO 19757-3) schema specifies constraints regarding 
  the content of WFS 2.0 service capabilities descriptions.</iso:p>

  <iso:phase id="SimpleWFSPhase">
    <iso:active pattern="EssentialCapabilitiesPattern"/>
    <iso:active pattern="TopLevelElementsPattern"/>
    <iso:active pattern="ConformanceStatementPattern"/>
    <iso:active pattern="ServiceIdentificationPattern"/>
    <iso:active pattern="SimpleWFSPattern"/>
  </iso:phase>

  <iso:phase id="BasicWFSPhase">
    <iso:active pattern="BasicWFSPattern"/>
  </iso:phase>

  <iso:phase id="TransactionalWFSPhase">
    <iso:active pattern="TransactionalWFSPattern"/>
  </iso:phase>
  
  <iso:phase id="LockingWFSPhase">
    <iso:active pattern="LockingWFSPattern"/>
  </iso:phase>

  <iso:pattern id="EssentialCapabilitiesPattern">
    <iso:rule context="/">
      <iso:assert test="wfs:WFS_Capabilities" diagnostics="dmsg.root.en">
	  The document element must have [local name] = "WFS_Capabilities" and [namespace name] = "http://www.opengis.net/wfs/2.0.
      </iso:assert>
      <iso:assert test="matches(wfs:WFS_Capabilities/@version, '2\.0\.\d')" diagnostics="dmsg.version.en">
      The capabilities document must have @version = '2.0.[0-9]'.
      </iso:assert>
    </iso:rule>
  </iso:pattern>

  <iso:pattern id="TopLevelElementsPattern">
    <iso:p>Rules regarding the inclusion of common service metadata elements.</iso:p>
    <iso:rule context="/*[1]">
      <iso:assert test="ows:ServiceIdentification">The ows:ServiceIdentification element is missing.</iso:assert>
      <iso:assert test="ows:ServiceProvider">The ows:ServiceProvider element is missing.</iso:assert>
      <iso:assert test="ows:OperationsMetadata">The ows:OperationsMetadata element is missing.</iso:assert>
      <iso:assert test="wfs:FeatureTypeList">The wfs:FeatureTypeList element is missing.</iso:assert>
      <iso:assert test="fes:Filter_Capabilities">The fes:Filter_Capabilities element is missing.</iso:assert>
    </iso:rule>
  </iso:pattern>

  <iso:pattern id="ConformanceStatementPattern">
    <iso:p>Implementation conformance statement. See ISO 19142:2010, cl. 8.3.5.3, Table 13.</iso:p>
    <iso:rule context="//ows:OperationsMetadata">
      <iso:assert test="ows:Constraint[@name='ImplementsBasicWFS']/ows:DefaultValue">
      The service constraint 'ImplementsBasicWFS' has no ows:DefaultValue child.
      </iso:assert>
      <iso:assert test="ows:Constraint[@name='ImplementsTransactionalWFS']/ows:DefaultValue">
      The service constraint 'ImplementsTransactionalWFS' has no ows:DefaultValue child.
      </iso:assert>
      <iso:assert test="ows:Constraint[@name='ImplementsLockingWFS']/ows:DefaultValue">
      The service constraint 'ImplementsLockingWFS' has no ows:DefaultValue child.
      </iso:assert>
      <iso:assert test="ows:Constraint[@name='ImplementsInheritance']/ows:DefaultValue">
      The service constraint 'ImplementsInheritance' has no ows:DefaultValue child.
      </iso:assert>
      <iso:assert test="ows:Constraint[@name='ImplementsRemoteResolve']/ows:DefaultValue">
      The service constraint 'ImplementsRemoteResolve' has no ows:DefaultValue child.
      </iso:assert>
      <iso:assert test="ows:Constraint[@name='ImplementsResultPaging']/ows:DefaultValue">
      The service constraint 'ImplementsResultPaging' has no ows:DefaultValue child.
      </iso:assert>
      <iso:assert test="ows:Constraint[@name='ImplementsStandardJoins']/ows:DefaultValue">
      The service constraint 'ImplementsStandardJoins' has no ows:DefaultValue child.
      </iso:assert>
      <iso:assert test="ows:Constraint[@name='ImplementsSpatialJoins']/ows:DefaultValue">
      The service constraint 'ImplementsSpatialJoins' has no ows:DefaultValue child.
      </iso:assert>
      <iso:assert test="ows:Constraint[@name='ImplementsTemporalJoins']/ows:DefaultValue">
      The service constraint 'ImplementsTemporalJoins' has no ows:DefaultValue child.
      </iso:assert>
      <iso:assert test="ows:Constraint[@name='ImplementsFeatureVersioning']/ows:DefaultValue">
      The service constraint 'ImplementsFeatureVersioning' has no ows:DefaultValue child.
      </iso:assert>
      <iso:assert test="ows:Constraint[@name='ManageStoredQueries']/ows:DefaultValue">
      The service constraint 'ManageStoredQueries' has no ows:DefaultValue child.
      </iso:assert>
    </iso:rule>
  </iso:pattern>

  <iso:pattern id="SimpleWFSPattern">
    <iso:p>Simple WFS conformance class. See ISO 19142:2010, cl. 2, A.1.1.</iso:p>
    <iso:rule context="//ows:OperationsMetadata">
      <iso:assert test="ows:Operation[@name='GetCapabilities']//ows:Get/@xlink:href">
      The mandatory GET method endpoint for GetCapabilities is missing.
      </iso:assert>
      <iso:assert test="ows:Operation[@name='DescribeFeatureType']">
      The mandatory DescribeFeatureType operation is missing.
      </iso:assert>
      <iso:assert test="ows:Operation[@name='ListStoredQueries']">
      The mandatory ListStoredQueries operation is missing.
      </iso:assert>
      <iso:assert test="ows:Operation[@name='DescribeStoredQueries']">
      The mandatory DescribeStoredQueries operation is missing.
      </iso:assert>
      <iso:assert test="ows:Operation[@name='GetFeature']">
      The mandatory GetFeature operation is missing.
      </iso:assert>
    </iso:rule>
    <iso:rule context="ows:Operation[@name='GetFeature']">
      <iso:assert test="exists(index-of(ows:Parameter[@name='resolve']//ows:Value, 'local'))">
        GetFeature: the 'resolve' parameter must contain 'local' as an allowed value.
      </iso:assert>
    </iso:rule>
    <iso:rule context="//fes:Filter_Capabilities/fes:Conformance">
      <iso:assert test="lower-case(fes:Constraint[@name='ImplementsQuery']/ows:DefaultValue) = 'true'">
      The filter constraint 'ImplementsQuery' must be 'true' for all conforming WFS implementations.
      </iso:assert>
    </iso:rule>
  </iso:pattern>

  <iso:pattern id="BasicWFSPattern">
    <iso:p>Basic WFS conformance class. See ISO 19142:2010, cl. 2, A.1.2.</iso:p>
    <iso:rule context="//ows:OperationsMetadata">
      <iso:assert test="lower-case(ows:Constraint[@name='ImplementsBasicWFS']/ows:DefaultValue) = 'true'">
      The service constraint 'ImplementsBasicWFS' must be 'TRUE' for all conforming Basic WFS implementations.
      </iso:assert>
      <iso:assert test="ows:Operation[@name='GetPropertyValue']">
      The mandatory GetPropertyValue operation is missing.
      </iso:assert>
    </iso:rule>
    <iso:rule context="//fes:Filter_Capabilities/fes:Conformance">
      <iso:assert test="fes:Constraint[@name='ImplementsAdHocQuery']/ows:DefaultValue = 'TRUE'">
      The filter constraint 'ImplementsAdHocQuery' must be 'TRUE' for all conforming Basic WFS implementations.
      </iso:assert>
      <iso:assert test="fes:Constraint[@name='ImplementsResourceId']/ows:DefaultValue = 'TRUE'">
      The filter constraint 'ImplementsResourceId' must be 'TRUE' for all conforming Basic WFS implementations.
      </iso:assert>
      <iso:assert test="fes:Constraint[@name='ImplementsMinStandardFilter']/ows:DefaultValue = 'TRUE'">
      The filter constraint 'ImplementsMinStandardFilter' must be 'TRUE' for all conforming Basic WFS implementations.
      </iso:assert>
      <iso:assert test="fes:Constraint[@name='ImplementsStandardFilter']/ows:DefaultValue = 'TRUE'">
      The filter constraint 'ImplementsStandardFilter' must be 'TRUE' for all conforming Basic WFS implementations.
      </iso:assert>
      <iso:assert test="fes:Constraint[@name='ImplementsMinSpatialFilter']/ows:DefaultValue = 'TRUE'">
      The filter constraint 'ImplementsMinSpatialFilter' must be 'TRUE' for all conforming Basic WFS implementations.
      </iso:assert>
      <iso:assert test="fes:Constraint[@name='ImplementsSorting']/ows:DefaultValue = 'TRUE'">
      The filter constraint 'ImplementsSorting' must be 'TRUE' for all conforming Basic WFS implementations.
      </iso:assert>
      <iso:assert test="fes:Constraint[@name='ImplementsMinimumXPath']/ows:DefaultValue = 'TRUE'">
      The filter constraint 'ImplementsMinimumXPath' must be 'TRUE' for all conforming Basic WFS implementations.
      </iso:assert>
    </iso:rule>
  </iso:pattern>

  <iso:pattern id="TransactionalWFSPattern">
    <iso:p>Transactional WFS conformance class. See ISO 19142:2010, cl. 2, A.1.3.</iso:p>
    <iso:rule context="//ows:OperationsMetadata">
      <iso:assert test="ows:Operation[@name='Transaction']">
        The mandatory Transaction operation is missing.
      </iso:assert>
      <iso:assert test="ows:Constraint[@name='ImplementsTransactionalWFS']/ows:DefaultValue = 'TRUE'">
        The service constraint 'ImplementsTransactionalWFS' must be 'TRUE' for all conforming Transactional WFS implementations.
      </iso:assert>
    </iso:rule>
  </iso:pattern>

  <iso:pattern id="LockingWFSPattern">
    <iso:p>Rules for the 'Locking WFS' conformance class. See ISO 19142:2010, cl. 2, A.1.4.</iso:p>
    <iso:rule context="//ows:OperationsMetadata">
      <iso:assert test="ows:Constraint[@name='ImplementsLockingWFS']/ows:DefaultValue = 'TRUE'">
        The service constraint 'ImplementsLockingWFS' must be 'TRUE' for all conforming Locking WFS implementations.
      </iso:assert>
      <iso:assert test="ows:Operation[@name='LockFeature' or @name='GetFeatureWithLock']">
        At least one of LockFeature or GetFeatureWithLock operations must be present.
      </iso:assert>
    </iso:rule>
  </iso:pattern>

  <iso:pattern id="ServiceIdentificationPattern">
    <iso:rule context="//ows:ServiceIdentification">
      <iso:let name="supportedVersions" value="string-join(ows:ServiceTypeVersion,',')"/>
      <iso:assert test="lower-case(ows:ServiceType) = 'wfs'"
        diagnostics="dmsg.serviceType.en"> 
        The value of the ows:ServiceType element must be "wfs" (case-insensitive).
      </iso:assert>
      <iso:assert test="matches($supportedVersions, '2\.0\.\d')" diagnostics="dmsg.serviceTypeVersion.en">
      An ows:ServiceTypeVersion element matching the value '2.0.[0-9]' must be present.
      </iso:assert>
    </iso:rule>
  </iso:pattern>

  <iso:diagnostics>
    <iso:diagnostic id="dmsg.root.en" xml:lang="en">
    The root element has [local name] = '<iso:value-of select="local-name(/*[1])"/>' and [namespace name] = '<iso:value-of select="namespace-uri(/*[1])"/>'.
    </iso:diagnostic>
    <iso:diagnostic id="dmsg.version.en" xml:lang="en">
    The reported version is <iso:value-of select="/*[1]/@version"/>.
    </iso:diagnostic>
    <iso:diagnostic id="dmsg.serviceType.en" xml:lang="en">
    The reported ServiceType is '<iso:value-of select="./ows:ServiceType"/>'.
    </iso:diagnostic>
    <iso:diagnostic id="dmsg.serviceTypeVersion.en" xml:lang="en">
    The supported versions are <iso:value-of select="$supportedVersions"/>.
    </iso:diagnostic>
  </iso:diagnostics>

</iso:schema>