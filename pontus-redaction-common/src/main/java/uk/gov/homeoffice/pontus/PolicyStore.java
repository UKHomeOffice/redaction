package uk.gov.homeoffice.pontus;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DFSInotifyEventInputStream;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.ietf.jgss.GSSException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by leo on 16/10/2016.
 * This class uses HDFS to maintain a list of policies for a given set of claims.  The claim is the result of
 * a temporary JWT namespace, which maps into the HDFS directory structure.  Each directory may have up to 4
 * files with Regular Expression Filters as policies; the file names must be one of the following entries:
 * - metadata - a file containing the regex that matches the metadata cell tag in hbase cells
 * - redaction_allowed - a file containing the regex that matches the value of allowed text in each cell
 * - redaction_denied - a file containing the regex that matches the value of disallowed text in each cell
 * - redaction_denied_all - a file containing the regex that matches the value of a catch-all disallowed text in each cell
 * This class also starts a thread that can run in the background looking for changes in the filesystem, and updating
 * the relevant filters affected by those changes.
 */
public class PolicyStore extends Thread {

  public static final Log LOG = LogFactory.getLog(PolicyStore.class);


  public static final int SECURITY_NAMESPACE_CACHE_DEFAULT_INIT_SIZE = 50000;
  public static final float SECURITY_NAMESPACE_CACHE_DEFAULT_LOAD_FACTOR = 0.75f;
  public static final int SECURITY_NAMESPACE_CACHE_DEFAULT_MAX_SIZE = 50000000;
  public static final String SECURITY_NAMESPACE_CACHE_INIT_SIZE = "hbase.security.namespace.cache_init_size";
  public static final String SECURITY_NAMESPACE_CACHE_LOAD_FACTOR = "hbase.security.namespace.cache_load_factor";
  public static final String SECURITY_NAMESPACE_CACHE_MAX_SIZE = "hbase.security.namespace.cache_max_size";
  public static final String ACL_READ_SUBPATH = "/acls/read";
  public static final String METADATA = "metadata";
  public static final String REDACTION_ALLOWED = "redaction_allowed";
  public static final String REDACTION_DENIED = "redaction_denied";
  public static final String REDACTION_DENIED_ALL = "redaction_denied_all";
  public static final String REDACTION_TYPE = "redaction_type";
  public static final String REDACTION_ES_FILTER_QUERY = "redaction_es_filter_query";

  public static final String THREAD_NAME = "PolicyStoreThread";


  protected Configuration config;
  protected static AtomicInteger refCount = new AtomicInteger(0);
  protected static PolicyStore instance = null;
  protected URI uri;
  protected long lastReadTxid = 0;
  protected String relevantPath;
  protected boolean exitHdfsNotifier = false;
  protected LruHashMap<Namespace, FilterData> securityNamespaceCache;
  protected Set<String> affectedDirs = new HashSet<>();
//  protected HdfsAdmin admin = null;
  protected DFSInotifyEventInputStream eventStream = null;
  protected StringBuffer strBuf = new StringBuffer();
  protected String topicStr;


  protected PolicyStore(Configuration conf) throws IOException {
    super(THREAD_NAME);

    this.config = conf;


    String urlStr = config.get( "pontus.redaction.policy.store.uri",    config.get("hbase.rootdir", "hdfs://sandbox.hortonworks.com:8020/apps/hbase/data"));
    if (urlStr.startsWith("/")){
      urlStr = "file://"+urlStr;
    }
    this.uri = URI.create(urlStr);

    config.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem");
    config.set("fs.file.impl", "org.apache.hadoop.fs.LocalFileSystem");
    topicStr = conf.get(KafkaConfig.KAFKA_TOPIC_CONF, KafkaConfig.KAFKA_TOPIC_POLLCY_CHANGE_DEFVAL);


    this.lastReadTxid = 0;
    this.relevantPath = uri.getPath() + ACL_READ_SUBPATH;
    LOG.info("!!!!! RELEVANT PATH = " + this.relevantPath);
    int initSize = config.getInt(SECURITY_NAMESPACE_CACHE_INIT_SIZE, SECURITY_NAMESPACE_CACHE_DEFAULT_INIT_SIZE);
    float loadFactor = config.getFloat(SECURITY_NAMESPACE_CACHE_LOAD_FACTOR, SECURITY_NAMESPACE_CACHE_DEFAULT_LOAD_FACTOR);
    int maxSize = config.getInt(SECURITY_NAMESPACE_CACHE_MAX_SIZE, SECURITY_NAMESPACE_CACHE_DEFAULT_MAX_SIZE);
    this.securityNamespaceCache = new LruHashMap<>(initSize, loadFactor, maxSize);

//    this.admin = new HdfsAdmin(uri, config);

  }

