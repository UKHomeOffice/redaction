 /**
 * Created by leo on 14/10/2016.
 */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.hdfs.inotify.MissingEventsException;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.ietf.jgss.GSSException;
import uk.gov.homeoffice.pontus.KafkaConfig;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Properties;

 public class HDFSNotifierSubscriber {

    public static void main(String[] args) throws IOException, InterruptedException, MissingEventsException, GSSException, URISyntaxException, LoginException {

        final Log LOG = LogFactory.getLog(HDFSNotifierSubscriber.class);
        final Consumer<String, String> consumer;
        final String topicStr;

        Properties props = new Properties();

//        props.put("bootstrap.servers", "localhost:6667");
        props.put("acks", "all");
        props.put("retries", 0);
        props.put("batch.size", 16384);
        props.put("linger.ms", 1);
        props.put("buffer.memory", 33554432);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("group.id", "test-sub");
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.interval.ms", "1000");
        props.put("session.timeout.ms", "30000");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");


        Properties conf = KafkaConfig.getConsumerProperties(props);
        conf.putAll(System.getProperties());


        consumer = new KafkaConsumer<String, String>(conf);

        topicStr = props.getProperty(KafkaConfig.KAFKA_TOPIC_CONF, KafkaConfig.KAFKA_TOPIC_POLLCY_CHANGE_DEFVAL);
        consumer.subscribe(Arrays.asList(topicStr));

        while (true) {
            try {
                String path = null;
                String owner = null;
                ConsumerRecords<String, String> records = consumer.poll(100);
                for (ConsumerRecord<String, String> record : records) {

                    System.out.println(String.format("GOT MESSAGE: offset = %d, key = %s, value = %s", record.offset(), record.key(), record.value()));

                }


            } catch (Exception e) {
                System.err.println("Found issue: " + e.toString());
                e.printStackTrace();
            }
        }
    }
}