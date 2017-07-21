package uk.gov.homeoffice.pontus.kafka;

/**
 * Created by leo on 22/10/2016.
 */
public class KafkaRequest {
    public String val;
    public String col;
    public String row;
    public String table;

    protected StringBuffer strBuf = new StringBuffer();

    public void reset(){
        strBuf.setLength(0);
    }

    public String createKeyStr (){
        strBuf.append(table).append('#').append(row).append('#').append(col);
        return strBuf.toString();
    }

    public String getKeyStr(){
        return strBuf.toString();
    }

    public String getValStr(){
        return val;
    }

}
