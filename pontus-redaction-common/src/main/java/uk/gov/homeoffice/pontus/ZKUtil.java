package uk.gov.homeoffice.pontus;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.SecurityUtil;
import org.apache.hadoop.security.authentication.util.KerberosUtil;
import org.apache.htrace.Trace;
import org.apache.htrace.TraceScope;
import org.apache.zookeeper.*;
import org.apache.zookeeper.client.ZooKeeperSaslClient;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.proto.CreateRequest;
import org.apache.zookeeper.proto.SetDataRequest;
import org.apache.zookeeper.server.ZooKeeperSaslServer;

import javax.security.auth.login.AppConfigurationEntry;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by leo on 22/11/2016. Poached from the hbase tree.
 */
public class ZKUtil {

    static class ZKConfig {
        public static final String ZK_CFG_PROPERTY_PREFIX = "hbase.zookeeper.property.";
        public static final int ZK_CFG_PROPERTY_PREFIX_LEN = ZK_CFG_PROPERTY_PREFIX.length();
        public static final String ZK_SESSION_TIMEOUT = "zookeeper.session.timeout";
        public static final int DEFAULT_ZK_SESSION_TIMEOUT = 180000;

        private static final Log LOG = LogFactory.getLog(ZKConfig.class);
        private static final String VARIABLE_START = "${";
        private static final int VARIABLE_START_LENGTH = VARIABLE_START.length();
        private static final String VARIABLE_END = "}";
        private static final int VARIABLE_END_LENGTH = "}".length();

        private ZKConfig() {
        }

        public static Properties makeZKProps(Configuration conf) {
            Properties zkProperties = makeZKPropsFromZooCfg(conf);
            if (zkProperties == null) {
                zkProperties = makeZKPropsFromHbaseConfig(conf);
            }

            return zkProperties;
        }

        private static Properties makeZKPropsFromZooCfg(Configuration conf) {
            if (conf.getBoolean("hbase.config.read.zookeeper.config", false)) {
                LOG.warn("Parsing ZooKeeper\'s zoo.cfg file for ZK properties has been deprecated. Please instead place all ZK related HBase configuration under the hbase-site.xml, using prefixes of the form \'hbase.zookeeper.property.\', and set property \'hbase.config.read.zookeeper.config\' to false");
                ClassLoader cl = ZKConfig.class.getClassLoader();
                InputStream inputStream = cl.getResourceAsStream("zoo.cfg");
                if (inputStream != null) {
                    try {
                        return parseZooCfg(conf, inputStream);
                    } catch (IOException var4) {
                        LOG.warn("Cannot read zoo.cfg, loading from XML files", var4);
                    }
                }
            } else if (LOG.isTraceEnabled()) {
                LOG.trace("Skipped reading ZK properties file \'zoo.cfg\' since \'hbase.config.read.zookeeper.config\' was not set to true");
            }

            return null;
        }

        private static Properties makeZKPropsFromHbaseConfig(Configuration conf) {
            Properties zkProperties = new Properties();
            String serverHost;
            String address;
            String key;
            synchronized (conf) {
                Iterator leaderPort = conf.iterator();

                while (true) {
                    if (!leaderPort.hasNext()) {
                        break;
                    }

                    Map.Entry serverHosts = (Map.Entry) leaderPort.next();
                    serverHost = (String) serverHosts.getKey();
                    if (serverHost.startsWith("hbase.zookeeper.property.")) {
                        address = serverHost.substring(ZK_CFG_PROPERTY_PREFIX_LEN);
                        key = (String) serverHosts.getValue();
                        if (key.contains("${")) {
                            key = conf.get(serverHost);
                        }

                        zkProperties.put(address, key);
                    }
                }
            }

            if (zkProperties.getProperty("clientPort") == null) {
                zkProperties.put("clientPort", Integer.valueOf(2181));
            }

            int peerPort = conf.getInt("hbase.zookeeper.peerport", 2888);
            int var10 = conf.getInt("hbase.zookeeper.leaderport", 3888);
            String[] var11 = conf.getStrings("hbase.zookeeper.quorum", new String[]{"localhost"});

            for (int i = 0; i < var11.length; ++i) {
                if (var11[i].contains(":")) {
                    serverHost = var11[i].substring(0, var11[i].indexOf(58));
                } else {
                    serverHost = var11[i];
                }

                address = serverHost + ":" + peerPort + ":" + var10;
                key = "server." + i;
                zkProperties.put(key, address);
            }

            return zkProperties;
        }

        /**
         * @deprecated
         */
        @Deprecated
        public static Properties parseZooCfg(Configuration conf, InputStream inputStream) throws IOException {
            Properties properties = new Properties();

            try {
                properties.load(inputStream);
            } catch (IOException var13) {
                String entry = "fail to read properties from zoo.cfg";
                LOG.fatal("fail to read properties from zoo.cfg");
                throw new IOException("fail to read properties from zoo.cfg", var13);
            }

            Iterator i$ = properties.entrySet().iterator();

            while (i$.hasNext()) {
                Map.Entry entry1 = (Map.Entry) i$.next();
                String value = entry1.getValue().toString().trim();
                String key = entry1.getKey().toString().trim();
                StringBuilder newValue = new StringBuilder();
                int varStart = value.indexOf("${");

                int varEnd;
                String msg;
                for (varEnd = 0; varStart != -1; varStart = value.indexOf("${", varEnd)) {
                    varEnd = value.indexOf("}", varStart);
                    String mode;
                    if (varEnd == -1) {
                        mode = "variable at " + varStart + " has no end marker";
                        LOG.fatal(mode);
                        throw new IOException(mode);
                    }

                    mode = value.substring(varStart + VARIABLE_START_LENGTH, varEnd);
                    msg = System.getProperty(mode);
                    if (msg == null) {
                        msg = conf.get(mode);
                    }

                    if (msg == null) {
                        String msg1 = "variable " + mode + " not set in system property " + "or hbase configs";
                        LOG.fatal(msg1);
                        throw new IOException(msg1);
                    }

                    newValue.append(msg);
                    varEnd += VARIABLE_END_LENGTH;
                }

                if (key.startsWith("server.")) {
                    boolean mode1 = conf.getBoolean("hbase.cluster.distributed", false);
                    if (mode1 && value.startsWith("localhost")) {
                        msg = "The server in zoo.cfg cannot be set to localhost in a fully-distributed setup because it won\'t be reachable. See \"Getting Started\" for more information.";
                        LOG.fatal(msg);
                        throw new IOException(msg);
                    }
                }

                newValue.append(value.substring(varEnd));
                properties.setProperty(key, newValue.toString());
            }

            return properties;
        }

