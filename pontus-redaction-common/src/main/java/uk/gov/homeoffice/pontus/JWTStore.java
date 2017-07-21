package uk.gov.homeoffice.pontus;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.zookeeper.*;
import org.apache.zookeeper.client.ZooKeeperSaslClient;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by leo on 16/10/2016.
 */
public class JWTStore extends Thread implements Watcher {

    public static final Log LOG = LogFactory.getLog(JWTStore.class);

    private static JWTStore instance = null;
    public static final String JWT_ZK_PATH = "jwt_store.zk.path";
    public static final String JWT_ZK_PATH_DEFVAL = "/jwt/users";
    public static final String JWT_ZK_SESSION_TIMEOUT = "jwt_store.zk.sessionTimeoutMs";
    public static final String JWT_ZK_CONNECT_STRING = "jwt_store.zk.connectString";
    public static final String JWT_ZK_CONNECT_STRING_DEFVAL = "sandbox.hortonworks.com";
    public static final String JWT_SECURITY_CLAIM_CACHE_INIT_SIZE = "jwt_store.security_claim_cache.initSizeBytes";
    public static final String JWT_SECURITY_CLAIM_CACHE_LOAD_FACTOR = "jwt_store.security_claim_cache.loadFactor";
    public static final String JWT_SECURITY_CLAIM_CACHE_MAX_SIZE = "jwt_store.security_claim_cache.maxSizeBytes";

    public static final int JWT_SECURITY_CLAIM_CACHE_INIT_SIZE_DEFVAL = 20000;
    public static final float JWT_SECURITY_CLAIM_CACHE_LOAD_FACTOR_DEFVAL = 0.75f;
    public static final long JWT_SECURITY_CLAIM_CACHE_MAX_SIZE_DEFVAL = 50000000;

    protected LruHashMap<JWTUser, JWTClaim> securityClaimCache;
    Configuration conf = null;
    protected String connectString;
    protected ZooKeeper zk;
    protected int sessionTimeout = 120000;
    protected String zkPath;

    public static JWTStore create(Configuration conf) throws IOException, KeeperException {
        if (instance == null) {
            synchronized (JWTStore.class) {
                instance = new JWTStore(conf);
            }
        }
        return instance;
    }

    public String getZkPath() {
        return zkPath;
    }

    protected JWTStore(Configuration conf) throws IOException, KeeperException {
        super("JWTStore-Thread");
        this.conf = conf;

        sessionTimeout = conf.getInt(JWT_ZK_SESSION_TIMEOUT, sessionTimeout);
        connectString = conf.get(JWT_ZK_CONNECT_STRING, JWT_ZK_CONNECT_STRING_DEFVAL);
        zkPath = conf.get(JWT_ZK_PATH, JWT_ZK_PATH_DEFVAL);
        int initSize = conf.getInt(JWT_SECURITY_CLAIM_CACHE_INIT_SIZE, JWT_SECURITY_CLAIM_CACHE_INIT_SIZE_DEFVAL);
        float loadFactor = conf.getFloat(JWT_SECURITY_CLAIM_CACHE_LOAD_FACTOR, JWT_SECURITY_CLAIM_CACHE_LOAD_FACTOR_DEFVAL);
        long maxSize = conf.getLong(JWT_SECURITY_CLAIM_CACHE_MAX_SIZE, JWT_SECURITY_CLAIM_CACHE_MAX_SIZE_DEFVAL);

        securityClaimCache = new LruHashMap<>(initSize, loadFactor, maxSize);

        ZooKeeperSaslClient cli = null;
        Watcher watcher = this;
        boolean canBeReadOnly = true;
        ZKUtil.RecoverableZooKeeper rzk = ZKUtil.connect(conf,this);

//        UserGroupInformation.setConfiguration(conf);
        zk = rzk.checkZk();// new ZooKeeper(connectString, sessionTimeout, watcher, canBeReadOnly);
        try {

            Stat stat = zk.exists(zkPath, this);
            List<ACL> perms = new ArrayList<>();
            // LPPM - always assume we have kerberos enabled.
//            if (UserGroupInformation.getCurrentUser().hasKerberosCredentials()) {
                perms.add(new ACL(ZooDefs.Perms.ALL, ZooDefs.Ids.AUTH_IDS));
                perms.add(new ACL(ZooDefs.Perms.READ,ZooDefs.Ids.ANYONE_ID_UNSAFE));
//            } else {
//                perms.add(new ACL(ZooDefs.Perms.ALL, ZooDefs.Ids.ANYONE_ID_UNSAFE));
//            }
            if (stat == null) {
                zk.create(zkPath, null, perms, CreateMode.PERSISTENT);
            }

        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

//    public JWTClaim getUserClaim(User user) throws KeeperException, InterruptedException {
//        JWTUser jwtUser = new JWTUser(user);
//        return getUserClaim(jwtUser);
//    }

    public JWTClaim getUserClaim(JWTUser jwtUser) throws KeeperException, InterruptedException, IOException {

        JWTClaim retVal = securityClaimCache.get(jwtUser);

        if (retVal == null) {

            ZooKeeper.States zkState = zk.getState();
            if ( !( zkState == ZooKeeper.States.CONNECTED ||
                    zkState == ZooKeeper.States.CONNECTEDREADONLY )){
                Watcher watcher = this;
                boolean canBeReadOnly = true;
                zk.close();
                ZKUtil.RecoverableZooKeeper rzk = ZKUtil.connect(conf,this);
                zk = rzk.checkZk();// new ZooKeeper(connectString, sessionTimeout, watcher, canBeReadOnly);
            }

            StringBuffer strBuf = new StringBuffer(zkPath).append("/").append(jwtUser.shortName);
            byte[] data = zk.getData(strBuf.toString(), true, null);
            if (data == null) {
                return null;
            }
            retVal = JWTClaim.fromBinary(data);
            if (retVal == null) {
                return null;
            }

            securityClaimCache.put(jwtUser, retVal);
        }

        return retVal;
    }

    @Override
    public void process(WatchedEvent event) {

        Event.EventType type = event.getType();

        if (type.equals(Event.EventType.NodeDeleted) || type.equals(Event.EventType.NodeDataChanged)) {
            JWTUser user = new JWTUser(event.getPath(),this);
            securityClaimCache.remove(user);
            LOG.info("Removed user from JWT cache:" + user);
        }


    }
}
