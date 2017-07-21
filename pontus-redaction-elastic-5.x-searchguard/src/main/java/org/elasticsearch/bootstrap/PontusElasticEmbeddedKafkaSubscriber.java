package org.elasticsearch.bootstrap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.index.IndexAction;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexRequestBuilder;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.cli.ExitCodes;
import org.elasticsearch.cli.Terminal;
import org.elasticsearch.cli.UserException;
import org.elasticsearch.client.AdminClient;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.util.concurrent.ThreadContext;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeValidationException;
import uk.gov.homeoffice.pontus.ElasticSearchGuardUser;
import uk.gov.homeoffice.pontus.KafkaConfig;
import uk.gov.homeoffice.pontus.NamedThreadFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.security.Permission;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by leo on 31/10/2016.
 */
public class PontusElasticEmbeddedKafkaSubscriber extends Elasticsearch {
    public final static String ENABLE_PLUGIN_CONF = "pontus.enablePlugin";

    public final static String CONFIG_URL_CONF = "elasticsearch.node.config.url";
    public final static String PATH_HOME = "path.home";
    public final static String PATH_HOME_DEFVAL = "/opt/elasticsearch";
    public final static String HTTP_ENABLED = "http.enabled";
    public final static String SCRIPT_INDEXED = "script.indexed";
    public final static String CLUSTER_NAME = "cluster.name";
    public final static String SCRIPT_INLINE = "script.inline";
    public final static String PATH_HOME_CONF = "elasticsearch.pathHome";
    public final static String NODE_CLIENT_CONF = "elasticsearch.node.client";
    public final static String NODE_CLIENT = "node.client";

    //    public final static String TRANSPORTCLIENT_HOST_CONF = "elasticsearch.transportclient.host";
//    public final static String TRANSPORTCLIENT_PORT_CONF = "elasticsearch.transportclient.port";
    public final static String HTTP_ENABLED_CONF = "elasticsearch.httpEnabled";
    public final static String SCRIPT_INDEXED_CONF = "elasticsearch.scriptIndexed";
    public final static String CLUSTER_NAME_CONF = "elasticsearch.clusterName";
    public final static String SCRIPT_INLINE_CONF = "elasticsearch.scriptInline";
//    public final static String DATA_IN_INSTANCE_CONF = "elasticsearch.dataInInstance";

    public final static SecurityManager secMgr = new SecurityManager() {
        @Override
        public void checkPermission(Permission perm) {
            // grant all permissions so that we can later set the security manager to the one that we want
        }
        @Override
        public void checkPermission(Permission perm, Object context) {
            // grant all permissions so that we can later set the security manager to the one that we want

        }

    };

    static{
        System.setSecurityManager(secMgr);
    }

    protected Executor executorKafka;
//    protected Executor executorElastic;

//    protected Node node;
    protected Client client;
    protected AdminClient adminClient;

//    public class EmbeddedNode extends Node {
//
////        protected EmbeddedNode(Environment environment, Version version, Collection<Class<? extends Plugin>> classpathPlugins) {
////            super(environment, version, classpathPlugins);
////        }
//        public EmbeddedNode(Environment environment,  Collection<Class<? extends Plugin>> classpathPlugins) {
//            super(environment,  classpathPlugins);
//        }
//
//
//    }


    protected static PontusElasticEmbeddedKafkaSubscriber instance = null;
    protected static final Log LOG = LogFactory.getLog(PontusElasticEmbeddedKafkaSubscriber.class);

    protected Consumer<String, String> consumer;
    protected String topicStr;

    protected ElasticSearchGuardUser elasticSearchGuardUser ;

