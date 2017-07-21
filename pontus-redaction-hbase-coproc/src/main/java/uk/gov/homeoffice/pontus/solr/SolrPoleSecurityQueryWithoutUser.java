package uk.gov.homeoffice.pontus.solr;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.search.IndexSearcher;
import org.apache.solr.search.DelegatingCollector;
import org.apache.solr.search.ExtendedQueryBase;
import org.apache.solr.search.PostFilter;

import java.io.IOException;

/**
 * Created by leo on 15/11/2016.
 */
public class SolrPoleSecurityQueryWithoutUser extends ExtendedQueryBase implements PostFilter {

    protected static final Log LOG = LogFactory.getLog(SolrPoleSecurityQueryWithoutUser.class);

    public SolrPoleSecurityQueryWithoutUser(){



    }

    @Override
    public boolean equals(Object o) {
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }


    @Override
    public int getCost() {
        // We make sure that the cost is at least 100 to be a post filter
        return Math.max(super.getCost(), 100);
    }

    @Override
    public boolean getCache() {
        return false;
    }

    @Override
    public boolean getCacheSep() {
        return false;
    }


    @Override
    public DelegatingCollector getFilterCollector(IndexSearcher searcher) {
        return new DelegatingCollector() {

            @Override
            public void collect(int docNumber) throws IOException {
             super.collect(docNumber);
            }
        };

    }
}
