import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import uk.gov.homeoffice.pontus.KafkaConfig;
import uk.gov.homeoffice.pontus.NamedThreadFactory;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by leo on 31/10/2016.
 */
public class KafkaTestSubscriber {


    protected Executor executorKafka;
    protected static KafkaTestSubscriber instance = null;

    protected Consumer<String, String> consumer;
    protected String topicStr;


    protected KafkaTestSubscriber(Properties properties) throws UnknownHostException {

        Properties conf = KafkaConfig.getConsumerProperties(properties);

        topicStr = properties.getProperty(KafkaConfig.KAFKA_TOPIC_CONF,KafkaConfig.KAFKA_TOPIC_DEFVAL);

        consumer = new KafkaConsumer<String, String>(conf);

        consumer.subscribe( Arrays.asList(topicStr) );

        executorKafka = Executors.newFixedThreadPool(1, new NamedThreadFactory("KafkaMainloop"));



        executorKafka.execute(new Runnable(){
            @Override
            public void run() {
                System.out.println("In executorKafka.execute");
                process();
            }
        });


//        node.start();

    }

    public static KafkaTestSubscriber create(Properties conf) throws UnknownHostException {
        if (instance == null) {
            synchronized (KafkaTestSubscriber.class) {
                if (instance == null) {
                    instance = new KafkaTestSubscriber(conf);
                }
            }
        }


        return instance;
    }

    public void process(){
        System.out.println(String.format("Starting Kafka mainloop\n"));

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(100);
            for (ConsumerRecord<String, String> record : records){
                System.out.println(String.format("offset = %d, key = %s, value = %s", record.offset(), record.key(), record.value()));
                String [] keyParts  = record.key().split("#");
                String val = record.value();
                String table = keyParts[0];
                String row = keyParts[1];
                String col = keyParts[2];

            }


        }

    }

    public static void main(String[] args){
        try {

            Properties props = new Properties();
            props.put("bootstrap.servers", "localhost:9092");
            props.put("group.id", "polegroup-sub");
            props.put("enable.auto.commit", "true");
            props.put("auto.commit.interval.ms", "1000");
            props.put("session.timeout.ms", "30000");
            props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
            consumer.subscribe(Arrays.asList(KafkaConfig.KAFKA_TOPIC_DEFVAL));
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(100);
                for (ConsumerRecord<String, String> record : records)
                    System.out.printf("offset = %d, key = %s, value = %s\n", record.offset(), record.key(), record.value());
            }




//            KafkaTestSubscriber sub = KafkaTestSubscriber.create(System.getProperties());





        } catch (Throwable e) {
            e.printStackTrace();
        }


    }



}