  public static PolicyStore createAndStart(Configuration config) throws IOException, GSSException {
    if (instance == null) {
      // Thread Safe. Might be costly operation in some case
      synchronized (PolicyStore.class) {
        if (instance == null) {
          instance = new PolicyStore(config);
          instance.start();
        }
      }
    }
    refCount.incrementAndGet();
    return instance;
  }


  protected static FileStatus[] shellListStatus(FileSystem srcFs,
                                                FileStatus src) {
    if (!src.isDirectory()) {
      FileStatus[] files = {src};
      return files;
    }
    Path path = src.getPath();
    try {
      FileStatus[] files = srcFs.listStatus(path);
//                if (files == null) {
//                    System.err.println(" could not get listing for '" + path + "'");
//                }
      return files;
    } catch (IOException e) {
      System.err.println("Could not get get listing for '" + path + "' : " +
        e.getMessage().split("\n")[0]);
    }
    return null;
  }

  protected static FilterData findFilterData(Configuration config, String srcDir) throws IOException {
    Path srcPath = new Path(srcDir);


    FileSystem srcFs = srcPath.getFileSystem(config);

    if (!srcFs.isDirectory(srcPath)) {
      return null;
    }


    FileStatus[] srcs = null;

    while (srcs == null) {
      srcs = srcFs.globStatus(srcPath);
      if (srcs == null) {
        if (srcPath.isRoot()) {
          return null;
        }
        srcPath = srcPath.getParent();
      }
    }

    FilterData retVal = new FilterData();

    boolean stopSearch = false;
    int numItemsFound = 0;

    while (!stopSearch) {

      for (int i = 0, ilen = srcs.length; i < ilen; i++) {
        final FileStatus[] items = shellListStatus(srcFs, srcs[i]);
        for (int j = 0, jlen = items.length; j < jlen; j++) {
          FileStatus item = items[j];
          if (!item.isDirectory()) {
            Path curPath = item.getPath();
            String fileName = curPath.getName();
            LOG.info("searching for " + fileName + " in path " + curPath.toString());
            if (METADATA.equals(fileName)) {
              if (retVal.getCo()r() == null) {
                FSDataInputStream is = srcFs.open(curPath);
                String filterStr = IOUtils.toString(is);
                retVal.setColumnRulesStr(filterStr);
                numItemsFound++;
                LOG.info("Found metadata in path (" + curPath.toString() + ") : " + filterStr);

              }
            }
            else if (REDACTION_ALLOWED.equals(fileName)) {
              if (retVal.getRedactionAllowedStr() == null) {
                FSDataInputStream is = srcFs.open(curPath);
                String filterStr = IOUtils.toString(is);
                retVal.setRedactionAllowedStr(filterStr);
                numItemsFound++;
                LOG.info("Found redaction allowed in path (" + curPath.toString() + ") : " + filterStr);

              }
            }
            else if (REDACTION_DENIED.equals(fileName)) {
              if (retVal.getRedactionDeniedStr() == null) {
                FSDataInputStream is = srcFs.open(curPath);
                String filterStr = IOUtils.toString(is);
                retVal.setRedactionDeniedStr(filterStr);
                numItemsFound++;
                LOG.info("Found redaction denied in path (" + curPath.toString() + ") : " + filterStr);

              }
            }
            else if (REDACTION_DENIED_ALL.equals(fileName)) {
              if (retVal.getRedactionDeniedAllStr() == null) {
                FSDataInputStream is = srcFs.open(curPath);
                String filterStr = IOUtils.toString(is);
                retVal.setRedactionDeniedAllStr(filterStr);
                numItemsFound++;
                LOG.info("Found redaction denied all in path (" + curPath.toString() + ") : " + filterStr);

              }

            }
            else if (REDACTION_TYPE.equals(fileName)) {
              if (retVal.getRedactionType() == null) {
                FSDataInputStream is = srcFs.open(curPath);
                String filterStr = IOUtils.toString(is);
                retVal.setRedactionType(filterStr);
                numItemsFound++;
                LOG.info("Found redaction type in path (" + curPath.toString() + ") : " + filterStr);
              }
            }
            else if (REDACTION_ES_FILTER_QUERY.equals(fileName)){
              if (retVal.getRedactionElasticPostFilterQueryStr() == null){
                FSDataInputStream is = srcFs.open(curPath);
                String filterStr = IOUtils.toString(is);
                retVal.setRedactionElasticPostFilterQueryStr(filterStr);
                numItemsFound++;
                LOG.info("Found es filter query in path (" + curPath.toString() + ") : " + filterStr);

              }
            }
            else {
              LOG.warn("Found unknown file name: " + fileName);
            }

            if (numItemsFound == 5) {
              stopSearch = true;
              break;
            }
          }
        }
      }

      if (srcPath.isRoot()) {
        stopSearch = true;
        break;
      }

      srcPath = srcPath.getParent();

      srcs = srcFs.globStatus(srcPath);
    }

    return retVal;
  }

