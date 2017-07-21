package uk.gov.homeoffice.pontus.solr;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.Krb5HttpClientConfigurer;
import org.apache.solr.client.solrj.request.DelegationTokenRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.DelegationTokenResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import uk.gov.homeoffice.pontus.NamedThreadFactory;

import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


/**
 * Created by leo on 21/10/2016.
 */
public class SolrConnectorSingleton implements EventHandler<SolrInputDocument> {

    public final static String ZK_HOSTS_CONF = "solr.zkHosts";
    public final static String SOLR_COLLECTION_CONF = "solr.collection";
    public final static String RING_BUFFER_SIZE_CONF = "solr.ringBufferSize";
    public final static String RING_BUFFER_BUSYSPIN_CONF = "solr.ringBufferBusySpin";
    private static final String PASSWORD = "SolrRocks";
    private static final String USER = "solr";


    protected static SolrConnectorSingleton instance = null;
    protected static final Log LOG = LogFactory.getLog(SolrConnectorSingleton.class);

    protected CloudSolrClient solr;
    protected Disruptor<SolrInputDocument> solrRequestDisruptor;
    protected RingBuffer<SolrInputDocument> solrRequestRingBuffer;
    protected Executor executor;

    private String getDelegationToken(final String renewer, final String user, HttpSolrClient solrClient) throws Exception {
        DelegationTokenRequest.Get get = new DelegationTokenRequest.Get(renewer) {
            @Override
            public SolrParams getParams() {
                ModifiableSolrParams params = new ModifiableSolrParams(super.getParams());
                params.set("user", user);
                return params;
            }
        };
        DelegationTokenResponse.Get getResponse = get.process(solrClient);
        return getResponse.getDelegationToken();
    }
    protected SolrConnectorSingleton(Configuration conf) throws UnknownHostException, MalformedURLException {

        String zkHostString =  conf.get(ZK_HOSTS_CONF,"sandbox.hortonworks.com:2181");

//        String[] solrServerUrls = conf.get(SOLR_SERVER_URLS_CONF,"http://sandbox.hortonworks.com:8983/solr").split(",");
//
//        LBHttpSolrClient lbClient = new LBHttpSolrClient(solrServerUrls);
//        System.setProperty("java.security.auth.login.config", "/home/foo/jaas-client.conf");
        HttpClientUtil.addConfigurer(new Krb5HttpClientConfigurer());

        //String curr
        System.setProperty("java.security.auth.login.config", "/opt/solr/solr_jaas.conf");

        CloudSolrClient.Builder builder = new CloudSolrClient.Builder();
        solr = builder.withZkHost(zkHostString).withZkChroot("/solr").build();

      //  solr = new CloudSolrClient(zkHostString); // .().withZkHost(zkHostString).build();
        solr.setDefaultCollection(conf.get(SOLR_COLLECTION_CONF,"labs"));


        executor = Executors.newFixedThreadPool(1, new NamedThreadFactory("SolrConn"));

        solrRequestDisruptor = new Disruptor<SolrInputDocument>(new EventFactory<SolrInputDocument>() {
            @Override
            public SolrInputDocument newInstance() {
                return new SolrInputDocument();
            }
        },conf.getInt(RING_BUFFER_SIZE_CONF,16384),
                executor,
                ProducerType.MULTI,
                conf.getBoolean(RING_BUFFER_BUSYSPIN_CONF,false)?
                  new BusySpinWaitStrategy():new SleepingWaitStrategy());

        solrRequestDisruptor.handleEventsWith(this);
        solrRequestRingBuffer = solrRequestDisruptor.start();


    }

    public static SolrConnectorSingleton create(Configuration conf) throws UnknownHostException, MalformedURLException {
        if (instance == null) {
            synchronized (SolrConnectorSingleton.class) {
                if (instance == null) {
                    instance = new SolrConnectorSingleton(conf);
                }
            }
        }


        return instance;
    }


    // This method puts an KafkaRequest into a ring buffer that is then consumed by
    // a separate thread in the onEvent() callback.  All the KafkaRequest entries are
    // pre-allocated, thus reducing a bit of burden on the garbage collector.
    public void indexCell(String tableName, String rowStr, String colStr, String valStr) throws InsufficientCapacityException {

        long sequence = solrRequestRingBuffer.tryNext();
        try
        {
            SolrInputDocument  event = solrRequestRingBuffer.get(sequence); // Get the entry in the Disruptor
            // for the sequence

            event.setField("table", tableName);
            event.setField("row", rowStr);
            event.setField("col", colStr);
            event.setField("val", valStr);

        }
        finally
        {
            solrRequestRingBuffer.publish(sequence);
        }


    }


    // This method is called by the Disruptor thread that handles events; it should not be called
    // directly from other classes.
    @Override
    public void onEvent(SolrInputDocument req, long sequence, boolean endOfBatch) throws Exception {
//        long cursor = kafkaRequestRingBuffer.next();
//
//        KafkaRequest req = kafkaRequestRingBuffer.get(cursor);

        try {
            UpdateRequest solrRequest = new UpdateRequest();
          //  solrRequest.setBasicAuthCredentials(USER, PASSWORD);
            solrRequest.add(req);


            LOG.info("before Adding to SOLR");
            solr.request(solrRequest);
            LOG.info("after Adding to SOLR");

            // Remember to commit your changes!
            // cheaper way to do mod 1024

            if ((sequence & 1) == 0) {

//                UpdateResponse commit = solr.commit();

                UpdateRequest commit =
                        new UpdateRequest();
                commit.setAction(UpdateRequest.ACTION.COMMIT, true, true);
                 //     .setBasicAuthCredentials(USER, PASSWORD);
                solr.request(commit);

                LOG.info("after committing to SOLR:" + commit.toString());

            }
        }catch (Throwable e){
            e.printStackTrace();
            LOG.error("Error indexing data; request = " + req.toString() + "; error = " + e.toString());
            LOG.error("Error indexing data: ",e);
        }
//        if (commit.getStatus() )
//            // If it wasn't successfully created, push the sequence number back to the ring buffer.
//            kafkaRequestRingBuffer.resetTo(sequence);
//        }

    }
}
