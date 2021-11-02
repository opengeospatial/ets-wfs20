package org.opengis.cite.iso19142.basic.filter.temporal;

import java.util.List;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.xerces.xs.XSElementDeclaration;
import org.opengis.cite.iso19142.basic.filter.QueryFilterFixture;
import org.opengis.temporal.Period;
import org.testng.SkipException;

/**
 * @author <a href="mailto:goltz@lat-lon.de">Lyn Goltz </a>
 */
public abstract class AbstractTemporalTest extends QueryFilterFixture {

    private static final Logger LOGR = Logger.getLogger( AbstractTemporalTest.class.getPackage().getName() );

    public TemporalProperty findTemporalProperty( QName featureType ) {
        List<XSElementDeclaration> temporalProperties = findTemporalProperties( featureType );
        if ( temporalProperties.isEmpty() ) {
            throw new SkipException( "Feature type has no temporal properties: " + featureType );
        }

        TemporalProperty temporalExtent = findTemporalExtent( featureType, temporalProperties );
        if ( temporalExtent == null )
            throw new SkipException(
                                     "Feature type + "
                                                             + featureType
                                                             + " has at least one temporal properties but an extent could not be calculated (e.g. all properties are nill). " );
        return temporalExtent;
    }

    private TemporalProperty findTemporalExtent( QName featureType, List<XSElementDeclaration> temporalProperties ) {
        for ( XSElementDeclaration temporalProp : temporalProperties ) {
            try {
                Period temporalExtent = this.dataSampler.getTemporalExtentOfProperty(getModel(), featureType,
                                                                                      temporalProp);
                if ( temporalExtent != null )
                    return new TemporalProperty( temporalProp, temporalExtent );
            } catch ( Exception e ) {
                LOGR.warning( "Could not calculate the extent of the temporal property " + temporalProp
                              + " of the feature type " + featureType );
            }
        }
        return null;
    }

    class TemporalProperty {

        private XSElementDeclaration property;

        private Period extent;

        public TemporalProperty( XSElementDeclaration property, Period extent ) {
            this.property = property;
            this.extent = extent;
        }

        public XSElementDeclaration getProperty() {
            return property;
        }

        public Period getExtent() {
            return extent;
        }
    }

}