  public static void destroyAndStop() {
    int counter = refCount.decrementAndGet();
    if (counter == 0) {
      instance.exitHdfsNotifier = true;
    }
  }

  public FilterData getFilterData(String userClaim) throws IOException {

    Namespace key = new Namespace(this.relevantPath + userClaim);
    FilterData retVal = securityNamespaceCache.get(key);
    if (retVal == null) {
      retVal = findFilterData(config, key.namespace);
      if (retVal != null) {
        securityNamespaceCache.put(key, retVal);
      }

    }
    LOG.info("Got filter data from key (" + key.toString() + ") :" + ((retVal != null) ? retVal.toString() : "NULL"));
    return retVal;
  }

  protected int findAffectedDirs(String srcFile, Set<String> affectedDirs) throws IOException, URISyntaxException {

    Path fullPath = new Path(srcFile);
    Path srcPath = fullPath.getParent();
    String changedFile = fullPath.getName();

    if (!METADATA.equals(changedFile) &&
      !REDACTION_ALLOWED.equals(changedFile) &&
      !REDACTION_DENIED.equals(changedFile) &&
      !REDACTION_DENIED_ALL.equals(changedFile) &&
      !REDACTION_ES_FILTER_QUERY.equals(changedFile) &&
      !REDACTION_TYPE.equals(changedFile)) {
      changedFile = null;
    }


    FileSystem srcFs = srcPath.getFileSystem(config);
    FileStatus[] srcs = srcFs.globStatus(srcPath);
    if (srcs == null || srcs.length == 0) {
      throw new FileNotFoundException("Cannot access " + srcFile +
        ": No such file or directory.");
    }

    int numOfErrors = 0;
    for (int i = 0, ilen = srcs.length; i < ilen; i++) {
      URI tmpUri = new URI(srcs[i].getPath().toString());

      if (srcs[i].isDirectory()) {
        affectedDirs.add(tmpUri.getPath());
      }
      else {
        affectedDirs.add(new Path(tmpUri.getPath()).getParent().toString());
      }
      numOfErrors += findAffectedDirs(srcs[i], srcFs, changedFile, affectedDirs);
    }
    return numOfErrors == 0 ? 0 : -1;
  }