        private static String getZKQuorumServersString(Properties properties) {
            String clientPort = null;
            ArrayList servers = new ArrayList();
            boolean anyValid = false;
            Iterator hostPortBuilder = properties.entrySet().iterator();

            String host;
            while (hostPortBuilder.hasNext()) {
                Map.Entry i = (Map.Entry) hostPortBuilder.next();
                host = i.getKey().toString().trim();
                String value = i.getValue().toString().trim();
                if (host.equals("clientPort")) {
                    clientPort = value;
                } else if (host.startsWith("server.")) {
                    String host1 = value.substring(0, value.indexOf(58));
                    servers.add(host1);
                    anyValid = true;
                }
            }

            if (!anyValid) {
                LOG.error("no valid quorum servers found in zoo.cfg");
                return null;
            } else if (clientPort == null) {
                LOG.error("no clientPort found in zoo.cfg");
                return null;
            } else if (servers.isEmpty()) {
                LOG.fatal("No servers were found in provided ZooKeeper configuration. HBase must have a ZooKeeper cluster configured for its operation. Ensure that you\'ve configured \'hbase.zookeeper.quorum\' properly.");
                return null;
            } else {
                StringBuilder var9 = new StringBuilder();

                for (int var10 = 0; var10 < servers.size(); ++var10) {
                    host = (String) servers.get(var10);
                    if (var10 > 0) {
                        var9.append(',');
                    }

                    var9.append(host);
                    var9.append(':');
                    var9.append(clientPort);
                }

                return var9.toString();
            }
        }

        private static String getZKQuorumServersStringFromHbaseConfig(Configuration conf) {
            String defaultClientPort = Integer.toString(conf.getInt("hbase.zookeeper.property.clientPort", 2181));
            String[] serverHosts = conf.getStrings("hbase.zookeeper.quorum", new String[]{"localhost"});
            return buildZKQuorumServerString(serverHosts, defaultClientPort);
        }

        public static String getZKQuorumServersString(Configuration conf) {
            Properties zkProperties = makeZKPropsFromZooCfg(conf);
            return zkProperties != null ? getZKQuorumServersString(zkProperties) : getZKQuorumServersStringFromHbaseConfig(conf);
        }

        public static String buildZKQuorumServerString(String[] serverHosts, String clientPort) {
            StringBuilder quorumStringBuilder = new StringBuilder();

            for (int i = 0; i < serverHosts.length; ++i) {
                String serverHost;
                if (serverHosts[i].contains(":")) {
                    serverHost = serverHosts[i];
                } else {
                    serverHost = serverHosts[i] + ":" + clientPort;
                }

                if (i > 0) {
                    quorumStringBuilder.append(',');
                }

                quorumStringBuilder.append(serverHost);
            }

            return quorumStringBuilder.toString();
        }

        public static void validateClusterKey(String key) throws IOException {
            transformClusterKey(key);
        }

        public static ZKConfig.ZKClusterKey transformClusterKey(String key) throws IOException {
            String[] parts = key.split(":");
            if (parts.length == 3) {
                return new ZKConfig.ZKClusterKey(parts[0], Integer.parseInt(parts[1]), parts[2]);
            } else if (parts.length > 3) {
                String zNodeParent = parts[parts.length - 1];
                String clientPort = parts[parts.length - 2];
                int endQuorumIndex = key.length() - zNodeParent.length() - clientPort.length() - 2;
                String quorumStringInput = key.substring(0, endQuorumIndex);
                String[] serverHosts = quorumStringInput.split(",");
                return parts.length - 2 == serverHosts.length + 1 ? new ZKConfig.ZKClusterKey(quorumStringInput, Integer.parseInt(clientPort), zNodeParent) : new ZKConfig.ZKClusterKey(buildZKQuorumServerString(serverHosts, clientPort), Integer.parseInt(clientPort), zNodeParent);
            } else {
                throw new IOException("Cluster key passed " + key + " is invalid, the format should be:" + "hbase.zookeeper.quorum" + ":" + "hbase.zookeeper.property.clientPort" + ":" + "zookeeper.znode.parent");
            }
        }

        public static String getZooKeeperClusterKey(Configuration conf) {
            return getZooKeeperClusterKey(conf, (String) null);
        }

        public static String getZooKeeperClusterKey(Configuration conf, String name) {
            String ensemble = conf.get("hbase.zookeeper.quorum").replaceAll("[\\t\\n\\x0B\\f\\r]", "");
            StringBuilder builder = new StringBuilder(ensemble);
            builder.append(":");
            builder.append(conf.get("hbase.zookeeper.property.clientPort"));
            builder.append(":");
            builder.append(conf.get("zookeeper.znode.parent"));
            if (name != null && !name.isEmpty()) {
                builder.append(",");
                builder.append(name);
            }

            return builder.toString();
        }

        public static String standardizeZKQuorumServerString(String quorumStringInput, String clientPort) {
            String[] serverHosts = quorumStringInput.split(",");
            return buildZKQuorumServerString(serverHosts, clientPort);
        }

        public static class ZKClusterKey {
            private String quorumString;
            private int clientPort;
            private String znodeParent;

            ZKClusterKey(String quorumString, int clientPort, String znodeParent) {
                this.quorumString = quorumString;
                this.clientPort = clientPort;
                this.znodeParent = znodeParent;
            }

            public String getQuorumString() {
                return this.quorumString;
            }

            public int getClientPort() {
                return this.clientPort;
            }

            public String getZnodeParent() {
                return this.znodeParent;
            }
        }
    }

    static public class RetryCounter {
        private static final Log LOG = LogFactory.getLog(RetryCounter.class);
        private RetryConfig retryConfig;
        private int attempts;

        public RetryCounter(int maxAttempts, long sleepInterval, TimeUnit timeUnit) {
            this(new RetryConfig(maxAttempts, sleepInterval, -1L, timeUnit, new RetryCounter.ExponentialBackoffPolicy()));
        }

        public RetryCounter(RetryConfig retryConfig) {
            this.attempts = 0;
            this.retryConfig = retryConfig;
        }

        public int getMaxAttempts() {
            return this.retryConfig.getMaxAttempts();
        }

        public void sleepUntilNextRetry() throws InterruptedException {
            int attempts = this.getAttemptTimes();
            long sleepTime = this.retryConfig.backoffPolicy.getBackoffTime(this.retryConfig, attempts);
            if (LOG.isTraceEnabled()) {
                LOG.trace("Sleeping " + sleepTime + "ms before retry #" + attempts + "...");
            }

            this.retryConfig.getTimeUnit().sleep(sleepTime);
            this.useRetry();
        }

        public boolean shouldRetry() {
            return this.attempts < this.retryConfig.getMaxAttempts();
        }

        public void useRetry() {
            ++this.attempts;
        }

        public boolean isRetry() {
            return this.attempts > 0;
        }

        public int getAttemptTimes() {
            return this.attempts;
        }

        static class ExponentialBackoffPolicyWithLimit extends RetryCounter.ExponentialBackoffPolicy {
            public ExponentialBackoffPolicyWithLimit() {
            }

            public long getBackoffTime(RetryConfig config, int attempts) {
                long backoffTime = super.getBackoffTime(config, attempts);
                return config.getMaxSleepTime() > 0L ? Math.min(backoffTime, config.getMaxSleepTime()) : backoffTime;
            }
        }

