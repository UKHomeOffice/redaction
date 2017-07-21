package uk.gov.homeoffice.pontus.solr;

/*
 * LPPM - to enable this class, please add it to the following file:
 * /opt/lucidworks-hdpsearch/solr/server/solr/configsets/data_driven_schema_configs_hdfs/conf/solrconfig.xml
 *
 * <queryResponseWriter name="xml"
 *                default="true"
 *                class="SolrPoleSecurityXMLResponseWriterFilter" />
 */

import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.QueryResponseWriter;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.zookeeper.KeeperException;

import java.io.IOException;
import java.io.Writer;

/**
 *
 */
public class SolrPoleSecurityXMLResponseWriterFilter implements QueryResponseWriter {
    @Override
    public void init(NamedList n) {
    /* NOOP */
    }

    @Override
    public void write(Writer writer, SolrQueryRequest req, SolrQueryResponse rsp) throws IOException {
        SolrPoleSecurityXMLWriterFilter w = null;
        try {
            w = new SolrPoleSecurityXMLWriterFilter(writer, req, rsp);
            w.writeResponse();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        } finally {
            if (w != null) {
                w.close();
            }
        }
    }

    @Override
    public String getContentType(SolrQueryRequest request, SolrQueryResponse response) {
        return XMLResponseParser.XML_CONTENT_TYPE;
    }
}
