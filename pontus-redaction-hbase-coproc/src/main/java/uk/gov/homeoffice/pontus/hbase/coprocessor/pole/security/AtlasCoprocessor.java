package uk.gov.homeoffice.pontus.hbase.coprocessor.pole.security;

import com.google.common.net.HostAndPort;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.coprocessor.MasterCoprocessorEnvironment;
import org.apache.hadoop.hbase.coprocessor.MasterObserver;
import org.apache.hadoop.hbase.coprocessor.ObserverContext;
import org.apache.hadoop.hbase.master.RegionPlan;
import org.apache.hadoop.hbase.master.procedure.MasterProcedureEnv;
import org.apache.hadoop.hbase.procedure2.ProcedureExecutor;
import org.apache.hadoop.hbase.protobuf.generated.HBaseProtos.SnapshotDescription;
import org.apache.hadoop.hbase.protobuf.generated.QuotaProtos.Quotas;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Hello world!
 *
 */
public class AtlasCoprocessor implements MasterObserver
{
//  Producer<String, String> producer;
  
  String atlasUpdateTopic = "ATLAS_HOOK";
  
  String namespaceChangeMsgFormat = "{\n" + 
    "\"version\": {\n" + 
    "\"version\": \"1.0.0\"\n" + 
    "},\n" + 
    "\"message\": {\n" + 
    "\"entities\": [{\n" + 
    "\"jsonClass\":\n" + 
    "\"org.apache.atlas.typesystem.json.InstanceSerialization$_Reference\",\n" + 
    "\"id\": {\n" + 
    "\"jsonClass\":\n" + 
    "\"org.apache.atlas.typesystem.json.InstanceSerialization$_Id\",\n" + 
    "\"id\": \"­1467290565135246000\",\n" + 
    "\"version\": 0,\n" + 
    "\"typeName\": \"hbase_namespace\",\n" + 
    "\"state\": \"ACTIVE\"\n" + 
    "},\n" + 
    "\"typeName\": \"hbase_namespace\",\n" + 
    "\"values\": {\n" + 
    "\"qualifiedName\": \"default@cluster3\",\n" + 
    "\"owner\": \"hbase_admin\",\n" + 
    "\"description\": \"Default HBase namespace\",\n" + 
    "\"name\": \"default\"\n" + 
    "},\n" + 
    "\"traitNames\": [],\n" + 
    "\"traits\": {}\n" + 
    "}],\n" + 
    "\"type\": \"ENTITY_CREATE\",\n" + 
    "\"user\": \"%s\"\n" + 
    "}\n" + 
    "}";

  public static void main(String[] args)
  {
    System.out.println("Hello World!");
  }

  public AtlasCoprocessor()
  {
  }

  @Override
  public void start(CoprocessorEnvironment env) throws IOException
  {
//    Properties props = new Properties();
//    props.put("bootstrap.servers", "localhost:9092");
//    props.put("acks", "all");
//    props.put("retries", 0);
//    props.put("batch.size", 16384);
//    props.put("linger.ms", 1);
//    props.put("buffer.memory", 33554432);
//    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
//    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
//
//    producer = new KafkaProducer<>(props);
//
  }

  @Override
  public void stop(CoprocessorEnvironment env) throws IOException
  {
//    producer.close();

  }

  @Override
  public void preCreateTable(ObserverContext<MasterCoprocessorEnvironment> ctx, HTableDescriptor desc,
                             HRegionInfo[] regions) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void postCreateTable(ObserverContext<MasterCoprocessorEnvironment> ctx, HTableDescriptor desc,
                              HRegionInfo[] regions) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void preCreateTableHandler(ObserverContext<MasterCoprocessorEnvironment> ctx, HTableDescriptor desc,
                                    HRegionInfo[] regions) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void postCreateTableHandler(ObserverContext<MasterCoprocessorEnvironment> ctx, HTableDescriptor desc,
                                     HRegionInfo[] regions) throws IOException
  {


  }

