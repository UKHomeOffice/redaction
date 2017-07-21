package uk.gov.homeoffice.pontus;

import java.util.Properties;

/**
 * Created by leo on 01/11/2016.
 */
public class KafkaConfig {
    public final static String KAFKA_PREFIX = "kafka.";
    public final static String KAFKA_BOOSTSTRAP_SERVERS = "bootstrap.servers";
    public final static String KAFKA_BOOSTSTRAP_SERVERS_CONF = KAFKA_PREFIX + KAFKA_BOOSTSTRAP_SERVERS;
    public final static String KAFKA_ACKS = "acks";
    public final static String KAFKA_ACKS_CONF = KAFKA_PREFIX + KAFKA_ACKS;

    public final static String RING_BUFFER_SIZE_CONF = "kafka.ringBufferSize";
    public final static String RING_BUFFER_WAIT_STRATEGY_CONF = "kafka.ringBufferWaitStrategy";

    public final static String KAFKA_TOPIC_CONF = "kafka.hbaseTopic";
    public static final String KAFKA_TOPIC_DEFVAL = "hbase_pole_data";
    public static final String KAFKA_TOPIC_POLLCY_CHANGE_DEFVAL = "hbase_pole_data_policy_change";

    public final static String KAFKA_RETRIES = "retries";
    public final static String KAFKA_RETRIES_CONF = KAFKA_PREFIX + KAFKA_RETRIES;
    public final static String KAFKA_BATCH_SIZE = "batch.size";
    public final static String KAFKA_BATCH_SIZE_CONF = KAFKA_PREFIX + KAFKA_BATCH_SIZE;
    public final static String KAFKA_LINGER_MS = "linger.ms";
    public final static String KAFKA_LINGER_MS_CONF = KAFKA_PREFIX + KAFKA_LINGER_MS;
    public final static String KAFKA_BUFFER_MEMORY = "buffer.memory";
    public final static String KAFKA_BUFFER_MEMORY_CONF = KAFKA_PREFIX + KAFKA_BUFFER_MEMORY;
    public final static String KAFKA_KEY_SERIALIZER = "key.serializer";
    public final static String KAFKA_KEY_SERIALIZER_CONF = KAFKA_PREFIX + KAFKA_KEY_SERIALIZER;
    public final static String KAFKA_VAL_SERIALIZER = "value.serializer";
    public final static String KAFKA_VAL_SERIALIZER_CONF = KAFKA_PREFIX + KAFKA_VAL_SERIALIZER;
    public final static String KAFKA_KEY_DESERIALIZER = "key.deserializer";
    public final static String KAFKA_KEY_DESERIALIZER_CONF = KAFKA_PREFIX + KAFKA_KEY_DESERIALIZER;
    public final static String KAFKA_VAL_DESERIALIZER = "value.deserializer";
    public final static String KAFKA_VAL_DESERIALIZER_CONF = KAFKA_PREFIX + KAFKA_VAL_DESERIALIZER;
    public final static String KAFKA_GROUP_ID = "group.id";
    public final static String KAFKA_GROUP_CONF = KAFKA_PREFIX + KAFKA_GROUP_ID;
    public final static String KAFKA_ENABLE_AUTO_COMMIT = "enable.auto.commit";
    public final static String KAFKA_ENABLE_AUTO_COMMIT_CONF = KAFKA_PREFIX + KAFKA_ENABLE_AUTO_COMMIT;
    public final static String KAFKA_ENABLE_AUTO_COMMIT_INTERVAL_MS = "auto.commit.interval.ms";
    public final static String KAFKA_ENABLE_AUTO_COMMIT_INTERVAL_MS_CONF = KAFKA_PREFIX + KAFKA_ENABLE_AUTO_COMMIT_INTERVAL_MS;

    public final static String KAFKA_FETCH_MIN_BYTES = "fetch.min.bytes";
    public final static String KAFKA_FETCH_MIN_BYTES_CONF = KAFKA_PREFIX + KAFKA_FETCH_MIN_BYTES;
    public final static String KAFKA_RECEIVE_BUFFER_BYTES = "receive.buffer.bytes";
    public final static String KAFKA_RECEIVE_BUFFER_BYTES_CONF = KAFKA_PREFIX + KAFKA_RECEIVE_BUFFER_BYTES;
    public final static String KAFKA_MAX_PARTITION_FETCH_BYTES = "max.partition.fetch.bytes";
    public final static String KAFKA_MAX_PARTITION_FETCH_BYTES_CONF = KAFKA_PREFIX + KAFKA_MAX_PARTITION_FETCH_BYTES;


