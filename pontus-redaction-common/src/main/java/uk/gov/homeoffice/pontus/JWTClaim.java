package uk.gov.homeoffice.pontus;

import com.google.gson.Gson;
import org.apache.hadoop.util.Time;

import java.util.List;

public class JWTClaim implements HeapSize {

    static Gson gson = new Gson();

    public static JWTClaim fromJson(String json) {
        return gson.fromJson(json, JWTClaim.class);
    }
    public static JWTClaim fromBinary(byte[] array) {
        return fromJson(Bytes.toString(array, 0, array.length));
    }


    public static JWTClaim fromBinary(byte[] array, int offset, int length) {
        return fromJson(Bytes.toString(array, offset, length));
    }


    protected String bizctx = null;
    protected String sub = null;
    protected String iss = null;
    protected long startTimeMs = 0;
    protected long exp = 0;


    protected List<String> groups = null;

    public JWTClaim() {
    }

    public boolean isValid(){
        if (startTimeMs == 0 && exp == 0){
            return true;
        }
        long currTime = Time.monotonicNow();

        return startTimeMs <= currTime && currTime <= exp;
    }

    public String getSub() {
        return sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    public String getIss() {
        return iss;
    }

    public void setIss(String iss) {
        this.iss = iss;
    }


    public List<String> getGroups() { return groups; }
    public void setGroups(List<String> groups)
    {
        this.groups = groups;
    }


    public String getBizctx() {
        return bizctx;
    }

    public void setBizctx(String bizctx) {
        this.bizctx = bizctx;
    }

    public long getStartTimeMs() {
        return startTimeMs;
    }

    public void setStartTimeMs(long startTimeMs) {
        this.startTimeMs = startTimeMs;
    }

    public long getExp() {
        return exp;
    }

    public void setExp(long exp) {
        this.exp = exp;
    }


    @Override
    public long heapSize() {
        return bizctx == null ? 0 : bizctx.length();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JWTClaim jwtClaim1 = (JWTClaim) o;

        if (startTimeMs != jwtClaim1.startTimeMs) return false;
        if (exp != jwtClaim1.exp) return false;
        return bizctx != null ? bizctx.equals(jwtClaim1.bizctx) : jwtClaim1.bizctx == null;

    }

    @Override
    public int hashCode() {
        int result = bizctx != null ? bizctx.hashCode() : 0;
        result = 31 * result + sub != null ? sub.hashCode() : 0;
        result = 31 * result + iss != null ? iss.hashCode() : 0;
        result = 31 * result + (int) (startTimeMs ^ (startTimeMs >>> 32));
        result = 31 * result + (int) (exp ^ (exp >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return gson.toJson(this,this.getClass());
    }
}