        public static class ExponentialBackoffPolicy extends RetryCounter.BackoffPolicy {
            public ExponentialBackoffPolicy() {
            }

            public long getBackoffTime(RetryConfig config, int attempts) {
                long backoffTime = (long) ((double) config.getSleepInterval() * Math.pow(2.0D, (double) attempts));
                return backoffTime;
            }
        }

        public static class BackoffPolicy {
            public BackoffPolicy() {
            }

            public long getBackoffTime(RetryConfig config, int attempts) {
                return config.getSleepInterval();
            }
        }

    }

    public static class RetryConfig {
        private int maxAttempts;
        private long sleepInterval;
        private long maxSleepTime;
        private TimeUnit timeUnit;
        private RetryCounter.BackoffPolicy backoffPolicy;
        private static final RetryCounter.BackoffPolicy DEFAULT_BACKOFF_POLICY = new RetryCounter.ExponentialBackoffPolicy();

        public RetryConfig() {
            this.maxAttempts = 1;
            this.sleepInterval = 1000L;
            this.maxSleepTime = -1L;
            this.timeUnit = TimeUnit.MILLISECONDS;
            this.backoffPolicy = DEFAULT_BACKOFF_POLICY;
        }

        public RetryConfig(int maxAttempts, long sleepInterval, long maxSleepTime, TimeUnit timeUnit, RetryCounter.BackoffPolicy backoffPolicy) {
            this.maxAttempts = maxAttempts;
            this.sleepInterval = sleepInterval;
            this.maxSleepTime = maxSleepTime;
            this.timeUnit = timeUnit;
            this.backoffPolicy = backoffPolicy;
        }

        public RetryConfig setBackoffPolicy(RetryCounter.BackoffPolicy backoffPolicy) {
            this.backoffPolicy = backoffPolicy;
            return this;
        }

        public RetryConfig setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        public RetryConfig setMaxSleepTime(long maxSleepTime) {
            this.maxSleepTime = maxSleepTime;
            return this;
        }

        public RetryConfig setSleepInterval(long sleepInterval) {
            this.sleepInterval = sleepInterval;
            return this;
        }

        public RetryConfig setTimeUnit(TimeUnit timeUnit) {
            this.timeUnit = timeUnit;
            return this;
        }

        public int getMaxAttempts() {
            return this.maxAttempts;
        }

        public long getMaxSleepTime() {
            return this.maxSleepTime;
        }

        public long getSleepInterval() {
            return this.sleepInterval;
        }

        public TimeUnit getTimeUnit() {
            return this.timeUnit;
        }

        public RetryCounter.BackoffPolicy getBackoffPolicy() {
            return this.backoffPolicy;
        }
    }
    static class ExponentialBackoffPolicyWithLimit extends RetryCounter.ExponentialBackoffPolicy {
        public ExponentialBackoffPolicyWithLimit() {
        }

        public long getBackoffTime(RetryConfig config, int attempts) {
            long backoffTime = super.getBackoffTime(config, attempts);
            return config.getMaxSleepTime() > 0L?Math.min(backoffTime, config.getMaxSleepTime()):backoffTime;
        }
    }

    static class RetryCounterFactory {
        private final RetryConfig retryConfig;

        public RetryCounterFactory(int maxAttempts, int sleepIntervalMillis) {
            this(maxAttempts, sleepIntervalMillis, -1);
        }

        public RetryCounterFactory(int maxAttempts, int sleepIntervalMillis, int maxSleepTime) {
            this(new RetryConfig(maxAttempts, (long) sleepIntervalMillis, (long) maxSleepTime, TimeUnit.MILLISECONDS, new ExponentialBackoffPolicyWithLimit()));
        }

        public RetryCounterFactory(RetryConfig retryConfig) {
            this.retryConfig = retryConfig;
        }

        public RetryCounter create() {
            return new RetryCounter(this.retryConfig);
        }
    }

    static class RecoverableZooKeeper {
        private static final Log LOG = LogFactory.getLog(RecoverableZooKeeper.class);
        // the actual ZooKeeper client instance
        private ZooKeeper zk;
        private final RetryCounterFactory retryCounterFactory;
        // An identifier of this process in the cluster
        private final String identifier;
        private final byte[] id;
        private Watcher watcher;
        private int sessionTimeout;
        private String quorumServers;
        private final Random salter;

        // The metadata attached to each piece of data has the
        // format:
        //   <magic> 1-byte constant
        //   <id length> 4-byte big-endian integer (length of next field)
        //   <id> identifier corresponding uniquely to this process
        // It is prepended to the data supplied by the user.

        // the magic number is to be backward compatible
        private static final byte MAGIC = (byte) 0XFF;
        private static final int MAGIC_SIZE = Bytes.SIZEOF_BYTE;
        private static final int ID_LENGTH_OFFSET = MAGIC_SIZE;
        private static final int ID_LENGTH_SIZE = Bytes.SIZEOF_INT;

        public RecoverableZooKeeper(String quorumServers, int sessionTimeout,
                                    Watcher watcher, int maxRetries, int retryIntervalMillis)
                throws IOException {
            this(quorumServers, sessionTimeout, watcher, maxRetries, retryIntervalMillis,
                    null);
        }

        public RecoverableZooKeeper(String quorumServers, int sessionTimeout,
                                    Watcher watcher, int maxRetries, int retryIntervalMillis, String identifier)
                throws IOException {
            // TODO: Add support for zk 'chroot'; we don't add it to the quorumServers String as we should.
            this.retryCounterFactory =
                    new RetryCounterFactory(maxRetries + 1, retryIntervalMillis);

            if (identifier == null || identifier.length() == 0) {
                // the identifier = processID@hostName
                identifier = ManagementFactory.getRuntimeMXBean().getName();
            }
            LOG.info("Process identifier=" + identifier +
                    " connecting to ZooKeeper ensemble=" + quorumServers);
            this.identifier = identifier;
            this.id = Bytes.toBytes(identifier);

            this.watcher = watcher;
            this.sessionTimeout = sessionTimeout;
            this.quorumServers = quorumServers;
            try {
                checkZk();
            } catch (Exception x) {/* ignore */}
            salter = new Random();
        }

        /**
         * Try to create a Zookeeper connection. Turns any exception encountered into a
         * KeeperException.OperationTimeoutException so it can retried.
         *
         * @return The created Zookeeper connection object
         * @throws KeeperException
         */
        public synchronized ZooKeeper checkZk() throws KeeperException {
            if (this.zk == null) {
                try {
                    this.zk = new ZooKeeper(quorumServers, sessionTimeout, watcher);
                } catch (IOException ex) {
                    LOG.warn("Unable to create ZooKeeper Connection", ex);
                    throw new KeeperException.OperationTimeoutException();
                }
            }
            return zk;
        }

