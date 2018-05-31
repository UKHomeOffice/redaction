package uk.gov.homeoffice.pontus.hbase.coprocessor.pole.security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.coprocessor.*;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.ipc.RpcServer;
import org.apache.hadoop.hbase.protobuf.generated.AdminProtos;
import org.apache.hadoop.hbase.regionserver.Region;
import org.apache.hadoop.hbase.regionserver.RegionScanner;
import org.apache.hadoop.hbase.regionserver.wal.HLogKey;
import org.apache.hadoop.hbase.regionserver.wal.WALEdit;
import org.apache.hadoop.hbase.replication.ReplicationEndpoint;
import org.apache.hadoop.hbase.security.User;
import org.apache.hadoop.hbase.security.UserProvider;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.hbase.wal.WALKey;
import org.apache.zookeeper.KeeperException;
import uk.gov.homeoffice.pontus.*;
import uk.gov.homeoffice.pontus.kafka.KafkaConnectorSingleton;
import uk.gov.homeoffice.pontus.solr.SolrConnectorSingleton;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class PoleSecurityCoprocessor extends BaseMasterAndRegionObserver
        implements RegionServerObserver, WALObserver {

    //    static final byte[] metaDataFamily = Bytes.toBytes("mdf");
    static final byte[] metaDataQualifier = Bytes.toBytes("rmdq");

    public static final Log LOG = LogFactory.getLog(PoleSecurityCoprocessor.class);

    protected PolicyStore policyStore = null;
    protected JWTStore jwtStore = null;
    protected UserProvider userProvider;
    //    protected ElasticConnectorSingleton elastic;
    protected SolrConnectorSingleton solr;
    protected KafkaConnectorSingleton kafka;


    public FilterData getUserPatterns(User user) throws IOException, KeeperException, InterruptedException {

        JWTClaim claim = jwtStore.getUserClaim(new JWTUser(user.getName()));

        if (claim == null) {
            LOG.error("Could not find claim for user " + user.toString());

            return null;
        }

        if (!claim.isValid()) {
            LOG.error("Invalid Claim received for user " + user.toString());

            return null;
        }

        FilterData retVal = policyStore.getFilterData(claim.getBizctx());  // new FilterData(".*/uk.gov.police.*", ".*", "denied", "ssshhhh");

        StringBuffer strBuf = new StringBuffer("Got filter Data for user ")
                .append(user.getShortName())
                .append(":")
                .append(retVal == null? "NULL" : retVal.toString());
        LOG.info (strBuf.toString());
        return retVal;
    }

    public static boolean filter(User user, TableName table, List<Cell> result, FilterData filterData, boolean redact) throws IOException {
//        ArrayList<Cell> redactionList = new ArrayList<>(result.size());

        String metadataStr = null;

        boolean keepAll = true;
        boolean foundMetaData = false;

        Iterator<Cell> it = result.iterator();

//        for (int i = 0, ilen = result.size(); i < ilen; i++) {
//            Cell cell = result.get(i);

        int offset, len;
        while (it.hasNext()) {
            Cell cell = it.next();
//            byte[] tags = cell.getTagsArray();
//            offset = cell.getTagsOffset();
//
//            int tagsLength = cell.getTagsLength();
//            // org.apache.hadoop.hbase.codec.KeyValueCodecWithTags
////            int tagsLength = ((KeyValue) cell).getLength() - (((KeyValue) cell).getKeyLength() + cell.getValueLength() + 8);
////            if(tagsLength > 0) {
////                tagsLength -= 2;
////            }
//
//            List<Tag> tagsList = Tag.asList(cell.getTagsArray(), offset, tagsLength);
//
//            for (int j = 0, jlen = tagsList.size(); j < jlen; j++) {
//                Tag tag = tagsList.get(j);
//
//                try {
//
//                    metadataStr = Bytes.toString(tag.getBuffer(), tag.getTagOffset(), tag.getTagLength());
//                } catch (Throwable e) {
//
//                    metadataStr = null;
//                }
//
//            }


            try {
//                metadataStr = Bytes.toString(tags, offset, tagsLength);


                byte[] rowData = cell.getRowArray();

//            offset = cell.getFamilyOffset();
//            byte[] currFamily = Arrays.copyOfRange(rowData, offset, offset + cell.getFamilyLength());
//            if (Arrays.equals(currFamily, metaDataFamily)) {
                offset = cell.getQualifierOffset();
                byte[] currQual = Arrays.copyOfRange(rowData, offset, offset + cell.getQualifierLength());

                if (Arrays.equals(currQual, metaDataQualifier)) {
                    metadataStr = Bytes.toString(cell.getValueArray(), cell.getValueOffset(), cell.getValueLength());

                    if (metadataStr != null && metadataStr.length() > 0) {
                        foundMetaData = true;
                        if (!filterData.metaDataMatchesJre(metadataStr)) {
                            // Don't throw an exception, as that will give the users clues that they're not allowed to see
                            // the data; instead, mark all records to be cleaned, and log this (in ranger?)
                            LOG.info("Failed to find metadata in query for table '"
                                    + table.getNameAsString() + "'; user '"
                                    + (user != null ? user.getShortName() : "null")
                                    + "'; rowid = " + Bytes.toString(cell.getRowArray(), cell.getRowOffset(), cell.getRowLength()));

                            keepAll = false;
//                        throw new AccessDeniedException("Failed to find metadata in query for table '"
//                                + table.getNameAsString() + "'; user '"
//                                + (user != null ? user.getShortName() : "null")
//                                + "'");
                            break;
                        }
                        it.remove();
                        continue;

                    }
//                    it.remove();
////                    redactionList.add(cell);
//                }
//            } else {


                }
                String val = Bytes.toString(cell.getValueArray(), cell.getValueOffset(), cell.getValueLength());
                if (filterData.needRedactionJre(val)) {
                    it.remove();
//
//                        redactionList.add(cell);
                }


            } catch (Throwable e) {
                String err = e.toString();
                e.printStackTrace();
                LOG.error("Got an exception during filtering process: ", e);
            }
//            }

        }

        if (!keepAll || !foundMetaData) {
            result.clear();
        }

//        if (keepAll) {
//            if (redactionList.size() > 0) {
//                if (redact) {
//                    for (int i = 0, ilen = redactionList.size(); i < ilen; i++) {
//                        Cell cell = redactionList.get(i);
//                        byte[] vals = cell.getValueArray();
//                        byte zero = 0;
//                        int offset = cell.getValueOffset();
//                        Arrays.fill(vals, offset, offset + cell.getValueLength(), zero);
//
//                        offset = cell.getQualifierOffset();
//                        Arrays.fill(cell.getQualifierArray(), offset, offset + cell.getQualifierLength(), zero);
//
//                        offset = cell.getFamilyOffset();
//                        Arrays.fill(cell.getFamilyArray(), offset, offset + cell.getFamilyLength(), zero);
//
//                        if (cell instanceof KeyValue) {
//                            KeyValue kv = (KeyValue) cell;
//                            kv.setTimestamp(0L);
//                            kv.getOffset();
//                        }
//                    }
//                } else {
//                    result.removeAll(redactionList);
//                }
//            }
//        } else {
//            if (redact) {
//                for (int i = 0, ilen = result.size(); i < ilen; i++) {
//                    Cell cell = result.get(i);
//                    byte[] vals = cell.getValueArray();
//                    byte zero = 0;
//                    int offset = cell.getValueOffset();
//                    Arrays.fill(vals, offset, offset + cell.getValueLength(), zero);
//                }
//            } else {
//                result.clear();
//
//            }
//
//        }

        return keepAll;
    }

    public static boolean filterRedaction(User user, TableName table, List<Cell> result, FilterData filterData) throws IOException {
//        ArrayList<Cell> redactionList = new ArrayList<>(result.size());



        Iterator<Cell> it = result.iterator();

        boolean retVal = true;

        while (it.hasNext()) {
            Cell cell = it.next();
            try {
                String qualifier = Bytes.toString(cell.getQualifierArray(), cell.getQualifierOffset(), cell.getQualifierLength());
//                String val = Bytes.toString(cell.getValueArray(), cell.getValueOffset(), cell.getValueLength());
                if (filterData.needRedactionJre(qualifier)) {
                    it.remove();
                }


            } catch (Throwable e) {
//                String err = e.toString();
//                e.printStackTrace();
                LOG.error("Got an exception during filtering process: ", e);
                retVal = false;
            }

        }

//        if (!keepAll) {
//            result.clear();
//        }


        return retVal;
    }


    public static boolean filterMetatata(User user, TableName table, List<Cell> result, FilterData filterData) throws IOException {
        String metadataStr = null;

        boolean keepAll = true;
        boolean foundMetaData = false;

        Iterator<Cell> it = result.iterator();
        int offset, len;
        while (it.hasNext()) {
            Cell cell = it.next();
            try {
                byte[] rowData = cell.getRowArray();
                offset = cell.getQualifierOffset();
                byte[] currQual = Arrays.copyOfRange(rowData, offset, offset + cell.getQualifierLength());

                if (Arrays.equals(currQual, metaDataQualifier)) {
                    metadataStr = Bytes.toString(cell.getValueArray(), cell.getValueOffset(), cell.getValueLength());

                    if (metadataStr != null && metadataStr.length() > 0) {
                        foundMetaData = true;
                        if (!filterData.metaDataMatchesJre(metadataStr)) {
                            // Don't throw an exception, as that will give the users clues that they're not allowed to see
                            // the data; instead, mark all records to be cleaned, and log this (in ranger?)

                            // LPPM 31 Jan 2017 - this is not supported at the moment....
//                            LOG.info("Failed to find metadata in query for table '"
//                                    + table.getNameAsString() + "'; user '"
//                                    + (user != null ? user.getShortName() : "null")
//                                    + "'; rowid = " + Bytes.toString(cell.getRowArray(), cell.getRowOffset(), cell.getRowLength()));

                            keepAll = false;
//                        throw new AccessDeniedException("Failed to find metadata in query for table '"
//                                + table.getNameAsString() + "'; user '"
//                                + (user != null ? user.getShortName() : "null")
//                                + "'");
                            break;
                        }
                        it.remove();
                        continue;

                    }
                }

            } catch (Throwable e) {
                LOG.error("Got an exception during filtering process: ", e);
                keepAll = false;
            }

        }

        if (!keepAll || !foundMetaData) {
            result.clear();
        }

        return keepAll;
    }

    @Override
    public void start(CoprocessorEnvironment env) throws IOException {
        super.start(env);

        this.userProvider = UserProvider.instantiate(env.getConfiguration());

        CompoundConfiguration conf = new CompoundConfiguration();
        conf.add(env.getConfiguration());

        Configuration config = new Configuration(false);
        config.addResource(new Path("/usr/hdp/current/hadoop-client/conf/core-site.xml"));
        config.addResource(new Path("/usr/hdp/current/hadoop-client/conf/hdfs-site.xml"));
        config.addResource(new Path("/opt/pontus/pontus-hbase/current/conf/core-site.xml"));
        config.addResource(new Path("/opt/pontus/pontus-hbase/current/conf/hdfs-site.xml"));

        conf.add(config);


        try {
            policyStore = PolicyStore.createAndStart(conf);
        } catch (Throwable e) {
            LOG.error("Failed to create policy store:", e);
        }

        try {
            jwtStore = JWTStore.create(conf);
        } catch (KeeperException e) {
            LOG.error("Failed to create JWT store:", e);
            throw new IOException(e);
        }

//        solr = SolrConnectorSingleton.create(conf);
//        elastic = ElasticConnectorSingleton.createElasticConnectorSingleton(conf);
        kafka = KafkaConnectorSingleton.create(conf);

    }

    @Override
    public void stop(CoprocessorEnvironment env) {

        PolicyStore.destroyAndStop();

    }

    @Override
    public void postGetOp(ObserverContext<RegionCoprocessorEnvironment> env, Get get, List<Cell> result)
            throws IOException {

        Region reg = (env.getEnvironment().getRegion());
        TableName table = getTableName(reg);


        if (!table.isSystemTable()) {

            User user = getActiveUser();
            FilterData filterData = null;
            try {
                filterData = getUserPatterns(user);
                if (filterData == null) {
                    throw new IOException("Failed to get credentials");
                }
                filter(user, table, result, filterData, false);
            } catch (Exception e) {
                e.printStackTrace();
                LOG.error("Found error when filtering data: ", e);
                throw new IOException(e.getMessage());

            }
        }

    }

    protected User getActiveUser() throws IOException {
        User user = RpcServer.getRequestUser();
        if (user == null) {
            // for non-rpc handling, fallback to system user
            user = userProvider.getCurrent();
        }
        return user;
    }

    protected TableName getTableName(Region region) {
        HRegionInfo regionInfo = region.getRegionInfo();
        if (regionInfo != null) {
            return regionInfo.getTable();
        }
        return null;
    }

/*
    @Override
    public void postPut(final ObserverContext<RegionCoprocessorEnvironment> e,
                        final Put put, final WALEdit edit, final Durability durability) throws IOException {

        String metadataStr = null;

        TableName tableName = getTableName(e.getEnvironment().getRegion());


//        byte[] row  = put.getRow();

        NavigableMap<byte[], List<Cell>> familyCellMap = put.getFamilyCellMap();


        String id = put.getId();
        ArrayList<Cell> cells = edit.getCells();

        StringBuffer colStrBuf = new StringBuffer();
        String tableNameStr = null;
        String rowStr = null;
        String colStr = null;
        String valStr = null;
        try {
            for (int i = 0, ilen = cells.size(); i < ilen; i++) {
                Cell cell = cells.get(i);

                rowStr = Bytes.toStringBinary(cell.getRowArray(), cell.getRowOffset(), cell.getRowLength());
                byte[] family = Bytes.copy(cell.getFamilyArray(), cell.getFamilyOffset(), cell.getFamilyLength());
                String familyStr = Bytes.toString(family);

                String qualifierStr = Bytes.toString(cell.getQualifierArray(), cell.getQualifierOffset(), cell.getQualifierLength());

                valStr = Bytes.toString(cell.getValueArray(), cell.getValueOffset(), cell.getValueLength());

                colStrBuf.setLength(0);
                colStrBuf.append(familyStr).append(':').append(qualifierStr);
                colStr = colStrBuf.toString();
                tableNameStr = tableName.getNameAsString();
//                elastic.indexCell(tableNameStr, rowStr, colStr, valStr);
//                solr.indexCell(tableNameStr, rowStr, colStr, valStr);
//                kafka.indexCell(tableNameStr, rowStr, colStr, valStr);

            }
        } catch (InsufficientCapacityException e1) {
            LOG.error(String.format("Failed to index Table %s Row %s Col %s Val=%s",
                    tableNameStr, rowStr, colStr, valStr));
        }

//        cells.get(0).getQualifierArray();


//
//
//        while (it.hasNext()) {
//            Cell cell = it.next();
//            byte[] rowData = cell.getRowArray();
//
//            int offset;
//
////            offset = cell.getFamilyOffset();
////            byte[] currFamily = Arrays.copyOfRange(rowData, offset, offset + cell.getFamilyLength());
////            if (Arrays.equals(currFamily, metaDataFamily)) {
//            offset = cell.getQualifierOffset();
//            byte[] currQual = Arrays.copyOfRange(rowData, offset, offset + cell.getQualifierLength());
//
//            if (Arrays.equals(currQual, metaDataQualifier)) {
//                metadataStr = Bytes.toString(cell.getValueArray(), cell.getValueOffset(), cell.getValueLength());
//
//            }
//
//        }
    }
*/

    @Override
    public RegionScanner preScannerOpen(final ObserverContext<RegionCoprocessorEnvironment> c, final Scan scan,
                                        final RegionScanner s) throws IOException {
        try {

            Region region = (c.getEnvironment().getRegion());
            TableName table = getTableName(region);

            if (!table.isSystemTable()) {
                User user = getActiveUser();
                FilterData filterData = getUserPatterns(user);
                if (filterData == null) {
                    throw new IOException("Failed to get credentials");
                }
                if (scan.hasFilter()) {
                    Filter origFilter = scan.getFilter();
                    PoleFilterPreserveOrig newFilter = new PoleFilterPreserveOrig(origFilter, user, table, filterData);
                    scan.setFilter(newFilter);
                } else {
                    PoleFilter newFilter = new PoleFilter(user, table, filterData);
                    scan.setFilter(newFilter);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error("Found error when filtering data: ", e);
            throw new IOException(e.getMessage());

        }

        return s;
    }

    @Override
    public void preStopRegionServer(ObserverContext<RegionServerCoprocessorEnvironment> env) throws IOException {

    }

    @Override
    public void preMerge(ObserverContext<RegionServerCoprocessorEnvironment> ctx, Region regionA, Region regionB) throws IOException {

    }

    @Override
    public void postMerge(ObserverContext<RegionServerCoprocessorEnvironment> c, Region regionA, Region regionB, Region mergedRegion) throws IOException {

    }

    @Override
    public void preMergeCommit(ObserverContext<RegionServerCoprocessorEnvironment> ctx, Region regionA, Region regionB, @MetaMutationAnnotation List<Mutation> metaEntries) throws IOException {

    }

    @Override
    public void postMergeCommit(ObserverContext<RegionServerCoprocessorEnvironment> ctx, Region regionA, Region regionB, Region mergedRegion) throws IOException {

    }

    @Override
    public void preRollBackMerge(ObserverContext<RegionServerCoprocessorEnvironment> ctx, Region regionA, Region regionB) throws IOException {

    }

    @Override
    public void postRollBackMerge(ObserverContext<RegionServerCoprocessorEnvironment> ctx, Region regionA, Region regionB) throws IOException {

    }

    @Override
    public void preRollWALWriterRequest(ObserverContext<RegionServerCoprocessorEnvironment> ctx) throws IOException {

    }

    @Override
    public void postRollWALWriterRequest(ObserverContext<RegionServerCoprocessorEnvironment> ctx) throws IOException {

    }

    @Override
    public ReplicationEndpoint postCreateReplicationEndPoint(ObserverContext<RegionServerCoprocessorEnvironment> ctx, ReplicationEndpoint endpoint) {
        return endpoint;
    }

    @Override
    public void preReplicateLogEntries(ObserverContext<RegionServerCoprocessorEnvironment> ctx, List<AdminProtos.WALEntry> entries, CellScanner cells) throws IOException {

    }

    @Override
    public void postReplicateLogEntries(ObserverContext<RegionServerCoprocessorEnvironment> ctx, List<AdminProtos.WALEntry> entries, CellScanner cells) throws IOException {

    }

    @Override
    public boolean preWALWrite(ObserverContext<? extends WALCoprocessorEnvironment> ctx, HRegionInfo info, WALKey logKey, WALEdit logEdit) throws IOException {
        return true;
    }

    @Override
    public boolean preWALWrite(ObserverContext<WALCoprocessorEnvironment> ctx, HRegionInfo info, HLogKey logKey, WALEdit logEdit) throws IOException {
        return true;
    }

    @Override
    public void postWALWrite(ObserverContext<? extends WALCoprocessorEnvironment> ctx, HRegionInfo info, WALKey logKey, WALEdit logEdit) throws IOException {

    }

    @Override
    public void postWALWrite(ObserverContext<WALCoprocessorEnvironment> ctx, HRegionInfo info, HLogKey logKey, WALEdit logEdit) throws IOException {

    }


    //    @Override
//    public boolean postScannerNext(final ObserverContext<RegionCoprocessorEnvironment> c, final InternalScanner s,
//                                   final List<Result> results, final int limit, final boolean hasNext) throws IOException {
//
//
////        boolean skip = true;
////
////        if (skip) {
////            return hasNext;
////        }
//
//        //      RowFilter filter;
//
//        //       ArrayList<Result> redactionResults = new ArrayList<>(results.size());
//
//        try {
//            User user = getActiveUser();
//            Region region = getRegion(c.getEnvironment());
//            TableName table = getTableName(region);
//
//            Pattern[] patterns = getUserPatterns(user);
//            if (patterns[REDACTION_TABLES_REGEX].matcher(table.getNameAsString()).matches()) {
//
//                Iterator<Result> it = results.iterator();
//
//                while (it.hasNext()) {
//                    Result res = it.next();
//                    List<Cell> cells = res.listCells();
//
//                    if (!filter(user, table, cells, patterns, true)) {
//                        it.remove();
//                    }
//
//
//                }
//
//            }
//        } catch (Exception e) {
//            LOG.error("Failed to filter data: ", e);
//        }
//
////        if (redactionResults.size() > 0) {
////
////            results.removeAll(redactionResults);
////        }
//
//        return hasNext;
//    }


}
