package uk.gov.homeoffice.pontus.solr;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.search.ReturnFields;
import org.apache.zookeeper.KeeperException;
import uk.gov.homeoffice.pontus.*;

import java.io.IOException;
import java.io.Writer;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Set;

/**
 * Created by leo on 26/10/2016.
 */




public class SolrPoleSecurityCSVWriterFilter extends CSVWriter {
    protected static final Log LOG = LogFactory.getLog(SolrPoleSecurityCSVWriterFilter.class);
    protected static Configuration conf;
    protected static JWTStore jwtStore = null;
    protected static PolicyStore policyStore = null;

    protected JWTClaim claim;
    protected FilterData filterData;

    protected long start;
    protected int size ;
    protected long numFound;


    static {
        conf = new Configuration();
        conf.addResource(new Path("/usr/hdp/current/hadoop-client/conf/core-site.xml"));
        conf.addResource(new Path("/usr/hdp/current/hadoop-client/conf/hdfs-site.xml"));

        Properties props = System.getProperties();
        Set<Object> keys = props.keySet();

        for (Object key:keys){
            if (key instanceof String){
                String keyStr = (String) key;
                String valStr = props.getProperty(keyStr);
                conf.set(keyStr,valStr);
            }
        }

        try {
            jwtStore = JWTStore.create(conf);
            policyStore = PolicyStore.createAndStart(conf);

        } catch (Exception e) {
            LOG.error("Failed to create jwtStore or policyStore: " + jwtStore.toString());
        }

    }


    public SolrPoleSecurityCSVWriterFilter(Writer writer, SolrQueryRequest req, SolrQueryResponse rsp) throws KeeperException, InterruptedException, IOException {
        super(writer,req,rsp);

        Principal princ = req.getUserPrincipal();
        LOG.info(String.format("Creating new XML writer filter for principal %s", princ));

        JWTUser user = new JWTUser(princ);

        LOG.info(String.format("Creating new CSV writer filter for user %s", user));
        claim = jwtStore.getUserClaim(user);

        LOG.info(String.format("Creating new CSV writer filter for claim %s", claim));

        filterData = policyStore.getFilterData(claim.getBizctx());  // new FilterData(".*/uk.gov.police.*", ".*", "denied", "ssshhhh");

    }


    @Override
    public void writeStartDocumentList(String name, long start, int size, long numFound, Float maxScore) throws IOException {

        this.start = start;
        this.size = size;
        this.numFound = numFound;
        super.writeStartDocumentList(name,start,size,numFound,maxScore);
    }

    @Override
    public void writeSolrDocument(String name, SolrDocument doc, ReturnFields returnFields, int idx) throws IOException {

        boolean needsRedaction = false;
        for (String fname : doc.getFieldNames()) {
            if (returnFields!= null && !returnFields.wantsField(fname)) {
                continue;
            }

            Object val = doc.getFieldValue(fname);
            if (val instanceof ArrayList){
                ArrayList<Object> vals = (ArrayList<Object>)val;
                for (int i = 0, ilen = vals.size(); i < ilen; i++){
                    Object obj = vals.get(i);
                    if (obj instanceof String){
                        String strVal = (String)obj;
                        needsRedaction = filterData.needRedaction(strVal);
                        if (needsRedaction) {
                            obj = filterData.redactWithTextReplacement(strVal,FilterData.REPLACEMENT_VAL);
                            vals.set(i,obj);
//                            break;
                        }

                    }

                }
            }
        }

//        if (!needsRedaction){
            super.writeSolrDocument(name,doc,returnFields,idx);
//        }

    }



}