    protected PontusElasticEmbeddedKafkaSubscriber(Properties properties, String[] args) throws Exception {
        super();
        elasticSearchGuardUser = new ElasticSearchGuardUser("CN=sgadmin");



//        Properties conf = KafkaConfig.getConsumerProperties(properties);

        topicStr = properties.getProperty(KafkaConfig.KAFKA_TOPIC_CONF, KafkaConfig.KAFKA_TOPIC_DEFVAL);
//        Properties props = new Properties(properties);
//        props.put("bootstrap.servers", "localhost:9092");
//        props.put("group.id", "polegroup-sub");
//        props.put("enable.auto.commit", "true");
//        props.put("auto.commit.interval.ms", "1000");
//        props.put("session.timeout.ms", "30000");
//        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
//        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

//        consumer = new KafkaConsumer<String, String>(props);


        LOG.info("creating kafka Consumer with the following props: \n" + properties.toString());
        Properties conf = KafkaConfig.getConsumerProperties(properties);

//        conf.putAll(System.getProperties());

        System.getProperties().putAll(conf);

        consumer = new KafkaConsumer<String, String>(conf);




//        List<String> topics = new ArrayList<>();
//        topics.add(topicStr);

        consumer.subscribe(Arrays.asList(topicStr));



//        String dataDirectory = conf.getProperty(PATH_HOME, PATH_HOME_DEFVAL);


//        Settings.Builder esSettings = Settings.builder();
//        esSettings.put(HTTP_ENABLED, conf.getProperty(HTTP_ENABLED_CONF, "true"))
//                .put(PATH_HOME, conf.getProperty(PATH_HOME_CONF, dataDirectory))
//                .put(SCRIPT_INLINE, conf.getProperty(SCRIPT_INLINE_CONF, "false"))
//                //LPPM - elastic 2.x
////                .put(SCRIPT_INDEXED, conf.getProperty(SCRIPT_INDEXED_CONF, "false")) //.put("script.disable_dynamic","true")
//                .put(CLUSTER_NAME, conf.getProperty(CLUSTER_NAME_CONF, "elasticsearch"))
//        //LPPM - elastic 2.x
////                .put(NODE_CLIENT, conf.getProperty(NODE_CLIENT_CONF, "false"))
//                ;
//                .put("searchguard.ssl.transport.keystore_filepath","CN=node-0.example.com,OU=SSL,O=Test,L=Test,C=DE-keystore.jks")
//                .put("searchguard.ssl.transport.keystore_password","changeit")
//                .put("searchguard.ssl.transport.truststore_filepath","truststore.jks")
//                .put("searchguard.ssl.transport.truststore_password","changeit")
//                .put("searchguard.ssl.transport.enforce_hostname_verification","false")
//                .put("searchguard.ssl.transport.enabled", "true")
//                .put("tests.jarhell.check", false);

//        try {
//            String settingsFile = conf.getProperty(CONFIG_URL_CONF);
//            if (settingsFile != null) {
//                Path path = Paths.get(settingsFile);
//                esSettings.loadFromPath(path);
//            }
//        } catch (Throwable t) {
//            LOG.error("Failed to load settings from URL; error = " + t.toString());
//        }



//        String host = conf.getProperty(TRANSPORTCLIENT_HOST_CONF, "localhost");
//        int port = Integer.parseInt(conf.getProperty(TRANSPORTCLIENT_PORT_CONF, "9300"));
//

//        client = TransportClient.builder().settings(esSettings)
//                .build()
//                .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName(host), port));

        //  LPPM - NOTE: the following would be more efficient to avoid the double-indexing issues (the client from the
        // TransportClient.builder() does not know where the index is sharded, so it causes a double indexing locally and
        // then in the actual shard; however, this is causing all sorts of exceptions due to clashes between the
        // hbase and elasticsearch classes being loaded through separate class loaders:
        //ElasticsearchException[Failed to load plugin class [org.elasticsearch.script.expression.ExpressionPlugin]];
        // nested: IncompatibleClassChangeError[class org.objectweb.asm.commons.LocalVariablesSorter has interface
        // org.objectweb.asm.MethodVisitor as super class];
        //        at org.elasticsearch.plugins.PluginsService.loadPlugin(PluginsService.java:483)
        //        at org.elasticsearch.plugins.PluginsService.loadBundles(PluginsService.java:432)
        //        at org.elasticsearch.plugins.PluginsService.<init>(PluginsService.java:115)
        //        at org.elasticsearch.node.Node.<init>(Node.java:158)
        //        at org.elasticsearch.node.Node.<init>(Node.java:140)
        //        at org.elasticsearch.node.NodeBuilder.build(NodeBuilder.java:143)
        //        at uk.gov.homeoffice.pontus.elastic.ElasticConnectorSingleton.<init>(ElasticConnectorSingleton.java:82)
        //        at uk.gov.homeoffice.pontus.elastic.ElasticConnectorSingleton.createElasticConnectorSingleton(ElasticConnectorSingleton.java:115)
        //        at uk.gov.homeoffice.pontus.hbase.coprocessor.pole.security.PoleSecurityCoprocessor.start(PoleSecurityCoprocessor.java:236)


        // LPPM - 2.x elastic -- now in our modified boostrap
//        Collection<Class<? extends Plugin>> classpathPlugins = new ArrayList<>();
//
//
////        classpathPlugins.add(com.floragunn.searchguard.ssl.SearchGuardSSLPlugin.class);
////        classpathPlugins.add(com.floragunn.searchguard.SearchGuardPlugin.class);
//
//        if (Boolean.parseBoolean(System.getProperties().getProperty(ENABLE_PLUGIN_CONF, "false"))) {
//            LOG.info(String.format("Adding uk.gov.homeoffice.pontus.ElasticSearchFilterPlugin\n"));
//            classpathPlugins.add(ElasticSearchFilterPlugin.class);
//        }
//
//
//        node = new EmbeddedNode(InternalSettingsPreparer.prepareEnvironment(esSettings.build(), null),
////                Version.CURRENT,
//                classpathPlugins);


//        NodeBuilder nb = NodeBuilder.nodeBuilder();
//        node = nb.client(! Boolean.parseBoolean(conf.getProperty(DATA_IN_INSTANCE_CONF, "true")))
//                 .settings(esSettings)
//                 .build();
//        client = node.client();
//
//        adminClient = client.admin();

        // LPPM Elastic 2.x
//        executorElastic = Executors.newFixedThreadPool(1, new NamedThreadFactory("ElasticMainloop"));
        executorKafka = Executors.newFixedThreadPool(1, new NamedThreadFactory("KafkaMainloop"));
// LPPM Elastic 2.x
//        executorElastic.execute(new Runnable() {
//            @Override
//            public void run() {
//                LOG.warn("In executorKafka.execute");
//                try {
//
//
//                    node.start();
//                } catch (NodeValidationException e) {
//                    LOG.error("In executorKafka.execute",e);
//                }
//            }
//        });


        executorKafka.execute(new Runnable() {
            @Override
            public void run() {
                LOG.warn("In executorKafka.execute");
                process();
            }
        });


//        node.start();

        // LPPM - need to do this again, as something in Kafka seems to reset it.

        // we want the JVM to think there is a security manager installed so that if internal policy decisions that would be based on the
        // presence of a security manager or lack thereof act as if there is a security manager present (e.g., DNS cache policy)
        int status = main(args, this, Terminal.DEFAULT);



        if (status != ExitCodes.OK) {
            exit(status);
        }
        Node node = Bootstrap.getNode();
        client = node.client();

        adminClient = client.admin();


    }