  protected int findAffectedDirs(FileStatus src, FileSystem srcFs, String changedFile, Set<String> affectedDirs) throws IOException, URISyntaxException {
    final FileStatus[] items = shellListStatus(srcFs, src);
    if (items == null) {
      return 1;
    }
    else {
      int numOfErrors = 0;

      for (int i = 0; i < items.length; i++) {
        FileStatus stat = items[i];
        Path cur = stat.getPath();
        URI tmpUri = new URI(cur.toString());


        if (!stat.isDirectory()) {
          // LPPM - this was causing the directory where the change occurred to get removed.
//          if (changedFile != null && changedFile.equals(cur.getName())) {
//            affectedDirs.remove(new Path(tmpUri.getPath()).getParent().toString());
//          }

        }
        else {
          affectedDirs.add(tmpUri.getPath());
          numOfErrors += findAffectedDirs(stat, srcFs, changedFile, affectedDirs);
        }
      }
      return numOfErrors;
    }
  }

  @Override
  public void run() {
    Consumer<String, String> consumer;


    Properties props = new Properties();

//        props.put("bootstrap.servers", "localhost:6667");
    props.put("group.id", "polegroup_sub");
    props.put("enable.auto.commit", "true");
    props.put("auto.commit.interval.ms", "1000");
    props.put("session.timeout.ms", "30000");
    props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
    props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

    props.putAll(this.config.getValByRegex(".*"));

    props.putAll(System.getProperties());


    LOG.info("creating kafka Consumer with the following props: \n" + props.toString());
    Properties conf = KafkaConfig.getConsumerProperties(props);


    consumer = new KafkaConsumer<String, String>(conf);

//        List<String> topics = new ArrayList<>();
//        topics.add(topicStr);
    LOG.info("Subscribing to kafka topic " + topicStr);

    consumer.subscribe(Arrays.asList(topicStr));
    LOG.info("After Subscribing to kafka topic " + topicStr);

    while (!exitHdfsNotifier) {
      try {
        ConsumerRecords<String, String> records = consumer.poll(100);
        for (ConsumerRecord<String, String> record : records) {

          LOG.info(String.format("offset = %d, key = %s, value = %s", record.offset(), record.key(), record.value()));
          String kafkaKey = record.key();
          String val = record.value();

          if ("path".equals(kafkaKey)) {
//                            System.out.println("event type = " + event.getEventType());
            String path = val;
            if (path != null && path.startsWith(relevantPath)) {
              strBuf.setLength(0);
              strBuf.append("Path ").append(path);

              LOG.info(strBuf.toString());
              affectedDirs.clear();
              try {
                findAffectedDirs(path, affectedDirs);
              } catch (Exception e) {
                LOG.error("Failed to process " + strBuf.toString(), e);
              }

              Namespace key = new Namespace();

              for (String affectedDir : affectedDirs) {
                key.namespace = affectedDir;
                if (securityNamespaceCache.remove(key) != null) {
                  FilterData data = findFilterData(config, affectedDir);
                  if (data != null) {
                    LOG.info("Added new Filter Data for " + affectedDir + ": " + data.toString());
                    securityNamespaceCache.put(new Namespace(affectedDir), data);
                  }

                }


              }


            }
//                        else{
//                            strBuf.setLength(0);
//                            strBuf.append("Irrelevant Path ").append(path).append("; event ").append(eventType.name());
//                            LOG.info(strBuf.toString());
//
//                        }


          }

        }
        consumer.commitAsync();
      } catch (Exception e) {
        LOG.error("Failed to process " + strBuf.toString(), e);

      }
    }
  }
//
//
//    @Override
//    public void run() {
//
//
//        while (!exitHdfsNotifier) {
//
//            try {
//
//                GSSManager manager = GSSManager.getInstance();
//
//
//        /*
//         * Create a GSSName out of the server's name. The null
//         * indicates that this application does not wish to make
//         * any claims about the syntax of this name and that the
//         * underlying mechanism should try to parse it as per whatever
//         * default syntax it chooses.
//         */
//                GSSName clientGSSName = manager.createName("hdfs-sandbox@YOUR_REALM_GOES_HERE", null);
//
//                System.out.println("clientGSSName: " + clientGSSName);
//                Subject client =
//                        com.sun.security.jgss.GSSUtil.createSubject(clientGSSName,
//                                null);
//
//                //        LoginContext lc = new LoginContext(String name, Subject subject,
////                CallbackHandler callbackHandler,
////                javax.security.auth.login.Configuration config);
//
//                javax.security.auth.login.Configuration conf = new ConfigFile(new URI("file:///opt/pontus/jaas_policy_store.conf"));
//
//                LoginContext lc = new LoginContext("hdfs", client,
//                        null,
//                        conf);
//
//                lc.login();
//
//
//
///*
//* Invoke the action via a doAsPrivileged. This allows the
//* action to be executed as the client subject, and it also
//* runs that code as privileged. This means that any permission
//* checking that happens beyond this point applies only to
//* the code being run as the client.
//*/
//                Subject.doAsPrivileged(lc.getSubject(), new PrivilegedAction<Object>() {
//                    @Override
//                    public Object run() {
//                        try {
//                            eventStream = admin.getInotifyEventStream();
//
//
//                            while (true) {
//                                EventBatch batch = eventStream.take();
////                        System.out.println("TxId = " + batch.getTxid());
//
//                                for (Event event : batch.getEvents()) {
////                            System.out.println("event type = " + event.getEventType());
//                                    String path = null;
//                                    String owner = null;
//                                    Event.EventType eventType = event.getEventType();
//                                    switch (eventType) {
//                                        case CREATE:
//                                            Event.CreateEvent createEvent = (Event.CreateEvent) event;
//                                            path = createEvent.getPath();
//                                            owner = createEvent.getOwnerName();
////                                    System.out.println("  path = " + path);
////                                    System.out.println("  owner = " + createEvent.getOwnerName());
////                                    System.out.println("  ctime = " + createEvent.getCtime());
//                                            break;
//                                        case UNLINK:
//                                            Event.UnlinkEvent unlinkEvent = (Event.UnlinkEvent) event;
//                                            path = unlinkEvent.getPath();
////                                    System.out.println("  path = " + path);
////                                    System.out.println("  timestamp = " + unlinkEvent.getTimestamp());
//                                            break;
//
//                                        case APPEND:
//                                            Event.AppendEvent appendEvent = (Event.AppendEvent) event;
//                                            path = appendEvent.getPath();
//                                            break;
//                                        case CLOSE:
//                                            Event.CloseEvent closeEvent = (Event.CloseEvent) event;
//                                            path = closeEvent.getPath();
//                                            break;
//                                        case RENAME:
//                                            Event.RenameEvent renameEvent = (Event.RenameEvent) event;
//                                            path = renameEvent.getSrcPath();
//                                            break;
//                                        default:
//                                            break;
//                                    }
//                                    if (path != null && path.startsWith(relevantPath)) {
//                                        strBuf.setLength(0);
//                                        strBuf.append("Path ").append(path).append("; event ").append(eventType.name());
//
//                                        LOG.info(strBuf.toString());
//                                        affectedDirs.clear();
//                                        try {
//                                            findAffectedDirs(path, affectedDirs);
//                                        } catch (URISyntaxException e) {
//                                            e.printStackTrace();
//                                        }
//                                        Namespace key = new Namespace();
//
//                                        for (String affectedDir : affectedDirs) {
//                                            key.namespace = affectedDir;
//                                            if (securityNamespaceCache.remove(key) != null) {
//                                                FilterData data = findFilterData(config, affectedDir);
//                                                if (data != null) {
//                                                    LOG.info("Added new Filter Data for " + affectedDir + ": " + data.toString());
//                                                    securityNamespaceCache.put(new Namespace(affectedDir), data);
//                                                }
//
//                                            }
//
//
//                                        }
//
//
//                                    }
////                        else{
////                            strBuf.setLength(0);
////                            strBuf.append("Irrelevant Path ").append(path).append("; event ").append(eventType.name());
////                            LOG.info(strBuf.toString());
////
////                        }
//
//                                }
//                            }
//                        } catch (Exception e) {
//                            LOG.error("Failed to process changes in the security policy:", e);
//                        }
//                        return null;
//                    }
//                }, null);
//
//
//            } catch (Exception e) {
//                LOG.error("Failed to process changes in the security policy:", e);
//            }
//        }
//    }


}