        public synchronized void reconnectAfterExpiration()
                throws IOException, KeeperException, InterruptedException {
            if (zk != null) {
                LOG.info("Closing dead ZooKeeper connection, session" +
                        " was: 0x" + Long.toHexString(zk.getSessionId()));
                zk.close();
                // reset the Zookeeper connection
                zk = null;
            }
            checkZk();
            LOG.info("Recreated a ZooKeeper, session" +
                    " is: 0x" + Long.toHexString(zk.getSessionId()));
        }

        /**
         * delete is an idempotent operation. Retry before throwing exception.
         * This function will not throw NoNodeException if the path does not
         * exist.
         */
        public void delete(String path, int version)
                throws InterruptedException, KeeperException {
            TraceScope traceScope = null;
            try {
                traceScope = Trace.startSpan("RecoverableZookeeper.delete");
                RetryCounter retryCounter = retryCounterFactory.create();
                boolean isRetry = false; // False for first attempt, true for all retries.
                while (true) {
                    try {
                        checkZk().delete(path, version);
                        return;
                    } catch (KeeperException e) {
                        switch (e.code()) {
                            case NONODE:
                                if (isRetry) {
                                    LOG.info("Node " + path + " already deleted. Assuming a " +
                                            "previous attempt succeeded.");
                                    return;
                                }
                                LOG.info("Node " + path + " already deleted, retry=" + isRetry);
                                throw e;

                            case CONNECTIONLOSS:
                            case SESSIONEXPIRED:
                            case OPERATIONTIMEOUT:
                                retryOrThrow(retryCounter, e, "delete");
                                break;

                            default:
                                throw e;
                        }
                    }
                    retryCounter.sleepUntilNextRetry();
                    isRetry = true;
                }
            } finally {
                if (traceScope != null) traceScope.close();
            }
        }

        /**
         * exists is an idempotent operation. Retry before throwing exception
         *
         * @return A Stat instance
         */
        public Stat exists(String path, Watcher watcher)
                throws KeeperException, InterruptedException {
            TraceScope traceScope = null;
            try {
                traceScope = Trace.startSpan("RecoverableZookeeper.exists");
                RetryCounter retryCounter = retryCounterFactory.create();
                while (true) {
                    try {
                        return checkZk().exists(path, watcher);
                    } catch (KeeperException e) {
                        switch (e.code()) {
                            case CONNECTIONLOSS:
                            case SESSIONEXPIRED:
                            case OPERATIONTIMEOUT:
                                retryOrThrow(retryCounter, e, "exists");
                                break;

                            default:
                                throw e;
                        }
                    }
                    retryCounter.sleepUntilNextRetry();
                }
            } finally {
                if (traceScope != null) traceScope.close();
            }
        }

        /**
         * exists is an idempotent operation. Retry before throwing exception
         *
         * @return A Stat instance
         */
        public Stat exists(String path, boolean watch)
                throws KeeperException, InterruptedException {
            TraceScope traceScope = null;
            try {
                traceScope = Trace.startSpan("RecoverableZookeeper.exists");
                RetryCounter retryCounter = retryCounterFactory.create();
                while (true) {
                    try {
                        return checkZk().exists(path, watch);
                    } catch (KeeperException e) {
                        switch (e.code()) {
                            case CONNECTIONLOSS:
                            case SESSIONEXPIRED:
                            case OPERATIONTIMEOUT:
                                retryOrThrow(retryCounter, e, "exists");
                                break;

                            default:
                                throw e;
                        }
                    }
                    retryCounter.sleepUntilNextRetry();
                }
            } finally {
                if (traceScope != null) traceScope.close();
            }
        }

        private void retryOrThrow(RetryCounter retryCounter, KeeperException e,
                                  String opName) throws KeeperException {
            LOG.warn("Possibly transient ZooKeeper, quorum=" + quorumServers + ", exception=" + e);
            if (!retryCounter.shouldRetry()) {
                LOG.error("ZooKeeper " + opName + " failed after "
                        + retryCounter.getMaxAttempts() + " attempts");
                throw e;
            }
        }

        /**
         * getChildren is an idempotent operation. Retry before throwing exception
         *
         * @return List of children znodes
         */
        public List<String> getChildren(String path, Watcher watcher)
                throws KeeperException, InterruptedException {
            TraceScope traceScope = null;
            try {
                traceScope = Trace.startSpan("RecoverableZookeeper.getChildren");
                RetryCounter retryCounter = retryCounterFactory.create();
                while (true) {
                    try {
                        return checkZk().getChildren(path, watcher);
                    } catch (KeeperException e) {
                        switch (e.code()) {
                            case CONNECTIONLOSS:
                            case SESSIONEXPIRED:
                            case OPERATIONTIMEOUT:
                                retryOrThrow(retryCounter, e, "getChildren");
                                break;

                            default:
                                throw e;
                        }
                    }
                    retryCounter.sleepUntilNextRetry();
                }
            } finally {
                if (traceScope != null) traceScope.close();
            }
        }

        /**
         * getChildren is an idempotent operation. Retry before throwing exception
         *
         * @return List of children znodes
         */
        public List<String> getChildren(String path, boolean watch)
                throws KeeperException, InterruptedException {
            TraceScope traceScope = null;
            try {
                traceScope = Trace.startSpan("RecoverableZookeeper.getChildren");
                RetryCounter retryCounter = retryCounterFactory.create();
                while (true) {
                    try {
                        return checkZk().getChildren(path, watch);
                    } catch (KeeperException e) {
                        switch (e.code()) {
                            case CONNECTIONLOSS:
                            case SESSIONEXPIRED:
                            case OPERATIONTIMEOUT:
                                retryOrThrow(retryCounter, e, "getChildren");
                                break;

                            default:
                                throw e;
                        }
                    }
                    retryCounter.sleepUntilNextRetry();
                }
            } finally {
                if (traceScope != null) traceScope.close();
            }
        }

        /**
         * getData is an idempotent operation. Retry before throwing exception
         *
         * @return Data
         */
        public byte[] getData(String path, Watcher watcher, Stat stat)
                throws KeeperException, InterruptedException {
            TraceScope traceScope = null;
            try {
                traceScope = Trace.startSpan("RecoverableZookeeper.getData");
                RetryCounter retryCounter = retryCounterFactory.create();
                while (true) {
                    try {
                        byte[] revData = checkZk().getData(path, watcher, stat);
                        return this.removeMetaData(revData);
                    } catch (KeeperException e) {
                        switch (e.code()) {
                            case CONNECTIONLOSS:
                            case SESSIONEXPIRED:
                            case OPERATIONTIMEOUT:
                                retryOrThrow(retryCounter, e, "getData");
                                break;

                            default:
                                throw e;
                        }
                    }
                    retryCounter.sleepUntilNextRetry();
                }
            } finally {
                if (traceScope != null) traceScope.close();
            }
        }