  @Override
  public void preDeleteTable(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void postDeleteTable(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName)
    throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void preDeleteTableHandler(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName)
    throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void postDeleteTableHandler(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName)
    throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void preTruncateTable(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName)
    throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void postTruncateTable(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName)
    throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void preTruncateTableHandler(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName)
    throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void postTruncateTableHandler(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName)
    throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void preModifyTable(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName,
                             HTableDescriptor htd) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void postModifyTable(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName,
                              HTableDescriptor htd) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void preModifyTableHandler(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName,
                                    HTableDescriptor htd) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void postModifyTableHandler(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName,
                                     HTableDescriptor htd) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void preAddColumn(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName,
                           HColumnDescriptor column) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void postAddColumn(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName,
                            HColumnDescriptor column) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void preAddColumnHandler(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName,
                                  HColumnDescriptor column) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void postAddColumnHandler(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName,
                                   HColumnDescriptor column) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void preModifyColumn(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName,
                              HColumnDescriptor descriptor) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void postModifyColumn(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName,
                               HColumnDescriptor descriptor) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void preModifyColumnHandler(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName,
                                     HColumnDescriptor descriptor) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void postModifyColumnHandler(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName,
                                      HColumnDescriptor descriptor) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void preDeleteColumn(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName, byte[] c)
    throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void postDeleteColumn(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName, byte[] c)
    throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void preDeleteColumnHandler(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName, byte[] c)
    throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void postDeleteColumnHandler(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName, byte[] c)
    throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void preEnableTable(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void postEnableTable(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName)
    throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void preEnableTableHandler(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName)
    throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void postEnableTableHandler(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName)
    throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void preDisableTable(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName)
    throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void postDisableTable(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName)
    throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void preDisableTableHandler(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName)
    throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void postDisableTableHandler(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName)
    throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void preMove(ObserverContext<MasterCoprocessorEnvironment> ctx, HRegionInfo region, ServerName srcServer,
                      ServerName destServer) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void postMove(ObserverContext<MasterCoprocessorEnvironment> ctx, HRegionInfo region, ServerName srcServer,
                       ServerName destServer) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void preAbortProcedure(ObserverContext<MasterCoprocessorEnvironment> ctx,
                                ProcedureExecutor<MasterProcedureEnv> procEnv, long procId) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void postAbortProcedure(ObserverContext<MasterCoprocessorEnvironment> ctx) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void preListProcedures(ObserverContext<MasterCoprocessorEnvironment> ctx) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void postListProcedures(ObserverContext<MasterCoprocessorEnvironment> ctx, List<ProcedureInfo> procInfoList)
    throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void preAssign(ObserverContext<MasterCoprocessorEnvironment> ctx, HRegionInfo regionInfo) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void postAssign(ObserverContext<MasterCoprocessorEnvironment> ctx, HRegionInfo regionInfo) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void preUnassign(ObserverContext<MasterCoprocessorEnvironment> ctx, HRegionInfo regionInfo, boolean force)
    throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void postUnassign(ObserverContext<MasterCoprocessorEnvironment> ctx, HRegionInfo regionInfo, boolean force)
    throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void preRegionOffline(ObserverContext<MasterCoprocessorEnvironment> ctx, HRegionInfo regionInfo)
    throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void postRegionOffline(ObserverContext<MasterCoprocessorEnvironment> ctx, HRegionInfo regionInfo)
    throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void preBalance(ObserverContext<MasterCoprocessorEnvironment> ctx) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void postBalance(ObserverContext<MasterCoprocessorEnvironment> ctx, List<RegionPlan> plans) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public boolean preBalanceSwitch(ObserverContext<MasterCoprocessorEnvironment> ctx, boolean newValue)
    throws IOException
  {
    // TODO Auto-generated method stub
    return true;
  }

  @Override
  public void postBalanceSwitch(ObserverContext<MasterCoprocessorEnvironment> ctx, boolean oldValue, boolean newValue)
    throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void preShutdown(ObserverContext<MasterCoprocessorEnvironment> ctx) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void preStopMaster(ObserverContext<MasterCoprocessorEnvironment> ctx) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void postStartMaster(ObserverContext<MasterCoprocessorEnvironment> ctx) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void preMasterInitialization(ObserverContext<MasterCoprocessorEnvironment> ctx) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void preSnapshot(ObserverContext<MasterCoprocessorEnvironment> ctx, SnapshotDescription snapshot,
                          HTableDescriptor hTableDescriptor) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void postSnapshot(ObserverContext<MasterCoprocessorEnvironment> ctx, SnapshotDescription snapshot,
                           HTableDescriptor hTableDescriptor) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void preListSnapshot(ObserverContext<MasterCoprocessorEnvironment> ctx, SnapshotDescription snapshot)
    throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void postListSnapshot(ObserverContext<MasterCoprocessorEnvironment> ctx, SnapshotDescription snapshot)
    throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void preCloneSnapshot(ObserverContext<MasterCoprocessorEnvironment> ctx, SnapshotDescription snapshot,
                               HTableDescriptor hTableDescriptor) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void postCloneSnapshot(ObserverContext<MasterCoprocessorEnvironment> ctx, SnapshotDescription snapshot,
                                HTableDescriptor hTableDescriptor) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void preRestoreSnapshot(ObserverContext<MasterCoprocessorEnvironment> ctx, SnapshotDescription snapshot,
                                 HTableDescriptor hTableDescriptor) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void postRestoreSnapshot(ObserverContext<MasterCoprocessorEnvironment> ctx, SnapshotDescription snapshot,
                                  HTableDescriptor hTableDescriptor) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void preDeleteSnapshot(ObserverContext<MasterCoprocessorEnvironment> ctx, SnapshotDescription snapshot)
    throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void postDeleteSnapshot(ObserverContext<MasterCoprocessorEnvironment> ctx, SnapshotDescription snapshot)
    throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void preGetTableDescriptors(ObserverContext<MasterCoprocessorEnvironment> ctx, List<TableName> tableNamesList,
                                     List<HTableDescriptor> descriptors) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void postGetTableDescriptors(ObserverContext<MasterCoprocessorEnvironment> ctx,
                                      List<HTableDescriptor> descriptors) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void preGetTableDescriptors(ObserverContext<MasterCoprocessorEnvironment> ctx, List<TableName> tableNamesList,
                                     List<HTableDescriptor> descriptors, String regex) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void postGetTableDescriptors(ObserverContext<MasterCoprocessorEnvironment> ctx,
                                      List<TableName> tableNamesList, List<HTableDescriptor> descriptors, String regex)
    throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void preGetTableNames(ObserverContext<MasterCoprocessorEnvironment> ctx, List<HTableDescriptor> descriptors,
                               String regex) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void postGetTableNames(ObserverContext<MasterCoprocessorEnvironment> ctx, List<HTableDescriptor> descriptors,
                                String regex) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void preCreateNamespace(ObserverContext<MasterCoprocessorEnvironment> ctx, NamespaceDescriptor ns)
    throws IOException
  {
    
//    producer.send(new ProducerRecord<String, String>("ATLAS_HOOK", "ENTITY_PARTIAL_UPDATE", namespaceChangeMsgFormat   ));

    
  }

  @Override
  public void postCreateNamespace(ObserverContext<MasterCoprocessorEnvironment> ctx, NamespaceDescriptor ns)
    throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void preDeleteNamespace(ObserverContext<MasterCoprocessorEnvironment> ctx, String namespace)
    throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void postDeleteNamespace(ObserverContext<MasterCoprocessorEnvironment> ctx, String namespace)
    throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void preModifyNamespace(ObserverContext<MasterCoprocessorEnvironment> ctx, NamespaceDescriptor ns)
    throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void postModifyNamespace(ObserverContext<MasterCoprocessorEnvironment> ctx, NamespaceDescriptor ns)
    throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void preGetNamespaceDescriptor(ObserverContext<MasterCoprocessorEnvironment> ctx, String namespace)
    throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void postGetNamespaceDescriptor(ObserverContext<MasterCoprocessorEnvironment> ctx, NamespaceDescriptor ns)
    throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void preListNamespaceDescriptors(ObserverContext<MasterCoprocessorEnvironment> ctx,
                                          List<NamespaceDescriptor> descriptors) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void postListNamespaceDescriptors(ObserverContext<MasterCoprocessorEnvironment> ctx,
                                           List<NamespaceDescriptor> descriptors) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void preTableFlush(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void postTableFlush(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void preSetUserQuota(ObserverContext<MasterCoprocessorEnvironment> ctx, String userName, Quotas quotas)
    throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void postSetUserQuota(ObserverContext<MasterCoprocessorEnvironment> ctx, String userName, Quotas quotas)
    throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void preSetUserQuota(ObserverContext<MasterCoprocessorEnvironment> ctx, String userName, TableName tableName,
                              Quotas quotas) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void postSetUserQuota(ObserverContext<MasterCoprocessorEnvironment> ctx, String userName, TableName tableName,
                               Quotas quotas) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void preSetUserQuota(ObserverContext<MasterCoprocessorEnvironment> ctx, String userName, String namespace,
                              Quotas quotas) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void postSetUserQuota(ObserverContext<MasterCoprocessorEnvironment> ctx, String userName, String namespace,
                               Quotas quotas) throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void preSetTableQuota(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName, Quotas quotas)
    throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void postSetTableQuota(ObserverContext<MasterCoprocessorEnvironment> ctx, TableName tableName, Quotas quotas)
    throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void preSetNamespaceQuota(ObserverContext<MasterCoprocessorEnvironment> ctx, String namespace, Quotas quotas)
    throws IOException
  {
    // TODO Auto-generated method stub

  }

  @Override
  public void postSetNamespaceQuota(ObserverContext<MasterCoprocessorEnvironment> ctx, String namespace, Quotas quotas)
    throws IOException
  {
    // TODO Auto-generated method stub

  }

  public void postAddRSGroup(ObserverContext<MasterCoprocessorEnvironment> arg0, String arg1) throws IOException
  {
    // TODO Auto-generated method stub
    
  }

  public void postBalanceRSGroup(ObserverContext<MasterCoprocessorEnvironment> arg0, String arg1, boolean arg2)
    throws IOException
  {
    // TODO Auto-generated method stub
    
  }

  public void postMoveServers(ObserverContext<MasterCoprocessorEnvironment> arg0, Set<HostAndPort> arg1, String arg2)
    throws IOException
  {
    // TODO Auto-generated method stub
    
  }

  public void postMoveTables(ObserverContext<MasterCoprocessorEnvironment> arg0, Set<TableName> arg1, String arg2)
    throws IOException
  {
    // TODO Auto-generated method stub
    
  }

  public void postRemoveRSGroup(ObserverContext<MasterCoprocessorEnvironment> arg0, String arg1) throws IOException
  {
    // TODO Auto-generated method stub
    
  }

  public void preAddRSGroup(ObserverContext<MasterCoprocessorEnvironment> arg0, String arg1) throws IOException
  {
    // TODO Auto-generated method stub
    
  }

  public void preBalanceRSGroup(ObserverContext<MasterCoprocessorEnvironment> arg0, String arg1) throws IOException
  {
    // TODO Auto-generated method stub
    
  }

  public void preMoveServers(ObserverContext<MasterCoprocessorEnvironment> arg0, Set<HostAndPort> arg1, String arg2)
    throws IOException
  {
    // TODO Auto-generated method stub
    
  }

  public void preMoveTables(ObserverContext<MasterCoprocessorEnvironment> arg0, Set<TableName> arg1, String arg2)
    throws IOException
  {
    // TODO Auto-generated method stub
    
  }

  public void preRemoveRSGroup(ObserverContext<MasterCoprocessorEnvironment> arg0, String arg1) throws IOException
  {
    
  }
}
