## WFS 2.0 Conformance Test Suite

### Scope

This test suite verifies that a Web Feature Service (WFS) 2.0 implementation conforms 
to [OGC 09-025r2](http://docs.opengeospatial.org/is/09-025r2/09-025r2.html)) and related 
standards. Tests exist for the conformance classes listed below:</p>

- **Simple WFS**: Implements the following operations: `GetCapabilities`, `DescribeFeatureType`, 
`ListStoredQueries`, `DescribeStoredQueries`, and the `GetFeature` operation with at least the 
StoredQuery action (GetFeatureById).
- **Basic WFS**: As for **Simple WFS**, plus the `GetFeature` operation with the Query 
action and the `GetPropertyValue` operation.
- **Transactional WFS**: As for **Basic WFS**, plus the `Transaction` operation.
- **Locking WFS**: As for **Transactional WFS**, plus at least one of the `GetFeatureWithLock` 
or `LockFeature` operations.
- **HTTP GET**
- **HTTP POST**
- **SOAP**
- **Response paging**
- **Manage stored queries**
- **Feature versions**
- **Standard joins**
- **Spatial joins**
- **Temporal joins**


The tests for WFS capabilities are supplemented by tests imported from the 
[GML 3.2 test suite](https://github.com/opengeospatial/ets-gml32); these GML 
conformance classes apply to all WFS 2.0 implementations:

- _All GML application schemas_
- _GML application schemas defining features and feature collections_

The WFS 2.0 test suite is schema-aware in the sense that the WFS under test does not 
need to support any particular application schemas or to be pre-loaded with special test 
data. However, the following preconditions must be satisfied:

* The GML application schema meets the requirements of the GML conformance class 
"GML application schemas defining features and feature collections" (ISO 19136, A.1.4).
* Data are available for at least one feature type listed in the capabilities 
document.
* The service capabilities description contains all required elements in accord 
with the "Simple WFS" conformance class.

Which tests are actually executed is determined by the content of the WFS capabilities 
document that is submitted; in particular, the test run is driven by the conformance 
classes that the implementation under test (IUT) claims to support. There is a service constraint 
defined for each conformance class, except for the mandatory "Simple WFS" conformance class 
(see OGC 09-025r2, Table 13). The boolean-valued service constraints are listed in the 
OperationsMetadata section of the capabilities document as shown below.

    <OperationsMetadata xmlns="http://www.opengis.net/ows/1.1">
      <!-- Operation and common Parameter definitions are omitted -->
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

Some optional conformance classes are not currently covered by the test suite:

* Inheritance
* Remote resolve

Visit the [project documentation website](http://opengeospatial.github.io/ets-wfs20/) 
for more information, including the API documentation.


### How to run the tests

#### Integrated development environment (IDE)
You can use a Java IDE such as Eclipse, NetBeans, or IntelliJ to run the test suite. 
Clone the repository and build the project. The runtime configuration is summarized below.

__Main class__: `org.opengis.cite.iso19142.TestNGController`

__Arguments__: The first argument must refer to an XML properties file containing the 
required test run argument (a reference to a WFS 2.0 capabilities document). If not 
specified, the default location at `${user.home}/test-run-props.xml` will be used.

You can modify the default settings in the sample [test-run-props.xml](src/main/config/test-run-props.xml) 
file. The value of the `wfs` argument must be an absolute URI that adheres to the 'http' 
or 'file' schemes.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE properties SYSTEM "http://java.sun.com/dtd/properties.dtd">
<properties version="1.0">
    <comment>Test run arguments (ets-wfs20)</comment>
	<entry key="wfs">http://localhost:9090/wfs2/capabilities.xml</entry>
</properties>
```

The TestNG results file (testng-results.xml) will be written to a subdirectory 
in ${user.home}/testng/ having a UUID value as its name.

#### Command shell (terminal)

One of the build artifacts is an "all-in-one" JAR file that includes the test 
suite with all of its dependencies. This makes it very easy to execute the test 
suite in a command shell like so:

`java -jar ets-wfs20-${version}-aio.jar  [test-run-props.xml]`

#### Docker

This test suite comes with a Dockerfile which can be used to easily setup the OGC test harness with
the test suite. Details can be found on [Create Docker Image and create and start Docker Container](https://github.com/opengeospatial/cite/wiki/How-to-create-Docker-Images-of-test-suites#create-docker-image-and-create-and-start-docker-container).

#### OGC test harness

Use [TEAM Engine](https://github.com/opengeospatial/teamengine), the official 
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
* PR template