        /**
         * getData is an idemnpotent operation. Retry before throwing exception
         *
         * @return Data
         */
        public byte[] getData(String path, boolean watch, Stat stat)
                throws KeeperException, InterruptedException {
            TraceScope traceScope = null;
            try {
                traceScope = Trace.startSpan("RecoverableZookeeper.getData");
                RetryCounter retryCounter = retryCounterFactory.create();
                while (true) {
                    try {
                        byte[] revData = checkZk().getData(path, watch, stat);
                        return this.removeMetaData(revData);
                    } catch (KeeperException e) {
                        switch (e.code()) {
                            case CONNECTIONLOSS:
                            case SESSIONEXPIRED:
                            case OPERATIONTIMEOUT:
                                retryOrThrow(retryCounter, e, "getData");
                                break;

                            default:
                                throw e;
                        }
                    }
                    retryCounter.sleepUntilNextRetry();
                }
            } finally {
                if (traceScope != null) traceScope.close();
            }
        }

        /**
         * setData is NOT an idempotent operation. Retry may cause BadVersion Exception
         * Adding an identifier field into the data to check whether
         * badversion is caused by the result of previous correctly setData
         *
         * @return Stat instance
         */
        public Stat setData(String path, byte[] data, int version)
                throws KeeperException, InterruptedException {
            TraceScope traceScope = null;
            try {
                traceScope = Trace.startSpan("RecoverableZookeeper.setData");
                RetryCounter retryCounter = retryCounterFactory.create();
                byte[] newData = appendMetaData(data);
                boolean isRetry = false;
                while (true) {
                    try {
                        return checkZk().setData(path, newData, version);
                    } catch (KeeperException e) {
                        switch (e.code()) {
                            case CONNECTIONLOSS:
                            case SESSIONEXPIRED:
                            case OPERATIONTIMEOUT:
                                retryOrThrow(retryCounter, e, "setData");
                                break;
                            case BADVERSION:
                                if (isRetry) {
                                    // try to verify whether the previous setData success or not
                                    try {
                                        Stat stat = new Stat();
                                        byte[] revData = checkZk().getData(path, false, stat);
                                        if (Bytes.compareTo(revData, newData) == 0) {
                                            // the bad version is caused by previous successful setData
                                            return stat;
                                        }
                                    } catch (KeeperException keeperException) {
                                        // the ZK is not reliable at this moment. just throwing exception
                                        throw keeperException;
                                    }
                                }
                                // throw other exceptions and verified bad version exceptions
                            default:
                                throw e;
                        }
                    }
                    retryCounter.sleepUntilNextRetry();
                    isRetry = true;
                }
            } finally {
                if (traceScope != null) traceScope.close();
            }
        }

        /**
         * getAcl is an idempotent operation. Retry before throwing exception
         *
         * @return list of ACLs
         */
        public List<ACL> getAcl(String path, Stat stat)
                throws KeeperException, InterruptedException {
            TraceScope traceScope = null;
            try {
                traceScope = Trace.startSpan("RecoverableZookeeper.getAcl");
                RetryCounter retryCounter = retryCounterFactory.create();
                while (true) {
                    try {
                        return checkZk().getACL(path, stat);
                    } catch (KeeperException e) {
                        switch (e.code()) {
                            case CONNECTIONLOSS:
                            case SESSIONEXPIRED:
                            case OPERATIONTIMEOUT:
                                retryOrThrow(retryCounter, e, "getAcl");
                                break;

                            default:
                                throw e;
                        }
                    }
                    retryCounter.sleepUntilNextRetry();
                }
            } finally {
                if (traceScope != null) traceScope.close();
            }
        }

        /**
         * setAcl is an idempotent operation. Retry before throwing exception
         *
         * @return list of ACLs
         */
        public Stat setAcl(String path, List<ACL> acls, int version)
                throws KeeperException, InterruptedException {
            TraceScope traceScope = null;
            try {
                traceScope = Trace.startSpan("RecoverableZookeeper.setAcl");
                RetryCounter retryCounter = retryCounterFactory.create();
                while (true) {
                    try {
                        return checkZk().setACL(path, acls, version);
                    } catch (KeeperException e) {
                        switch (e.code()) {
                            case CONNECTIONLOSS:
                            case SESSIONEXPIRED:
                            case OPERATIONTIMEOUT:
                                retryOrThrow(retryCounter, e, "setAcl");
                                break;

                            default:
                                throw e;
                        }
                    }
                    retryCounter.sleepUntilNextRetry();
                }
            } finally {
                if (traceScope != null) traceScope.close();
            }
        }

        /**
         * <p>
         * NONSEQUENTIAL create is idempotent operation.
         * Retry before throwing exceptions.
         * But this function will not throw the NodeExist exception back to the
         * application.
         * </p>
         * <p>
         * But SEQUENTIAL is NOT idempotent operation. It is necessary to add
         * identifier to the path to verify, whether the previous one is successful
         * or not.
         * </p>
         *
         * @return Path
         */
        public String create(String path, byte[] data, List<ACL> acl,
                             CreateMode createMode)
                throws KeeperException, InterruptedException {
            TraceScope traceScope = null;
            try {
                traceScope = Trace.startSpan("RecoverableZookeeper.create");
                byte[] newData = appendMetaData(data);
                switch (createMode) {
                    case EPHEMERAL:
                    case PERSISTENT:
                        return createNonSequential(path, newData, acl, createMode);

                    case EPHEMERAL_SEQUENTIAL:
                    case PERSISTENT_SEQUENTIAL:
                        return createSequential(path, newData, acl, createMode);

                    default:
                        throw new IllegalArgumentException("Unrecognized CreateMode: " +
                                createMode);
                }
            } finally {
                if (traceScope != null) traceScope.close();
            }
        }

        private String createNonSequential(String path, byte[] data, List<ACL> acl,
                                           CreateMode createMode) throws KeeperException, InterruptedException {
            RetryCounter retryCounter = retryCounterFactory.create();
            boolean isRetry = false; // False for first attempt, true for all retries.
            while (true) {
                try {
                    return checkZk().create(path, data, acl, createMode);
                } catch (KeeperException e) {
                    switch (e.code()) {
                        case NODEEXISTS:
                            if (isRetry) {
                                // If the connection was lost, there is still a possibility that
                                // we have successfully created the node at our previous attempt,
                                // so we read the node and compare.
                                byte[] currentData = checkZk().getData(path, false, null);
                                if (currentData != null &&
                                        Bytes.compareTo(currentData, data) == 0) {
                                    // We successfully created a non-sequential node
                                    return path;
                                }
                                LOG.error("Node " + path + " already exists with " +
                                        Bytes.toStringBinary(currentData) + ", could not write " +
                                        Bytes.toStringBinary(data));
                                throw e;
                            }
                            LOG.debug("Node " + path + " already exists");
                            throw e;

                        case CONNECTIONLOSS:
                        case SESSIONEXPIRED:
                        case OPERATIONTIMEOUT:
                            retryOrThrow(retryCounter, e, "create");
                            break;

                        default:
                            throw e;
                    }
                }
                retryCounter.sleepUntilNextRetry();
                isRetry = true;
            }
        }

