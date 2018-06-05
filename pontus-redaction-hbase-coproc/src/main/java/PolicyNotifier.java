/**
 * Created by leo on 14/10/2016.
 */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.inotify.MissingEventsException;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.ietf.jgss.GSSException;
import uk.gov.homeoffice.pontus.KafkaConfig;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import static uk.gov.homeoffice.pontus.PolicyStore.ACL_READ_SUBPATH;

public class PolicyNotifier {

  public static void main(String[] args) throws IOException, InterruptedException, MissingEventsException, GSSException, URISyntaxException, LoginException {

    final Log LOG = LogFactory.getLog(PolicyNotifier.class);
    final Producer<String, String> producer;
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
    Properties conf = KafkaConfig.getProducerProperties(props);

    conf.putAll(System.getProperties());

    producer = new KafkaProducer<>(conf);

    String path = null;
    if (args.length == 1) {
      path = args[0];
    }
    else {
      path = System.getProperty( "pontus.redaction.policy.store.uri",    System.getProperty("hbase.rootdir", "hdfs://sandbox.hortonworks.com:8020/apps/hbase/data"));
      if (path.startsWith("/")){
        path = "file://"+path;
      }
      path += ACL_READ_SUBPATH;
    }
    System.out.println("Using the following path:" + path);


    final Configuration config = new Configuration(false);
    config.addResource(new Path("/usr/hdp/current/hadoop-client/conf/core-site.xml"));
    config.addResource(new Path("/usr/hdp/current/hadoop-client/conf/hdfs-site.xml"));
    config.addResource(new Path("/opt/pontus/pontus-hbase/current/conf/core-site.xml"));
    config.addResource(new Path("/opt/pontus/pontus-hbase/current/conf/hdfs-site.xml"));

    config.set("hadoop.security.authentication", "Kerberos");

    topicStr = config.get(KafkaConfig.KAFKA_TOPIC_CONF, KafkaConfig.KAFKA_TOPIC_POLLCY_CHANGE_DEFVAL);

//    final String relevantPath = uri.getPath() + ACL_READ_SUBPATH;
    try {
      String key = "path";
      String val =  path;
      producer.send(new ProducerRecord<String, String>(topicStr, key, val));
      producer.flush();

      System.out.println(String.format("Sent key %s val %s to topic %s ", key, val, topicStr));


    } catch (Exception e) {
      System.err.println("Found issue: " + e);
      e.printStackTrace();
    }
  }
}