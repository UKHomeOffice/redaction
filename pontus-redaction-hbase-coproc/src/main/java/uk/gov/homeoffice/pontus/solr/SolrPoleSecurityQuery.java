package uk.gov.homeoffice.pontus.solr;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.solr.common.params.MultiMapSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.DelegatingCollector;
import org.apache.solr.search.ExtendedQueryBase;
import org.apache.solr.search.PostFilter;
import org.apache.zookeeper.KeeperException;
import uk.gov.homeoffice.pontus.*;

import java.io.IOException;
import java.security.Principal;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Created by leo on 15/11/2016.
 */
public class SolrPoleSecurityQuery extends ExtendedQueryBase implements PostFilter {

    protected static final Log LOG = LogFactory.getLog(SolrPoleSecurityQParserPlugin.class);

    protected static Configuration conf;
    protected static JWTStore jwtStore = null;
    protected static PolicyStore policyStore = null;

    protected JWTClaim claim;
    protected SolrQueryRequest req;


    static {
        conf = new Configuration();
        conf.addResource(new Path("/usr/hdp/current/hadoop-client/conf/core-site.xml"));
        conf.addResource(new Path("/usr/hdp/current/hadoop-client/conf/hdfs-site.xml"));
        conf.addResource(new Path("/usr/hdp/current/hbase-regionserver/conf/hbase-site.xml"));
        Properties props = System.getProperties();
        Set<Object> keys = props.keySet();

        for (Object key : keys) {
            if (key instanceof String) {
                String keyStr = (String) key;
                String valStr = props.getProperty(keyStr);
                conf.set(keyStr, valStr);
            }
        }

        try {
            jwtStore = JWTStore.create(conf);
            policyStore = PolicyStore.createAndStart(conf);

        } catch (Exception e) {
            LOG.error("Failed to create jwtStore or policyStore: " + jwtStore.toString());
        }

    }


    protected FilterData filterData = null;
    protected Principal princ = null;

    public SolrPoleSecurityQuery(SolrQueryRequest req, SolrParams origParms) throws IOException, KeeperException, InterruptedException {

        // LPPM - the issue here is that one request can turn into multiple requests, and unfortunately, the
        // req.getUserPrincipal() is returning null in the other requests.  As a workaround, we need to
        // get the principal the first time, and put it in the origParms map so subsequent requests
        // can then piggy-back off the credentials from the original request.

        this.req = req;
        princ = req.getUserPrincipal();

        if (princ == null) {
            if (origParms instanceof MultiMapSolrParams){
                Map<String,String[]> map = ((MultiMapSolrParams)origParms).getMap();
                final String[] princStr = map.get("principal");
                if (princStr != null && princStr.length == 1){
                    princ = new Principal() {
                        @Override
                        public String getName() {
                            return princStr[0];
                        }
                    };
                }
            }
        }
        if (princ != null) {

            JWTUser user = new JWTUser(princ);
            LOG.info(String.format("Creating new SolrPoleSecurityQuery  filter for user %s", user));
            claim = jwtStore.getUserClaim(user);
            LOG.info(String.format("Creating new SolrPoleSecurityQuery filter for claim %s", claim));
            filterData = policyStore.getFilterData(claim.getBizctx());
        }


    }

    @Override
    public int getCost() {
        // We make sure that the cost is at least 100 to be a post filter
        return Math.max(super.getCost(), 1000);
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SolrPoleSecurityQuery that = (SolrPoleSecurityQuery) o;

        if (claim != null ? !claim.equals(that.claim) : that.claim != null) return false;
        if (req != null ? !req.equals(that.req) : that.req != null) return false;
        if (filterData != null ? !filterData.equals(that.filterData) : that.filterData != null) return false;
        return princ != null ? princ.equals(that.princ) : that.princ == null;

    }

    @Override
    public int hashCode() {
        int result =1;
        result = 31 * result + (claim != null ? claim.hashCode() : 0);
        result = 31 * result + (req != null ? req.hashCode() : 0);
        result = 31 * result + (filterData != null ? filterData.hashCode() : 0);
        result = 31 * result + (princ != null ? princ.hashCode() : 0);
        return result;
    }

    @Override
    public DelegatingCollector getFilterCollector(IndexSearcher searcher) {
        return new DelegatingCollector() {

            @Override
            public void collect(int docNumber) throws IOException {

                if (filterData != null) {

                    // To be able to get documents, we need the reader
                    LeafReader reader = context.reader();

                    // From the reader we get the current document by the docNumber
                    Document doc = reader.document(docNumber);


                    boolean needsRedaction = false;
                    Iterator<IndexableField> iter = doc.iterator();
                    while (iter.hasNext()) {

                        IndexableField ifield = iter.next();

                        String strVal = ifield.stringValue();
                        needsRedaction = filterData.needRedaction(strVal);
                        if (needsRedaction) {
                            break;
                        }

                    }


                    // Filter magic
                    if (!needsRedaction) {
                        super.collect(docNumber);
                    }
                }
                else {
                    super.collect(docNumber);

                }
            }
        };

    }
}
