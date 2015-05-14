## WFS 2.0 (ISO 19142:2010) Conformance Test Suite

### Scope

This test suite checks Web Feature Service (WFS) 2.0 implementations for conformance 
to ISO 19142:2010, _Geographic information -- Web Feature Service_ (also published as 
[OGC 09-025r1](http://portal.opengeospatial.org/files/?artifact_id=39967)).
Four fundamental conformance levels are implemented in the test suite:</p>

1. **Simple WFS**: Implements the following operations: `GetCapabilities`, `DescribeFeatureType`, 
`ListStoredQueries`, `DescribeStoredQueries`, and the `GetFeature` operation with at least the 
StoredQuery action (GetFeatureById).
2. **Basic WFS**: As for **Simple WFS**, plus the `GetFeature` operation with the Query 
action and the `GetPropertyValue` operation.
3. **Transactional WFS**: As for **Basic WFS**, plus the `Transaction` operation.
4. **Locking WFS**: As for **Transactional WFS**, plus at least one of the `GetFeatureWithLock` 
or `LockFeature` operations.

The tests for WFS capabilities are supplemented by tests imported from the 
[GML 3.2 test suite](https://github.com/opengeospatial/ets-gml32); these GML 
conformance classes apply to all WFS 2.0 implementations:

* _All GML application schemas_
* _GML application schemas defining features and feature collections_

The WFS 2.0 test suite is schema-aware in the sense that the WFS under test does not 
need to support any particular application schemas or to be loaded with special test 
data. However, the following preconditions must be satisfied:

* The GML application schema meets the requirements of the GML conformance class 
"GML application schemas defining features and feature collections" (ISO 19136, A.1.4).
* Data are available for at least one feature type advertised in the capabilities 
document.
* The service capabilities description contains all required elements in accord 
with the "Simple WFS" conformance class.

Which tests are actually run is determined by the content of the WFS capabilities 
document; in particular, the conformance classes the implementation claims to support. 
There is a service constraint defined for each conformance class, except for the 
mandatory "Simple WFS" conformance class (see ISO 19142, Table 13). The boolean-valued 
service constraints are listed in the OperationsMetadata section of the capabilities 
document as shown below.

    <OperationsMetadata xmlns="http://www.opengis.net/ows/1.1">
      <!-- Operation and common Parameter definitions omitted -->
      <Constraint name="ImplementsBasicWFS">
        <AllowedValues>
          <Value>TRUE</Value>
          <Value>FALSE</Value>
        </AllowedValues>
        <DefaultValue>TRUE</DefaultValue>
      </Constraint>
      <Constraint name="KVPEncoding">
        <AllowedValues>
          <Value>TRUE</Value>
          <Value>FALSE</Value>
        </AllowedValues>
        <DefaultValue>TRUE</DefaultValue>
      </Constraint>
      <Constraint name="XMLEncoding">
        <AllowedValues>
          <Value>TRUE</Value>
          <Value>FALSE</Value>
        </AllowedValues>
        <DefaultValue>TRUE</DefaultValue>
      </Constraint>
    </OperationsMetadata>

Note that several optional conformance classes are not currently covered by the test suite:

* Inheritance
* Remote resolve
* Response paging
* Standard joins
* Spatial joins
* Temporal joins
* Feature versions
* Manage stored queries

Visit the [project documentation website](http://opengeospatial.github.io/ets-wfs20/) 
for more information, including the API documentation.


### How to run the tests

#### Integrated development environment (IDE)
You can use a Java IDE such as Eclipse, NetBeans, or IntelliJ to run the test 
suite. Clone the repository and build the project. The runtime configuration 
is summarized below.

__Main class__: `org.opengis.cite.iso19142.TestNGController`

__Arguments__: The first argument must refer to an XML properties file containing 
the required test run argument (a reference to a KML resource). If not specified, 
the default location at `${user.home}/test-run-props.xml` will be used.
   
You can modify the default settings in the sample [test-run-props.xml](src/main/config/test-run-props.xml) 
file:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
<properties version="1.0">
    <comment>Test run arguments (ets-wfs20)</comment>
	<entry key="wfs">wfs2-capabilities.xml</entry>
</properties>
```

The TestNG results file (testng-results.xml) will be written to a subdirectory 
in ${user.home}/testng/ having a UUID value as its name.

#### Command shell (console)

One of the build artifacts is an "all-in-one" JAR file that includes the test 
suite with all of its dependencies. This makes it very easy to execute the test 
suite in a command shell like so:

`java -jar ets-wfs20-${version}-aio.jar  [test-run-props.xml]`

#### OGC test harness

Use [TEAMengine](https://github.com/opengeospatial/teamengine), the official 
OGC test harness. The latest test suite release should be available at the 
[beta testing facility](http://cite.opengeospatial.org/te2/). You can also 
[build and deploy](https://github.com/opengeospatial/teamengine) the test 
harness yourself and use a local installation.


### How to contribute

If you would like to get involved, you can:

* [Report an issue](https://github.com/opengeospatial/ets-wfs20/issues) such as a defect or an 
enhancement request
* Help to resolve an [open issue](https://github.com/opengeospatial/ets-wfs20/issues?q=is%3Aopen)
* Fix a bug: Fork the repository, apply the fix, and create a pull request
* Add new tests: Fork the repository, implement (and verify) the tests on a new topic branch, 
and create a pull request
