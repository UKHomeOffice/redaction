/*
 * Copyright 2014 Nick Dimiduk
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HBaseTest extends Configured implements Tool {

    /** The identifier for the application table. */
//    private static final TableName TABLE_NAME = TableName.valueOf("MyTable");

    /**
     * The name of the column family used by the application.
     */
//    private static final byte[] CF = Bytes.toBytes("cf1");
    public void printUsage() {
        System.out.println("This tool inserts data into hbase. Usage:\n\n" +
                " java -cp /opt/pontus/pontus-hbase-coprocessor-0.99.0.jar HBaseTest " +
                "<table:row:colFamily:col> <val> <metadata>\n\n  Example:\n\n" +
                "java -cp /opt/pontus/pontus-hbase-coprocessor-0.99.0.jar HBaseTest " +
                "driver_dangerous_event:1:events:driverId 2222 /uk.gov.police/investigators\n\n");
    }

    public int run(String[] argv) throws IOException {


        Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.property.clientPort", "2181");
        conf.set("hbase.zookeeper.quorum", "sandbox.hortonworks.com");
        conf.set("zookeeper.znode.parent", "/hbase-unsecure");

        conf.addResource(new Path("/usr/hdp/current/hbase-regionserver/conf/hbase-default.xml"));
        conf.addResource(new Path("/usr/hdp/current/hbase-regionserver/conf/hbase-site.xml"));

        setConf(conf);

        Connection conn = null;
        try {
            String[] cellLocation = argv[0].split(":");
            if (cellLocation.length != 4) {
                printUsage();
                System.exit(-1);
            }

            String tableStr = cellLocation[0];
            String rowStr = cellLocation[1];
            String colFamilyStr = cellLocation[2];
            String colStr = cellLocation[3];
            String valStr = argv[1];
            String metadata = argv[2];


            /** A lightweight handle to a specific table. Used from a single thread. */
            TableName tname = TableName.valueOf(tableStr);
            Table table = null;
            try {
                // establish the connection to the cluster.

                conn = ConnectionFactory.createConnection(conf);
                Admin admin = conn.getAdmin();

                conn = ConnectionFactory.createConnection(conf);
                // retrieve a handle to the target table.

                if (!admin.tableExists(tname)) {
                    HColumnDescriptor colDesc = new HColumnDescriptor(colFamilyStr);
                    admin.createTable(new HTableDescriptor(tname).addFamily(colDesc));
                }


                table = conn.getTable(tname);

                byte[] row=Bytes.toBytes("myRow");
                byte[] cf=Bytes.toBytes("myCF");
                byte[] q=Bytes.toBytes("myQualifier");
                byte[] value=Bytes.toBytes("myValue");
                int noOfTags = 4;
                List<Tag> tags=new ArrayList<Tag>(noOfTags);
                for (int i=1; i <= noOfTags; i++) {
                    tags.add(new Tag((byte)i,Bytes.toBytes("tagValue" + i)));
                }
                Cell cell =  new KeyValue(row,cf,q,HConstants.LATEST_TIMESTAMP,value,tags);


                // describe the data we want to write.
                Put p = new Put(cell.getRowArray(),cell.getRowOffset(),cell.getRowLength());

                p.addColumn(Bytes.toBytes(colFamilyStr), Bytes.toBytes(colStr), Bytes.toBytes(valStr));
//                p.setAttribute("metadata", Bytes.toBytes(metadata));
//                send the data.

                table.put(p);

                Get g = new Get(Bytes.toBytes(rowStr));
//                g.addColumn(Bytes.toBytes(colFamilyStr), Bytes.toBytes(colStr));
                Result res = table.get(g);
                Cell[] cells = res.rawCells();
                System.out.println("num cells = " + cells.length);


                for (int i = 0, ilen = cells.length; i < ilen; i++)
                {
                    Cell rcell = cells[i];
                    int tagsLen = rcell.getTagsLength();
                    System.out.printf("tags len = %d\n",tagsLen);
                }

                System.out.println("Successfully got data");

            } finally {
                // close everything down
                if (table != null) table.close();
                if (conn != null) conn.close();
            }
        } catch (Throwable e) {
            e.printStackTrace();
            System.err.println(e.toString());

            printUsage();


            System.exit(-1);

        }
        return 0;
    }

    public static void main(String[] argv) throws Exception {
        int ret = ToolRunner.run(new HBaseTest(), argv);
        System.exit(ret);
    }
}
