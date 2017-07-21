package uk.gov.homeoffice.pontus.solr;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.search.Query;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;
import org.apache.solr.search.SyntaxError;

/**
 * Created by leo on 15/11/2016.
 */
public class SolrPoleSecurityQParserPlugin extends QParserPlugin {

    protected static final Log LOG = LogFactory.getLog(SolrPoleSecurityQParserPlugin.class);


    @Override
    public QParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {

        try {

            return new QParser(qstr, localParams, params, req) {

                @Override
                public Query parse() throws SyntaxError {
                    try {
//                        if (req.getUserPrincipal() == null){
//                            return new SolrPoleSecurityQueryWithoutUser();
//                        }
                        return new SolrPoleSecurityQuery(req, req.getOriginalParams());
                    } catch (Throwable e) {
                        LOG.error("Failed to create Query: " + e);
                    }
                    return null;
                }
            };


        } catch (Throwable e) {
            LOG.error("failed to create parser:",e);
        }


        return null;
    }
}