        private String createSequential(String path, byte[] data,
                                        List<ACL> acl, CreateMode createMode)
                throws KeeperException, InterruptedException {
            RetryCounter retryCounter = retryCounterFactory.create();
            boolean first = true;
            String newPath = path + this.identifier;
            while (true) {
                try {
                    if (!first) {
                        // Check if we succeeded on a previous attempt
                        String previousResult = findPreviousSequentialNode(newPath);
                        if (previousResult != null) {
                            return previousResult;
                        }
                    }
                    first = false;
                    return checkZk().create(newPath, data, acl, createMode);
                } catch (KeeperException e) {
                    switch (e.code()) {
                        case CONNECTIONLOSS:
                        case SESSIONEXPIRED:
                        case OPERATIONTIMEOUT:
                            retryOrThrow(retryCounter, e, "create");
                            break;

                        default:
                            throw e;
                    }
                }
                retryCounter.sleepUntilNextRetry();
            }
        }

        private Iterable<Op> prepareZKMulti(Iterable<Op> ops)
                throws UnsupportedOperationException {
            if (ops == null) return null;

            List<Op> preparedOps = new LinkedList<Op>();
            for (Op op : ops) {
                if (op.getType() == ZooDefs.OpCode.create) {
                    CreateRequest create = (CreateRequest) op.toRequestRecord();
                    preparedOps.add(Op.create(create.getPath(), appendMetaData(create.getData()),
                            create.getAcl(), create.getFlags()));
                } else if (op.getType() == ZooDefs.OpCode.delete) {
                    // no need to appendMetaData for delete
                    preparedOps.add(op);
                } else if (op.getType() == ZooDefs.OpCode.setData) {
                    SetDataRequest setData = (SetDataRequest) op.toRequestRecord();
                    preparedOps.add(Op.setData(setData.getPath(), appendMetaData(setData.getData()),
                            setData.getVersion()));
                } else {
                    throw new UnsupportedOperationException("Unexpected ZKOp type: " + op.getClass().getName());
                }
            }
            return preparedOps;
        }

        /**
         * Run multiple operations in a transactional manner. Retry before throwing exception
         */
        public List<OpResult> multi(Iterable<Op> ops)
                throws KeeperException, InterruptedException {
            TraceScope traceScope = null;
            try {
                traceScope = Trace.startSpan("RecoverableZookeeper.multi");
                RetryCounter retryCounter = retryCounterFactory.create();
                Iterable<Op> multiOps = prepareZKMulti(ops);
                while (true) {
                    try {
                        return checkZk().multi(multiOps);
                    } catch (KeeperException e) {
                        switch (e.code()) {
                            case CONNECTIONLOSS:
                            case SESSIONEXPIRED:
                            case OPERATIONTIMEOUT:
                                retryOrThrow(retryCounter, e, "multi");
                                break;

                            default:
                                throw e;
                        }
                    }
                    retryCounter.sleepUntilNextRetry();
                }
            } finally {
                if (traceScope != null) traceScope.close();
            }
        }

        private String findPreviousSequentialNode(String path)
                throws KeeperException, InterruptedException {
            int lastSlashIdx = path.lastIndexOf('/');
            assert (lastSlashIdx != -1);
            String parent = path.substring(0, lastSlashIdx);
            String nodePrefix = path.substring(lastSlashIdx + 1);

            List<String> nodes = checkZk().getChildren(parent, false);
            List<String> matching = filterByPrefix(nodes, nodePrefix);
            for (String node : matching) {
                String nodePath = parent + "/" + node;
                Stat stat = checkZk().exists(nodePath, false);
                if (stat != null) {
                    return nodePath;
                }
            }
            return null;
        }

        public byte[] removeMetaData(byte[] data) {
            if (data == null || data.length == 0) {
                return data;
            }
            // check the magic data; to be backward compatible
            byte magic = data[0];
            if (magic != MAGIC) {
                return data;
            }

            int idLength = Bytes.toInt(data, ID_LENGTH_OFFSET);
            int dataLength = data.length - MAGIC_SIZE - ID_LENGTH_SIZE - idLength;
            int dataOffset = MAGIC_SIZE + ID_LENGTH_SIZE + idLength;

            byte[] newData = new byte[dataLength];
            System.arraycopy(data, dataOffset, newData, 0, dataLength);
            return newData;
        }

        private byte[] appendMetaData(byte[] data) {
            if (data == null || data.length == 0) {
                return data;
            }
            byte[] salt = Bytes.toBytes(salter.nextLong());
            int idLength = id.length + salt.length;
            byte[] newData = new byte[MAGIC_SIZE + ID_LENGTH_SIZE + idLength + data.length];
            int pos = 0;
            pos = Bytes.putByte(newData, pos, MAGIC);
            pos = Bytes.putInt(newData, pos, idLength);
            pos = Bytes.putBytes(newData, pos, id, 0, id.length);
            pos = Bytes.putBytes(newData, pos, salt, 0, salt.length);
            pos = Bytes.putBytes(newData, pos, data, 0, data.length);
            return newData;
        }

        public synchronized long getSessionId() {
            return zk == null ? -1 : zk.getSessionId();
        }

        public synchronized void close() throws InterruptedException {
            if (zk != null) zk.close();
        }

        public synchronized ZooKeeper.States getState() {
            return zk == null ? null : zk.getState();
        }

        public synchronized ZooKeeper getZooKeeper() {
            return zk;
        }

        public synchronized byte[] getSessionPasswd() {
            return zk == null ? null : zk.getSessionPasswd();
        }

        public void sync(String path, AsyncCallback.VoidCallback cb, Object ctx) throws KeeperException {
            checkZk().sync(path, null, null);
        }

        /**
         * Filters the given node list by the given prefixes.
         * This method is all-inclusive--if any element in the node list starts
         * with any of the given prefixes, then it is included in the result.
         *
         * @param nodes    the nodes to filter
         * @param prefixes the prefixes to include in the result
         * @return list of every element that starts with one of the prefixes
         */
        private static List<String> filterByPrefix(List<String> nodes,
                                                   String... prefixes) {
            List<String> lockChildren = new ArrayList<String>();
            for (String child : nodes) {
                for (String prefix : prefixes) {
                    if (child.startsWith(prefix)) {
                        lockChildren.add(child);
                        break;
                    }
                }
            }
            return lockChildren;
        }

        public String getIdentifier() {
            return identifier;
        }
    }


    private static final Log LOG = LogFactory.getLog(ZKUtil.class);

    // TODO: Replace this with ZooKeeper constant when ZOOKEEPER-277 is resolved.
    public static final char ZNODE_PATH_SEPARATOR = '/';
    private static int zkDumpConnectionTimeOut;

    /**
     * Creates a new connection to ZooKeeper, pulling settings and ensemble config
     * from the specified configuration object using methods from {@link ZKConfig}.
     * <p>
     * Sets the connection status monitoring watcher to the specified watcher.
     *
     * @param conf    configuration to pull ensemble and other settings from
     * @param watcher watcher to monitor connection changes
     * @return connection to zookeeper
     * @throws IOException if unable to connect to zk or config problem
     */
    public static RecoverableZooKeeper connect(Configuration conf, Watcher watcher)
            throws IOException {
        String ensemble = ZKConfig.getZKQuorumServersString(conf);
        return connect(conf, ensemble, watcher);
    }

