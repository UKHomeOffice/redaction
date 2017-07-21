/**
 * Created by leo on 14/10/2016.
 */

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSInotifyEventInputStream;
import org.apache.hadoop.hdfs.client.HdfsAdmin;
import org.apache.hadoop.hdfs.inotify.Event;
import org.apache.hadoop.hdfs.inotify.EventBatch;
import org.apache.hadoop.hdfs.inotify.MissingEventsException;
import org.apache.hadoop.security.UserGroupInformation;
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

public class HDFSNotifier {

    public static void main(String[] args) throws IOException, InterruptedException, MissingEventsException, GSSException, URISyntaxException, LoginException {

        final Log LOG = LogFactory.getLog(HDFSNotifier.class);
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

        long lastReadTxid = 0;

        if (args.length > 1) {
            lastReadTxid = Long.parseLong(args[1]);
        }


        final Configuration config = new Configuration(false);
        config.addResource(new Path("/usr/hdp/current/hadoop-client/conf/core-site.xml"));
        config.addResource(new Path("/usr/hdp/current/hadoop-client/conf/hdfs-site.xml"));

        config.set("hadoop.security.authentication", "Kerberos");

        config.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem");
        config.set("fs.file.impl", "org.apache.hadoop.fs.LocalFileSystem");

        final URI uri = URI.create(conf.getProperty("hbase.rootdir", "hdfs://sandbox.hortonworks.com:8020/apps/hbase/data"));

        config.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem");
        config.set("fs.file.impl", "org.apache.hadoop.fs.LocalFileSystem");

        topicStr = config.get(KafkaConfig.KAFKA_TOPIC_CONF, KafkaConfig.KAFKA_TOPIC_POLLCY_CHANGE_DEFVAL);

        final String relevantPath = uri.getPath() + ACL_READ_SUBPATH;
        try {
            UserGroupInformation.setConfiguration(config);
            UserGroupInformation userGroupInformation =
              UserGroupInformation.loginUserFromKeytabAndReturnUGI(conf.getProperty("redaction.loginuser.kerb.princ","hdfs-sandbox@YOUR_REALM_GOES_HERE"),
                conf.getProperty("redaction.loginuser.kerb.keytab","/etc/security/keytabs/hdfs.headless.keytab"));
            UserGroupInformation.setLoginUser(userGroupInformation);
            HdfsAdmin admin = new HdfsAdmin(uri, config);

            DFSInotifyEventInputStream eventStream = admin.getInotifyEventStream();
            String path = null;
            String owner = null;
            while (true) {
                EventBatch batch = eventStream.take();
                System.out.println("TxId = " + batch.getTxid());

                for (Event event : batch.getEvents()) {
                    System.out.println("event type = " + event.getEventType());
                    Event.EventType eventType = event.getEventType();
                    path = null;
                    switch (eventType) {
                        case CREATE:
                            Event.CreateEvent createEvent = (Event.CreateEvent) event;
                            path = createEvent.getPath();
                            owner = createEvent.getOwnerName();
                            break;
                        case UNLINK:
                            Event.UnlinkEvent unlinkEvent = (Event.UnlinkEvent) event;
                            path = unlinkEvent.getPath();
                            break;

                        case APPEND:
                            Event.AppendEvent appendEvent = (Event.AppendEvent) event;
                            path = appendEvent.getPath();
                            break;
                        case CLOSE:
                            Event.CloseEvent closeEvent = (Event.CloseEvent) event;
                            path = closeEvent.getPath();
                            break;
                        case RENAME:
                            Event.RenameEvent renameEvent = (Event.RenameEvent) event;
                            path = renameEvent.getDstPath();
                            break;
                        default:
                            break;
                    }
                    if (path != null && path.startsWith(relevantPath)) {

                        String key = "path";
                        String val = path;
                        producer.send(new ProducerRecord<String, String>(topicStr, key, val));
                        producer.flush();

                        System.out.println(String.format("Sent key %s val %s to topic %s ", key, val, topicStr));

                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Found issue: "+ e);
            e.printStackTrace();
        }
    }
}