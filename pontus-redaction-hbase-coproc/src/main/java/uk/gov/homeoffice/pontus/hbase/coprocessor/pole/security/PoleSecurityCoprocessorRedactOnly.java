package uk.gov.homeoffice.pontus.hbase.coprocessor.pole.security;

import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.coprocessor.*;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.regionserver.Region;
import org.apache.hadoop.hbase.regionserver.RegionScanner;
import org.apache.hadoop.hbase.security.User;
import uk.gov.homeoffice.pontus.*;

import java.io.IOException;
import java.util.*;

public class PoleSecurityCoprocessorRedactOnly extends PoleSecurityCoprocessor{


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
                filterRedaction(user, table, result, filterData);
            } catch (Exception e) {
                e.printStackTrace();
                LOG.error("Found error when filtering data: ", e);
                throw new IOException(e.getMessage());

            }
        }

    }


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
                    PoleFilterPreserveOrigRedactOnly newFilter = new PoleFilterPreserveOrigRedactOnly(origFilter, user, table, filterData);
                    scan.setFilter(newFilter);
                } else {
                    PoleFilterRedactOnly newFilter = new PoleFilterRedactOnly(user, table, filterData);
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

}