    public static Properties getConsumerProperties(Properties conf) {
        Properties props = getCommonProperties(conf);

        props.put(KAFKA_KEY_DESERIALIZER, conf.getProperty(KAFKA_KEY_DESERIALIZER_CONF, "org.apache.kafka.common.serialization.StringDeserializer"));
        props.put(KAFKA_VAL_DESERIALIZER, conf.getProperty(KAFKA_VAL_DESERIALIZER_CONF, "org.apache.kafka.common.serialization.StringDeserializer"));

        props.put(KAFKA_FETCH_MIN_BYTES, conf.getProperty(KAFKA_FETCH_MIN_BYTES_CONF, "1"));
        props.put(KAFKA_RECEIVE_BUFFER_BYTES, conf.getProperty(KAFKA_RECEIVE_BUFFER_BYTES_CONF, "262144"));
        props.put(KAFKA_MAX_PARTITION_FETCH_BYTES, conf.getProperty(KAFKA_MAX_PARTITION_FETCH_BYTES_CONF, "2097152"));

        props.put(KAFKA_GROUP_ID, conf.getProperty(KAFKA_GROUP_CONF, "polegroup"));

        return props;

    }

    public static Properties getProducerProperties(Properties conf) {
        Properties props = getCommonProperties(conf);
        props.put(KAFKA_KEY_SERIALIZER, conf.getProperty(KAFKA_KEY_SERIALIZER_CONF, "org.apache.kafka.common.serialization.StringSerializer"));
        props.put(KAFKA_VAL_SERIALIZER, conf.getProperty(KAFKA_VAL_SERIALIZER_CONF, "org.apache.kafka.common.serialization.StringSerializer"));

        props.put(KAFKA_ACKS, conf.getProperty(KAFKA_ACKS_CONF, "all"));
        props.put(KAFKA_RETRIES, conf.getProperty(KAFKA_RETRIES_CONF, "0"));
        props.put(KAFKA_BATCH_SIZE, conf.getProperty(KAFKA_BATCH_SIZE_CONF, "16384"));
        props.put(KAFKA_LINGER_MS, conf.getProperty(KAFKA_LINGER_MS_CONF, "1"));
        props.put(KAFKA_BUFFER_MEMORY, conf.getProperty(KAFKA_BUFFER_MEMORY_CONF, "33554432"));

        return props;

    }


    protected static Properties getCommonProperties(Properties conf) {


        Properties props = new Properties();

        props.putAll(conf);
        props.put("java.security.auth.login.config",conf.getProperty(KAFKA_PREFIX +"security.auth.login.config", "/opt/kafka/security/jaas_policy_store.conf"));


        props.put("security.protocol",conf.getProperty(KAFKA_PREFIX + "security.protocol","SASL_SSL"));
        props.put("ssl.truststore.location",conf.getProperty(KAFKA_PREFIX + "ssl.truststore.location","/opt/kafka/security/kafka.client.truststore.jks"));
        props.put("ssl.truststore.password",conf.getProperty(KAFKA_PREFIX + "ssl.truststore.password","pa55word"));
        props.put("ssl.keystore.location",conf.getProperty(KAFKA_PREFIX + "ssl.keystore.location","/opt/kafka/security/kafka.client.keystore.jks"));
        props.put("ssl.keystore.password",conf.getProperty(KAFKA_PREFIX +"ssl.keystore.password","pa55word"));
        props.put("ssl.key.password",conf.getProperty(KAFKA_PREFIX +"ssl.key.password","pa55word"));

        props.put("sasl.kerberos.service.name",conf.getProperty(KAFKA_PREFIX + "sasl.kerberos.service.name", "kafka"));
        props.put(KAFKA_BOOSTSTRAP_SERVERS, conf.getProperty(KAFKA_BOOSTSTRAP_SERVERS_CONF, "sandbox.hortonworks.com:9094"));
        props.put(KAFKA_ENABLE_AUTO_COMMIT, conf.getProperty(KAFKA_ENABLE_AUTO_COMMIT_CONF, "true"));
        props.put(KAFKA_ENABLE_AUTO_COMMIT_INTERVAL_MS, conf.getProperty(KAFKA_ENABLE_AUTO_COMMIT_INTERVAL_MS_CONF, "1000"));




//        props.put(KAFKA_TOPIC_CONF, conf.getProperty(KAFKA_TOPIC_CONF, "hbase-pole-data-001"));

        return props;
    }

}
