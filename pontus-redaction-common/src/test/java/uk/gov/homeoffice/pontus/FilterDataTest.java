package uk.gov.homeoffice.pontus;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

/**
 * Created by leo on 05/11/2016.
 */
public class FilterDataTest {
    static int numVals = 10000000;
    static int maxSize = 1000;
    static String[] vals = randomStrings(numVals, maxSize);
    static FilterData fd = new FilterData();

    static {
        fd.setMetadataRegexStr(".*b.*");
        fd.setRedactionAllowedStr(".*a.*");
        fd.setRedactionDeniedStr("den.*");
        fd.setRedactionDeniedAllStr(".*sh");

    }

    public static String[] randomStrings(int num, int maxSize) {
        char[] chars = "abcdefghijklmnopqrstuvwxyz           ".toCharArray();
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        String[] retVal = new String[num];

        for (int j = 0; j < num; j++) {
            sb.setLength(0);
            for (int i = 0; i < random.nextInt(maxSize); i++) {
                char c = chars[random.nextInt(chars.length)];
                sb.append(c);
            }
            String output = sb.toString();
            retVal[j] = output;
        }

        return retVal;

    }

    @Test
    public void metaDataMatches() throws Exception {
        for (int i = 0; i < 5; i++){
            int jvm = metaDataMatchesJvm();
//            int dkb = metaDataMatchesDkb();
            int jre = metaDataMatchesJre();
            assertEquals(jvm,jre);
        }

    }

    @Test
    public void needRedaction() throws Exception {
        for (int i = 0; i < 5; i++){
            int jvm = needRedactionJvm();
//            int dkb = needRedactionDkb();
            int jre = needRedactionJre();
            assertEquals(jvm,jre);

        }
    }

    public int needRedactionJvm() throws Exception {

        int numMatches = 0;
        long start = System.nanoTime();

        for (int i = 0, ilen = vals.length; i < ilen; i++) {
            if (fd.needRedaction(vals[i])) numMatches++;
        }
        long end = System.nanoTime();

        double delta = numVals * (double) 1000000000 / (double) (end - start);
        System.out.printf("Message rate: %15.2f checks/sec, matches = %d\n", delta, numMatches);
        return numMatches;
    }

    public int metaDataMatchesJvm() throws Exception {

        int numMatches = 0;
        long start = System.nanoTime();

        for (int i = 0, ilen = vals.length; i < ilen; i++) {
            if (fd.metaDataMatches(vals[i])) numMatches++;
        }
        long end = System.nanoTime();

        double delta = numVals * (double) 1000000000 / (double) (end - start);
        System.out.printf("Message rate: %15.2f checks/sec, metadata matches = %d\n", delta, numMatches);
        return numMatches;
    }

    public int metaDataMatchesDkb() throws Exception {
        int numMatches = 0;

        long start = System.nanoTime();

        for (int i = 0, ilen = vals.length; i < ilen; i++) {
            if (fd.metaDataMatchesDkb(vals[i])) numMatches++;
        }
        long end = System.nanoTime();

        double delta = numVals * (double) 1000000000 / (double) (end - start);
        System.out.printf("Message rate: %15.2f checks/sec, metadata DKB matches = %d\n", delta, numMatches);
        return numMatches;
    }

    public int needRedactionDkb() throws Exception {
        int numMatches = 0;

        long start = System.nanoTime();

        for (int i = 0, ilen = vals.length; i < ilen; i++) {
            if (fd.needRedactionDkb(vals[i])) numMatches++;
        }
        long end = System.nanoTime();

        double delta = numVals * (double) 1000000000 / (double) (end - start);
        System.out.printf("Message rate: %15.2f checks/sec, DKB matches = %d\n", delta, numMatches);
        return numMatches;
    }

    //    @Test
//    public void needRedactionKmy() throws Exception {
//        int numMatches = 0;
//
//        long start = System.nanoTime();
//
//        for (int i = 0, ilen = vals.length; i < ilen; i++) {
//            if  (fd.needRedactionKmy(vals[i])) numMatches++;
//        }
//        long end = System.nanoTime();
//
//        double delta = numVals* (double)1000000000/ (double)(end - start);
//        System.out.printf ("Message rate: %15.2f checks/sec, KMY matches = %d\n", delta, numMatches);
//    }
    public int needRedactionJre() throws Exception {
        int numMatches = 0;

        long start = System.nanoTime();

        for (int i = 0, ilen = vals.length; i < ilen; i++) {
            if (fd.needRedactionJre(vals[i])) numMatches++;
        }
        long end = System.nanoTime();

        double delta = numVals * (double) 1000000000 / (double) (end - start);
        System.out.printf("Message rate: %15.2f checks/sec, JRE matches = %d\n", delta, numMatches);
        return numMatches;
    }
    public int metaDataMatchesJre() throws Exception {
        int numMatches = 0;

        long start = System.nanoTime();

        for (int i = 0, ilen = vals.length; i < ilen; i++) {
            if (fd.metaDataMatchesJre(vals[i])) numMatches++;
        }
        long end = System.nanoTime();

        double delta = numVals * (double) 1000000000 / (double) (end - start);
        System.out.printf("Message rate: %15.2f checks/sec, metadata JRE matches = %d\n", delta, numMatches);
        return numMatches;
    }

}