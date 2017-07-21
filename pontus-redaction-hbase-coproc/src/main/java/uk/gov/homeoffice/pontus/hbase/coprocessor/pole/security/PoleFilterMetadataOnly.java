/**
 * Created by leo on 12/10/2016.
 */

package uk.gov.homeoffice.pontus.hbase.coprocessor.pole.security;

import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.filter.FilterBase;
import org.apache.hadoop.hbase.security.User;
import uk.gov.homeoffice.pontus.FilterData;

import java.io.IOException;
import java.util.List;

public class PoleFilterMetadataOnly extends FilterBase {

    User user;
    TableName table;

    FilterData patterns;


    public PoleFilterMetadataOnly(User user, TableName table, FilterData patterns) {
        this.user = user;
        this.table = table;
        this.patterns = patterns;
    }


    @Override
    public void reset() throws IOException {

    }

    @Override
    public boolean filterRowKey(byte[] buffer, int offset, int length) throws IOException {
        return false;
    }

    @Override
    public boolean filterAllRemaining() throws IOException {

        return false;
    }

    @Override
    public ReturnCode filterKeyValue(Cell v) throws IOException {
        return ReturnCode.INCLUDE_AND_NEXT_COL;
    }

    @Override
    public Cell transformCell(Cell v) throws IOException {
        return v;
    }

    @Override
    public KeyValue transform(KeyValue currentKV) throws IOException {
        return currentKV;
    }

    @Override
    public void filterRowCells(List<Cell> kvs) throws IOException {
        PoleSecurityCoprocessor.filterMetatata(user,table,kvs,patterns);
    }

    @Override
    public boolean hasFilterRow() {
        return true;
    }

    @Override
    public boolean filterRow() throws IOException {
        return false;
    }

    @Override
    public KeyValue getNextKeyHint(KeyValue currentKV) throws IOException {
        return null;
    }

    @Override
    public Cell getNextCellHint(Cell currentKV) throws IOException {
        return null;
    }

    @Override
    public boolean isFamilyEssential(byte[] name) throws IOException {
        return true;
    }

    @Override
    public byte[] toByteArray() throws IOException {
        return new byte[0];
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PoleFilterMetadataOnly that = (PoleFilterMetadataOnly) o;

        if (user != null ? !user.equals(that.user) : that.user != null) return false;
        if (table != null ? !table.equals(that.table) : that.table != null) return false;
        if (patterns != null ? !patterns.equals(that.patterns) : that.patterns != null) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals

        return true;
    }

    @Override
    public int hashCode() {
        int result = user != null ? user.hashCode() : 0;
        result = 31 * result + (table != null ? table.hashCode() : 0);
        result = 31 * result + (patterns != null ? patterns.hashCode() : 0);
        return result;
    }
}
