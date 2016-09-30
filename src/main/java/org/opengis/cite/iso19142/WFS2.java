package org.opengis.cite.iso19142;

import javax.xml.namespace.QName;

/**
 * Contains various constants pertaining to WFS 2.0 service interfaces as
 * specified in ISO 19142:2010 and related standards.
 * 
 * @see "ISO 19142:2010, Geographic information -- Web Feature Service"
 */
public class WFS2 {

    private WFS2() {
    }

    public static final String NS_URI = "http://www.opengis.net/wfs/2.0";
    public static final String SCHEMA_URI = "http://schemas.opengis.net/wfs/2.0/wfs.xsd";
    public static final String SERVICE_TYPE_CODE = "WFS";
    public static final String VERSION = "2.0";
    public static final String V2_0_0 = "2.0.0";
    public static final String GET_CAPABILITIES = "GetCapabilities";
    public static final String DESCRIBE_FEATURE_TYPE = "DescribeFeatureType";
    public static final String LIST_STORED_QUERIES = "ListStoredQueries";
    public static final String DESC_STORED_QUERIES = "DescribeStoredQueries";
    public static final String GET_FEATURE = "GetFeature";
    public static final String GET_PROP_VALUE = "GetPropertyValue";
    public static final String REQUEST_PARAM = "request";
    public static final String SERVICE_PARAM = "service";
    public static final String VERSION_PARAM = "version";
    public static final String TYPENAMES_PARAM = "typenames";
    public static final String NAMESPACES_PARAM = "namespaces";
    public static final String STOREDQUERY_ID_PARAM = "storedquery_id";
    public static final String ID_PARAM = "id";
    public static final String START_INDEX_PARAM = "startIndex";
    public static final String COUNT_PARAM = "count";
    public static final String SRSNAME_PARAM = "srsName";
    /** Stored query identifier: GetFeatureById */
    public static final String QRY_GET_FEATURE_BY_ID = "http://www.opengis.net/def/query/OGC-WFS/0/GetFeatureById";
    /** Stored query identifier: GetFeatureById (deprecated in v2.0.2 */
    public static final String QRY_GET_FEATURE_BY_ID_URN = "urn:ogc:def:query:OGC-WFS::GetFeatureById";
    /** Service constraint indicating support for HTTP GET method bindings. */
    public static final String KVP_ENC = "KVPEncoding";
    /** Service constraint indicating support for HTTP POST method bindings. */
    public static final String XML_ENC = "XMLEncoding";
    /** Service constraint indicating support for SOAP message bindings. */
    public static final String SOAP_ENC = "SOAPEncoding";
    public static final String SOAP_VERSION = "1.1";
    /** Local name of document element in WFS capabilities document. */
    public static final String WFS_CAPABILITIES = "WFS_Capabilities";
    /** Qualified name of document element in WFS capabilities document. */
    public static final QName QNAME_WFS_CAPABILITIES = new QName(Namespaces.WFS, WFS_CAPABILITIES);
    /** Local name of ad hoc Query element. */
    public static final String QUERY_ELEM = "Query";
    /** Local name of StoredQuery element. */
    public static final String STORED_QRY_ELEM = "StoredQuery";
    /** Local name of StoredQueryId element. */
    public static final String STORED_QRY_ID_ELEM = "StoredQueryId";
    /** Local name of Parameter element in a StoredQuery. */
    public static final String PARAM_ELEM = "Parameter";
    /** Local name of TypeName element in DescribeFeatureType. */
    public static final String TYPENAME_ELEM = "TypeName";
    /** ValueCollection element. */
    public static final String VALUE_COLLECTION = "ValueCollection";
    /** FeatureCollection element. */
    public static final String FEATURE_COLLECTION = "FeatureCollection";
    /** Transaction element. */
    public static final String TRANSACTION = "Transaction";
    /** Update element (Transaction). */
    public static final String UPDATE = "Update";
    /** Insert element (Transaction). */
    public static final String INSERT = "Insert";
    /** Delete element (Transaction). */
    public static final String DELETE = "Delete";
    /** Replace element (Transaction). */
    public static final String REPLACE = "Replace";
    /** Native element (Transaction). */
    public static final String NATIVE = "Native";
    /** TransactionResponse element. */
    public static final String TRANSACTION_RSP = "TransactionResponse";
    /** TransactionResponse/TransactionSummary element. */
    public static final String TRANSACTION_SUMMARY = "TransactionSummary";
    /** TransactionSummary/totalDeleted element. */
    public static final String TOTAL_DEL = "totalDeleted";
    /** Media type for SOAP 1.2 message envelopes (RFC 3902). */
    public static final String APPLICATION_SOAP = "application/soap+xml";
    /**
     * Service constraint corresponding to the 'Basic WFS' conformance class.
     */
    public static final String BASIC_WFS = "ImplementsBasicWFS";
    /**
     * Service constraint corresponding to the 'Locking WFS' conformance class.
     */
    public static final String LOCKING_WFS = "ImplementsLockingWFS";
    /** LockFeature request element. */
    public static final String LOCK_FEATURE = "LockFeature";
    /** LockFeature response element. */
    public static final String LOCK_FEATURE_RSP = "LockFeatureResponse";
    /** GetFeatureWithLock request element. */
    public static final String GET_FEATURE_WITH_LOCK = "GetFeatureWithLock";
    /** CRS: EPSG 4326 (see cl. 7.9.2.4.4) */
    public static final String EPSG_4326 = "urn:ogc:def:crs:EPSG::4326";
    /** Service constraint for 'Transactional WFS' conformance class. */
    public static final String TRX_WFS = "ImplementsTransactionalWFS";
    /** CreateStoredQuery request. */
    public static final String CREATE_STORED_QRY = "CreateStoredQuery";
    /** DropStoredQuery request. */
    public static final String DROP_STORED_QRY = "DropStoredQuery";

    /** VersionState indicates the state of a feature version. */
    public enum VersionState {
        VALID, SUPERSEDED, RETIRED, FUTURE;
        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }
}