    public static RecoverableZooKeeper connect(Configuration conf, String ensemble,
                                               Watcher watcher)
            throws IOException {
        return connect(conf, ensemble, watcher, null);
    }

    public static RecoverableZooKeeper connect(Configuration conf, String ensemble,
                                               Watcher watcher, final String identifier)
            throws IOException {
        if (ensemble == null) {
            throw new IOException("Unable to determine ZooKeeper ensemble");
        }
        int timeout = conf.getInt(ZKConfig.ZK_SESSION_TIMEOUT,
                ZKConfig.DEFAULT_ZK_SESSION_TIMEOUT);
        if (LOG.isTraceEnabled()) {
            LOG.trace(identifier + " opening connection to ZooKeeper ensemble=" + ensemble);
        }
        int retry = conf.getInt("zookeeper.recovery.retry", 3);
        int retryIntervalMillis =
                conf.getInt("zookeeper.recovery.retry.intervalmill", 1000);
        zkDumpConnectionTimeOut = conf.getInt("zookeeper.dump.connection.timeout",
                1000);
        return new RecoverableZooKeeper(ensemble, timeout, watcher,
                retry, retryIntervalMillis, identifier);
    }

    /**
     * Log in the current zookeeper server process using the given configuration
     * keys for the credential file and login principal.
     * <p>
     * <p><strong>This is only applicable when running on secure hbase</strong>
     * On regular HBase (without security features), this will safely be ignored.
     * </p>
     *
     * @param conf          The configuration data to use
     * @param keytabFileKey Property key used to configure the path to the credential file
     * @param userNameKey   Property key used to configure the login principal
     * @param hostname      Current hostname to use in any credentials
     * @throws IOException underlying exception from SecurityUtil.login() call
     */
    public static void loginServer(Configuration conf, String keytabFileKey,
                                   String userNameKey, String hostname) throws IOException {
        login(conf, keytabFileKey, userNameKey, hostname,
                ZooKeeperSaslServer.LOGIN_CONTEXT_NAME_KEY,
                JaasConfiguration.SERVER_KEYTAB_KERBEROS_CONFIG_NAME);
    }

    /**
     * Log in the current zookeeper client using the given configuration
     * keys for the credential file and login principal.
     * <p>
     * <p><strong>This is only applicable when running on secure hbase</strong>
     * On regular HBase (without security features), this will safely be ignored.
     * </p>
     *
     * @param conf          The configuration data to use
     * @param keytabFileKey Property key used to configure the path to the credential file
     * @param userNameKey   Property key used to configure the login principal
     * @param hostname      Current hostname to use in any credentials
     * @throws IOException underlying exception from SecurityUtil.login() call
     */
    public static void loginClient(Configuration conf, String keytabFileKey,
                                   String userNameKey, String hostname) throws IOException {
        login(conf, keytabFileKey, userNameKey, hostname,
                ZooKeeperSaslClient.LOGIN_CONTEXT_NAME_KEY,
                JaasConfiguration.CLIENT_KEYTAB_KERBEROS_CONFIG_NAME);
    }

    /**
     * Log in the current process using the given configuration keys for the
     * credential file and login principal.
     * <p>
     * <p><strong>This is only applicable when running on secure hbase</strong>
     * On regular HBase (without security features), this will safely be ignored.
     * </p>
     *
     * @param conf                 The configuration data to use
     * @param keytabFileKey        Property key used to configure the path to the credential file
     * @param userNameKey          Property key used to configure the login principal
     * @param hostname             Current hostname to use in any credentials
     * @param loginContextProperty property name to expose the entry name
     * @param loginContextName     jaas entry name
     * @throws IOException underlying exception from SecurityUtil.login() call
     */
    private static void login(Configuration conf, String keytabFileKey,
                              String userNameKey, String hostname,
                              String loginContextProperty, String loginContextName)
            throws IOException {
        if (!isSecureZooKeeper(conf))
            return;

        // User has specified a jaas.conf, keep this one as the good one.
        // HBASE_OPTS="-Djava.security.auth.login.config=jaas.conf"
        if (System.getProperty("java.security.auth.login.config") != null)
            return;

        // No keytab specified, no auth
        String keytabFilename = conf.get(keytabFileKey);
        if (keytabFilename == null) {
            LOG.warn("no keytab specified for: " + keytabFileKey);
            return;
        }

        String principalConfig = conf.get(userNameKey, System.getProperty("user.name"));
        String principalName = SecurityUtil.getServerPrincipal(principalConfig, hostname);

        // Initialize the "jaas.conf" for keyTab/principal,
        // If keyTab is not specified use the Ticket Cache.
        // and set the zookeeper login context name.
        JaasConfiguration jaasConf = new JaasConfiguration(loginContextName,
                principalName, keytabFilename);
        javax.security.auth.login.Configuration.setConfiguration(jaasConf);
        System.setProperty(loginContextProperty, loginContextName);
    }

    /**
     * A JAAS configuration that defines the login modules that we want to use for login.
     */
    private static class JaasConfiguration extends javax.security.auth.login.Configuration {
        private static final String SERVER_KEYTAB_KERBEROS_CONFIG_NAME =
                "zookeeper-server-keytab-kerberos";
        private static final String CLIENT_KEYTAB_KERBEROS_CONFIG_NAME =
                "zookeeper-client-keytab-kerberos";

        private static final Map<String, String> BASIC_JAAS_OPTIONS =
                new HashMap<String, String>();

        static {
            String jaasEnvVar = System.getenv("HBASE_JAAS_DEBUG");
            if (jaasEnvVar != null && "true".equalsIgnoreCase(jaasEnvVar)) {
                BASIC_JAAS_OPTIONS.put("debug", "true");
            }
        }

        private static final Map<String, String> KEYTAB_KERBEROS_OPTIONS =
                new HashMap<String, String>();

        static {
            KEYTAB_KERBEROS_OPTIONS.put("doNotPrompt", "true");
            KEYTAB_KERBEROS_OPTIONS.put("storeKey", "true");
            KEYTAB_KERBEROS_OPTIONS.put("refreshKrb5Config", "true");
            KEYTAB_KERBEROS_OPTIONS.putAll(BASIC_JAAS_OPTIONS);
        }

        private static final AppConfigurationEntry KEYTAB_KERBEROS_LOGIN =
                new AppConfigurationEntry(KerberosUtil.getKrb5LoginModuleName(),
                        AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                        KEYTAB_KERBEROS_OPTIONS);

        private static final AppConfigurationEntry[] KEYTAB_KERBEROS_CONF =
                new AppConfigurationEntry[]{KEYTAB_KERBEROS_LOGIN};

        private javax.security.auth.login.Configuration baseConfig;
        private final String loginContextName;
        private final boolean useTicketCache;
        private final String keytabFile;
        private final String principal;

        public JaasConfiguration(String loginContextName, String principal) {
            this(loginContextName, principal, null, true);
        }

