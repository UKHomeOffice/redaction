import org.apache.hadoop.security.UserGroupInformation;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;
import uk.gov.homeoffice.pontus.JWTClaim;
import uk.gov.homeoffice.pontus.JWTStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Created by leo on 17/10/2016.
 */
public class JWTStoreClient {
    // declare zookeeper instance to access ZooKeeper ensemble
    private ZooKeeper zoo;
    final CountDownLatch connectedSignal = new CountDownLatch(1);

    // Method to connect zookeeper ensemble.
    public ZooKeeper connect(String host) throws IOException, InterruptedException {

        zoo = new ZooKeeper(host, 5000, new Watcher() {

            public void process(WatchedEvent we) {

                if (we.getState() == Event.KeeperState.SyncConnected) {
                    connectedSignal.countDown();
                }
            }
        });

        connectedSignal.await();
        return zoo;
    }

    public void create(String path, byte[] data) throws
            KeeperException, InterruptedException {

        String[] parts = path.split("/");
        StringBuffer strBuf = new StringBuffer();
        // LPPM - the first level is empty, as we start with a slash.
        // skip it by setting i and j = 1.
        for (int i = 1, ilen = parts.length, lastItem = parts.length -1; i < ilen; i++){
            strBuf.setLength(0);
            for (int j = 1; j <= i; j++){
                strBuf.append("/").append(parts[j]);
            }
            String partialPath = strBuf.toString();
            if (exists(partialPath) == null){
                CreateMode mode;
                if (i == lastItem){
                    mode = CreateMode.PERSISTENT;
                }
                else{
                    mode = CreateMode.PERSISTENT;
                }
                List<ACL> perms = new ArrayList<>();
                if (UserGroupInformation.isSecurityEnabled()) {
                    perms.add(new ACL(ZooDefs.Perms.ALL, ZooDefs.Ids.AUTH_IDS));
                    perms.add(new ACL(ZooDefs.Perms.READ,ZooDefs.Ids.ANYONE_ID_UNSAFE));
                } else {
                    perms.add(new ACL(ZooDefs.Perms.ALL, ZooDefs.Ids.ANYONE_ID_UNSAFE));
                }

                zoo.create(partialPath, data,perms,
                        mode);

            }
        }
//        zoo.create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE,
//                CreateMode.EPHEMERAL);
    }

    public Stat exists(String path) throws
            KeeperException, InterruptedException {
        return zoo.exists(path, true);
    }

    public void update(String path, byte[] data) throws
            KeeperException, InterruptedException {
        zoo.setData(path, data, zoo.exists(path, true).getVersion());
    }


    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {

        if (args.length != 2) {
            System.err.printf("This utility sets the JWT json for a user\n\nUsage: java ... <json with JWT>\n\n");
            System.err.printf("Example:\n\n java ... hbase '{ \"sub\":\"hbase\", \"bizctx\":\"/uk.police/investigator\"}'\n\n\n");

            System.exit(-1);
        }


        JWTClaim sampleClaim = JWTClaim.fromJson(args[1]);

        StringBuffer strBuf = new StringBuffer(JWTStore.JWT_ZK_PATH_DEFVAL).append("/").append(sampleClaim.getSub());

        JWTStoreClient cli = new JWTStoreClient();

        cli.connect(System.getProperty("pontus.redaction.zk","localhost"));

        if (cli.exists(strBuf.toString()) == null) {
            cli.create(strBuf.toString(), args[0].getBytes());
        }
        else{
            cli.update(strBuf.toString(), args[0].getBytes());
        }

        System.out.println("Successfully updated user " + args[0]);

        cli.close();


    }


    // Method to disconnect from zookeeper server
    public void close() throws InterruptedException {
        zoo.close();
    }

}
