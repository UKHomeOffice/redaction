package uk.gov.homeoffice.pontus.kafka;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import uk.gov.homeoffice.pontus.KafkaConfig;

import java.net.UnknownHostException;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;


/**
 * Created by leo on 21/10/2016.
 */
public class KafkaConnectorSingleton implements EventHandler<KafkaRequest> {


    protected static KafkaConnectorSingleton instance = null;
    protected static final Log LOG = LogFactory.getLog(KafkaConnectorSingleton.class);

    protected Producer<String, String> producer;
    protected Disruptor<KafkaRequest> kafkaRequestDisruptor;
    protected RingBuffer<KafkaRequest> kafkaRequestRingBuffer;
    protected Executor executor;
    protected String topicStr;


    protected KafkaConnectorSingleton(Configuration properties) throws UnknownHostException {

        Properties currProps = new Properties();
        currProps.putAll(System.getProperties());

        currProps.putAll(properties.getValByRegex(".*"));

        topicStr = properties.get(KafkaConfig.KAFKA_TOPIC_CONF,KafkaConfig.KAFKA_TOPIC_DEFVAL);
//        Properties props = new Properties();
////        props.put("bootstrap.servers", "localhost:9092");
//        props.put("acks", "all");
//        props.put("retries", 0);
//        props.put("batch.size", 16384);
//        props.put("linger.ms", 1);
//        props.put("buffer.memory", 33554432);
//        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
//        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        Properties conf = KafkaConfig.getProducerProperties(currProps);




        producer = new KafkaProducer<>(conf);


        executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread retVal = new Thread(r, "KafkaDisruptorTP");
                return retVal;
            }
        });
        WaitStrategy waitStrategy;

        try {
            String waitingStrategyStr = conf.getProperty(KafkaConfig.RING_BUFFER_WAIT_STRATEGY_CONF,"com.lmax.disruptor.BusySpinWaitStrategy");
            waitStrategy = this.getClass()
                    .getClassLoader()
                    .loadClass(waitingStrategyStr)
                    .asSubclass(WaitStrategy.class)
                    .newInstance();
            LOG.info (String.format("Set up the wait strategy of %s for the Kafka coprocessor",waitingStrategyStr));

        } catch (ClassNotFoundException|InstantiationException|IllegalAccessException e) {
            waitStrategy = new SleepingWaitStrategy();
            LOG.warn(String.format("Set up the wait strategy of SleepingWaitStrategy for the Kafka coprocessor"));

        }

        kafkaRequestDisruptor = new Disruptor<KafkaRequest>(new EventFactory<KafkaRequest>() {
            @Override
            public KafkaRequest newInstance() {
                return new KafkaRequest();
            }
        },Integer.parseInt(conf.getProperty(KafkaConfig.RING_BUFFER_SIZE_CONF,"16384")),
                executor,
                ProducerType.MULTI,
                new SleepingWaitStrategy());

        kafkaRequestDisruptor.handleEventsWith(this);
        kafkaRequestRingBuffer = kafkaRequestDisruptor.start();


    }

    public static KafkaConnectorSingleton create(Configuration conf) throws UnknownHostException {
        if (instance == null) {
            synchronized (KafkaConnectorSingleton.class) {
                if (instance == null) {
                    instance = new KafkaConnectorSingleton(conf);
                }
            }
        }


        return instance;
    }


    // This method puts an KafkaRequest into a ring buffer that is then consumed by
    // a separate thread in the onEvent() callback.  All the KafkaRequest entries are
    // pre-allocated, thus reducing a bit of burden on the garbage collector.
    public void indexCell(String tableName, String rowStr, String colStr, String valStr) throws InsufficientCapacityException {

        LOG.info (String.format("in indexCell() tableName = %s rowStr = %s colStr = %s; val = %s ",
                tableName,rowStr,colStr,valStr));

        long sequence = kafkaRequestRingBuffer.tryNext();

        LOG.info (String.format("in indexCell() seq = %d tableName = %s rowStr = %s colStr = %s; val = %s ",
                sequence, tableName,rowStr,colStr,valStr));

        try
        {
            KafkaRequest  event = kafkaRequestRingBuffer.get(sequence); // Get the entry in the Disruptor
            // for the sequence

            event.table=  tableName;
            event.row= rowStr;
            event.col= colStr;
            event.val= valStr;

        }
        finally
        {
            kafkaRequestRingBuffer.publish(sequence);
            LOG.info (String.format("in indexCell() published seq = %d tableName = %s rowStr = %s colStr = %s; val = %s ",
                    sequence, tableName,rowStr,colStr,valStr));

        }


    }


    // This method is called by the Disruptor thread that handles events; it should not be called
    // directly from other classes.
    @Override
    public void onEvent(KafkaRequest req, long sequence, boolean endOfBatch) throws Exception {
//        long cursor = kafkaRequestRingBuffer.next();
//
//        KafkaRequest req = kafkaRequestRingBuffer.get(cursor);
        LOG.info (String.format("in onEvent() before publishing  seq = %d to topic %s",
                sequence, topicStr));


        req.reset();
        String key = req.createKeyStr();
        String val = req.val;
        producer.send(new ProducerRecord<String, String>(topicStr, key, val));
//        producer.flush();

        LOG.info (String.format("Sent key %s val %s to topic %s ",key,val, topicStr));

//        producer.close();


//        if (commit.getStatus() )
//            // If it wasn't successfully created, push the sequence number back to the ring buffer.
//            kafkaRequestRingBuffer.resetTo(sequence);
//        }

    }
}