    public static PontusElasticEmbeddedKafkaSubscriber create(Properties conf, String[] args) throws  Exception {
        if (instance == null) {
            synchronized (PontusElasticEmbeddedKafkaSubscriber.class) {
                if (instance == null) {
                    instance = new PontusElasticEmbeddedKafkaSubscriber(conf,args);
                }
            }
        }


        return instance;
    }

    public void process() {
        LOG.info(String.format("Starting Kafka mainloop\n"));

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(100);
            for (ConsumerRecord<String, String> record : records) {
                try {
                    LOG.info(String.format("offset = %d, key = %s, value = %s", record.offset(), record.key(), record.value()));
                    String[] keyParts = record.key().split("#");
                    String val = record.value();
                    String table = keyParts[0];
                    String row = keyParts[1];
                    String col = keyParts[2];

//                    CreateIndexRequest req = new CreateIndexRequest();
//                    req.source(val);
//                    req.index(table);
//                    req.putHeader("row", row);
//                    req.putHeader("col", col);
//                    req.putInContext("_sg_user",elasticSearchGuardUser);
//                    req.putInContext("_sg_channel_type","direct");

//                    CreateIndexResponse response =  adminClient.indices().create(req).actionGet();

                    IndexRequestBuilder irb  = new  IndexRequestBuilder(client,IndexAction.INSTANCE, "pontus_pole");

                    StringBuilder sb = new StringBuilder("{\"val\":\"").append(val).append("\",")
                            .append("\"row\":\"").append(row).append("\",")
                            .append("\"col\":\"").append(col).append("\"}");

                    irb.setIndex("pontus_pole")
                        .setType(table)
                        .setSource(sb.toString())
                        .setCreate(true)
                        .setId(record.key());

                    ThreadContext tc = client.threadPool().getThreadContext();
                    if (tc.getTransient("_sg_user") == null) {
                        tc.putTransient("_sg_user", elasticSearchGuardUser);
                        client.threadPool().getThreadContext().putTransient("_sg_channel_type","direct");

                    }

                    IndexRequest ir = irb.request();
                    // LPPM 2.x elastic
//                    ir.putInContext("_sg_user",elasticSearchGuardUser);
//                    ir.putInContext("_sg_channel_type","direct");


//                    IndexResponse iresp = irb.execute().actionGet();

//                    String id = iresp.getId();
                   IndexResponse response2  =   client.index(ir).actionGet();

                   // LPPM 2.x elastic
//                    if (response2.getCreated()){

                    if (DocWriteResponse.Result.CREATED == response2.getResult()){

                            consumer.commitAsync();

                    }

//                    // Index name
//                    String index = response2.getIndex();
//                    // Type name
//                    String type = response2.getType();
//                    // Document ID (generated or not)
//                    String id = response2.getId();
//                    // Version (if it's the first time you index this document, you will get: 1)
//                    long version = response2.getVersion();
//                    // isCreated() is true if the document is a new one, false if it has been updated
//                    boolean created = response2.isCreated();

//                    if (LOG.isDebugEnabled()) {
//                        LOG.debug(String.format("Added cell = %s; type=%s; id=%s; ver=%s; created = %s;\n",
//                                index, type, id, version, created ? "true" : "false"));
//
//                    }

//        kafkaRequestRingBuffer.
//                    if (created) {
//                        consumer.commitAsync();
//                    }

                } catch (Throwable e) {

                    LOG.error("Failed to inject data into ES:", e);
                }


            }


        }

    }

    public static void main(String[] args) {
        try {

            System.setSecurityManager(new SecurityManager() {
                @Override
                public void checkPermission(Permission perm) {
                    // grant all permissions so that we can later set the security manager to the one that we want
                }
                @Override
                public void checkPermission(Permission perm, Object context) {
                    // grant all permissions so that we can later set the security manager to the one that we want

                }

            });

            final PontusElasticEmbeddedKafkaSubscriber sub = PontusElasticEmbeddedKafkaSubscriber.create(System.getProperties(),args);



        } catch (Exception e) {
            e.printStackTrace();
        }


    }


    /**
     * Main entry point for starting elasticsearch
     */




    /**
     * Required method that's called by Apache Commons procrun when
     * running as a service on Windows, when the service is stopped.
     *
     * http://commons.apache.org/proper/commons-daemon/procrun.html
     *
     * NOTE: If this method is renamed and/or moved, make sure to
     * update elasticsearch-service.bat!
     */
    static void close(String[] args) throws IOException {
        Bootstrap.stop();
    }

    @Override
    void init(final boolean daemonize, final Path pidFile, final boolean quiet, final Map<String, String> esSettings)
            throws NodeValidationException, UserException {
        try {
            Bootstrap.init(!daemonize, pidFile, quiet, esSettings);
        } catch (BootstrapException | RuntimeException e) {
            // format exceptions to the console in a special way
            // to avoid 2MB stacktraces from guice, etc.
            throw new StartupException(e);
        }
    }


}
