/**
 * Created by leo on 12/10/2016.
 */

package uk.gov.homeoffice.pontus.hbase.coprocessor.pole.security;

import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.KeyValue;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.FilterBase;
import org.apache.hadoop.hbase.security.User;
import uk.gov.homeoffice.pontus.FilterData;

import java.io.IOException;
import java.util.List;

public class PoleFilterPreserveOrigRedactOnly extends FilterBase {

    Filter orig;
    User user;
    TableName table;
    FilterData patterns;


    public PoleFilterPreserveOrigRedactOnly(Filter orig, User user, TableName table, FilterData patterns) {
        this.orig = orig;
        this.user = user;
        this.table = table;
        this.patterns = patterns;
    }


    @Override
    public void reset() throws IOException {

        this.orig.reset();

    }

    @Override
    public boolean filterRowKey(byte[] buffer, int offset, int length) throws IOException {
        return this.orig.filterRowKey(buffer, offset, length);
    }

    @Override
    public boolean filterAllRemaining() throws IOException {
        return this.orig.filterAllRemaining();
    }

    @Override
    public ReturnCode filterKeyValue(Cell v) throws IOException {
        return this.orig.filterKeyValue(v);
    }

    @Override
    public Cell transformCell(Cell v) throws IOException {
        return this.orig.transformCell(v);
    }

    @Override
    public KeyValue transform(KeyValue currentKV) throws IOException {
        return this.orig.transform(currentKV);
    }

    @Override
    public void filterRowCells(List<Cell> kvs) throws IOException {

        long currTime = System.currentTimeMillis();
        this.orig.filterRowCells(kvs);
        PoleSecurityCoprocessor.filterRedaction(user, table, kvs, patterns,currTime);
    }

    @Override
    public boolean hasFilterRow() {
        return true;
    }

    @Override
    public boolean filterRow() throws IOException {
        return this.orig.filterRow();
    }

    @Override
    public KeyValue getNextKeyHint(KeyValue currentKV) throws IOException {
        return this.orig.getNextKeyHint(currentKV);
    }

    @Override
    public Cell getNextCellHint(Cell currentKV) throws IOException {
        return this.orig.getNextCellHint(currentKV);
    }

    @Override
    public boolean isFamilyEssential(byte[] name) throws IOException {
        return this.orig.isFamilyEssential(name);
    }

    @Override
    public byte[] toByteArray() throws IOException {
        return this.orig.toByteArray();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PoleFilterPreserveOrigRedactOnly that = (PoleFilterPreserveOrigRedactOnly) o;

        if (orig != null ? !orig.equals(that.orig) : that.orig != null) return false;
        if (user != null ? !user.equals(that.user) : that.user != null) return false;
        if (table != null ? !table.equals(that.table) : that.table != null) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (patterns != null ? !patterns.equals(that.patterns) : that.patterns != null) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals

        return true;

    }

    @Override
    public int hashCode() {
        int result = orig != null ? orig.hashCode() : 0;
        result = 31 * result + (user != null ? user.hashCode() : 0);
        result = 31 * result + (table != null ? table.hashCode() : 0);
        result = 31 * result + (patterns != null ? patterns.hashCode() : 0);
        return result;
    }

}