        public JaasConfiguration(String loginContextName, String principal, String keytabFile) {
            this(loginContextName, principal, keytabFile, keytabFile == null || keytabFile.length() == 0);
        }

        private JaasConfiguration(String loginContextName, String principal,
                                  String keytabFile, boolean useTicketCache) {
            try {
                this.baseConfig = javax.security.auth.login.Configuration.getConfiguration();
            } catch (SecurityException e) {
                this.baseConfig = null;
            }
            this.loginContextName = loginContextName;
            this.useTicketCache = useTicketCache;
            this.keytabFile = keytabFile;
            this.principal = principal;
            LOG.info("JaasConfiguration loginContextName=" + loginContextName +
                    " principal=" + principal + " useTicketCache=" + useTicketCache +
                    " keytabFile=" + keytabFile);
        }

        @Override
        public AppConfigurationEntry[] getAppConfigurationEntry(String appName) {
            if (loginContextName.equals(appName)) {
                if (!useTicketCache) {
                    KEYTAB_KERBEROS_OPTIONS.put("keyTab", keytabFile);
                    KEYTAB_KERBEROS_OPTIONS.put("useKeyTab", "true");
                }
                KEYTAB_KERBEROS_OPTIONS.put("principal", principal);
                KEYTAB_KERBEROS_OPTIONS.put("useTicketCache", useTicketCache ? "true" : "false");
                return KEYTAB_KERBEROS_CONF;
            }
            if (baseConfig != null) return baseConfig.getAppConfigurationEntry(appName);
            return (null);
        }
    }

    //
    // Helper methods
    //

    /**
     * Join the prefix znode name with the suffix znode name to generate a proper
     * full znode name.
     * <p>
     * Assumes prefix does not end with slash and suffix does not begin with it.
     *
     * @param prefix beginning of znode name
     * @param suffix ending of znode name
     * @return result of properly joining prefix with suffix
     */
    public static String joinZNode(String prefix, String suffix) {
        return prefix + ZNODE_PATH_SEPARATOR + suffix;
    }

    /**
     * Returns the full path of the immediate parent of the specified node.
     *
     * @param node path to get parent of
     * @return parent of path, null if passed the root node or an invalid node
     */
    public static String getParent(String node) {
        int idx = node.lastIndexOf(ZNODE_PATH_SEPARATOR);
        return idx <= 0 ? null : node.substring(0, idx);
    }

    /**
     * Get the name of the current node from the specified fully-qualified path.
     *
     * @param path fully-qualified path
     * @return name of the current node
     */
    public static String getNodeName(String path) {
        return path.substring(path.lastIndexOf("/") + 1);
    }


    /**
     * Simple class to hold a node path and node data.
     *
     * @deprecated Unused
     */
    @Deprecated
    public static class NodeAndData {
        private String node;
        private byte[] data;

        public NodeAndData(String node, byte[] data) {
            this.node = node;
            this.data = data;
        }

        public String getNode() {
            return node;
        }

        public byte[] getData() {
            return data;
        }

        @Override
        public String toString() {
            return node;
        }

        public boolean isEmpty() {
            return (data == null || data.length == 0);
        }
    }


    /**
     * Returns whether or not secure authentication is enabled
     * (whether <code>hbase.security.authentication</code> is set to
     * <code>kerberos</code>.
     */
    public static boolean isSecureZooKeeper(Configuration conf) {
        // Detection for embedded HBase client with jaas configuration
        // defined for third party programs.
        try {
            javax.security.auth.login.Configuration testConfig =
                    javax.security.auth.login.Configuration.getConfiguration();
            if (testConfig.getAppConfigurationEntry("Client") == null
                    && testConfig.getAppConfigurationEntry(
                    JaasConfiguration.CLIENT_KEYTAB_KERBEROS_CONFIG_NAME) == null
                    && testConfig.getAppConfigurationEntry(
                    JaasConfiguration.SERVER_KEYTAB_KERBEROS_CONFIG_NAME) == null) {
                return false;
            }
        } catch (Exception e) {
            // No Jaas configuration defined.
            return false;
        }

        // Master & RSs uses hbase.zookeeper.client.*
        return "kerberos".equalsIgnoreCase(conf.get("hbase.security.authentication"));
    }


    /**
     * Represents an action taken by ZKUtil, e.g. createAndFailSilent.
     * These actions are higher-level than ZKOp actions, which represent
     * individual actions in the ZooKeeper API, like create.
     */
    public abstract static class ZKUtilOp {
        private String path;

        private ZKUtilOp(String path) {
            this.path = path;
        }

        /**
         * @return a createAndFailSilent ZKUtilOp
         */
        public static ZKUtilOp createAndFailSilent(String path, byte[] data) {
            return new CreateAndFailSilent(path, data);
        }

        /**
         * @return a deleteNodeFailSilent ZKUtilOP
         */
        public static ZKUtilOp deleteNodeFailSilent(String path) {
            return new DeleteNodeFailSilent(path);
        }

        /**
         * @return a setData ZKUtilOp
         */
        public static ZKUtilOp setData(String path, byte[] data) {
            return new SetData(path, data);
        }

        /**
         * @return path to znode where the ZKOp will occur
         */
        public String getPath() {
            return path;
        }

        /**
         * ZKUtilOp representing createAndFailSilent in ZooKeeper
         * (attempt to create node, ignore error if already exists)
         */
        public static class CreateAndFailSilent extends ZKUtilOp {
            private byte[] data;

            private CreateAndFailSilent(String path, byte[] data) {
                super(path);
                this.data = data;
            }

            public byte[] getData() {
                return data;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof CreateAndFailSilent)) return false;

                CreateAndFailSilent op = (CreateAndFailSilent) o;
                return getPath().equals(op.getPath()) && Arrays.equals(data, op.data);
            }

            @Override
            public int hashCode() {
                int ret = 17 + getPath().hashCode() * 31;
                return ret * 31 + Bytes.hashCode(data);
            }
        }

        /**
         * ZKUtilOp representing deleteNodeFailSilent in ZooKeeper
         * (attempt to delete node, ignore error if node doesn't exist)
         */
        public static class DeleteNodeFailSilent extends ZKUtilOp {
            private DeleteNodeFailSilent(String path) {
                super(path);
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof DeleteNodeFailSilent)) return false;

                return super.equals(o);
            }

            @Override
            public int hashCode() {
                return getPath().hashCode();
            }
        }

        /**
         * ZKUtilOp representing setData in ZooKeeper
         */
        public static class SetData extends ZKUtilOp {
            private byte[] data;

            private SetData(String path, byte[] data) {
                super(path);
                this.data = data;
            }

            public byte[] getData() {
                return data;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof SetData)) return false;

                SetData op = (SetData) o;
                return getPath().equals(op.getPath()) && Arrays.equals(data, op.data);
            }

            @Override
            public int hashCode() {
                int ret = getPath().hashCode();
                return ret * 31 + Bytes.hashCode(data);
            }
        }
    }


}