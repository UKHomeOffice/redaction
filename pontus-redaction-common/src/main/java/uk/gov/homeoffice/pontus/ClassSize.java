package uk.gov.homeoffice.pontus;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Created by leo on 31/10/2016.
 */
public class ClassSize {
    static final Log LOG = LogFactory.getLog(ClassSize.class);
    private static int nrOfRefsPerObj = 2;
    public static final int ARRAY;
    public static final int ARRAYLIST;
    public static final int BYTE_BUFFER;
    public static final int INTEGER;
    public static final int MAP_ENTRY;
    public static final int OBJECT;
    public static final int REFERENCE;
    public static final int STRING;
    public static final int TREEMAP;
    public static final int CONCURRENT_HASHMAP;
    public static final int CONCURRENT_HASHMAP_ENTRY;
    public static final int CONCURRENT_HASHMAP_SEGMENT;
    public static final int CONCURRENT_SKIPLISTMAP;
    public static final int CONCURRENT_SKIPLISTMAP_ENTRY;
    public static final int REENTRANT_LOCK;
    public static final int ATOMIC_LONG;
    public static final int ATOMIC_INTEGER;
    public static final int ATOMIC_BOOLEAN;
    public static final int COPYONWRITE_ARRAYSET;
    public static final int COPYONWRITE_ARRAYLIST;
    public static final int TIMERANGE;
    public static final int TIMERANGE_TRACKER;
    public static final int CELL_SKIPLIST_SET;
    private static final boolean JDK7;

    public ClassSize() {
    }

    private static int[] getSizeCoefficients(Class cl, boolean debug) {
        int primitives = 0;
        int arrays = 0;
        int references = nrOfRefsPerObj;

        for(int index = 0; null != cl; cl = cl.getSuperclass()) {
            Field[] field = cl.getDeclaredFields();
            if(null != field) {
                Field[] arr$ = field;
                int len$ = field.length;

                for(int i$ = 0; i$ < len$; ++i$) {
                    Field aField = arr$[i$];
                    if(!Modifier.isStatic(aField.getModifiers())) {
                        Class fieldClass = aField.getType();
                        if(fieldClass.isArray()) {
                            ++arrays;
                            ++references;
                        } else if(!fieldClass.isPrimitive()) {
                            ++references;
                        } else {
                            String name = fieldClass.getName();
                            if(!name.equals("int") && !name.equals("I")) {
                                if(!name.equals("long") && !name.equals("J")) {
                                    if(!name.equals("boolean") && !name.equals("Z")) {
                                        if(!name.equals("short") && !name.equals("S")) {
                                            if(!name.equals("byte") && !name.equals("B")) {
                                                if(!name.equals("char") && !name.equals("C")) {
                                                    if(!name.equals("float") && !name.equals("F")) {
                                                        if(name.equals("double") || name.equals("D")) {
                                                            primitives += 8;
                                                        }
                                                    } else {
                                                        primitives += 4;
                                                    }
                                                } else {
                                                    primitives += 2;
                                                }
                                            } else {
                                                ++primitives;
                                            }
                                        } else {
                                            primitives += 2;
                                        }
                                    } else {
                                        ++primitives;
                                    }
                                } else {
                                    primitives += 8;
                                }
                            } else {
                                primitives += 4;
                            }
                        }

                        if(debug && LOG.isDebugEnabled()) {
                            LOG.debug("" + index + " " + aField.getName() + " " + aField.getType());
                        }

                        ++index;
                    }
                }
            }
        }

        return new int[]{primitives, arrays, references};
    }

    private static long estimateBaseFromCoefficients(int[] coeff, boolean debug) {
        long prealign_size = (long)(coeff[0] + align(coeff[1] * ARRAY) + coeff[2] * REFERENCE);
        long size = align(prealign_size);
        if(debug && LOG.isDebugEnabled()) {
            LOG.debug("Primitives=" + coeff[0] + ", arrays=" + coeff[1] + ", references(includes " + nrOfRefsPerObj + " for object overhead)=" + coeff[2] + ", refSize " + REFERENCE + ", size=" + size + ", prealign_size=" + prealign_size);
        }

        return size;
    }

    public static long estimateBase(Class cl, boolean debug) {
        return estimateBaseFromCoefficients(getSizeCoefficients(cl, debug), debug);
    }

    public static int align(int num) {
        return (int)align((long)num);
    }

    public static long align(long num) {
        return num + 7L >> 3 << 3;
    }

    public static boolean is32BitJVM() {
        return System.getProperty("sun.arch.data.model").equals("32");
    }

    static {
        String version = System.getProperty("java.version");
        if(!version.matches("\\d\\.\\d\\..*")) {
            throw new RuntimeException("Unexpected version format: " + version);
        } else {
            int major = version.charAt(0) - 48;
            int minor = version.charAt(2) - 48;
            JDK7 = major == 1 && minor == 7;
            if(is32BitJVM()) {
                REFERENCE = 4;
            } else {
                REFERENCE = 8;
            }

            OBJECT = 2 * REFERENCE;
            ARRAY = align(3 * REFERENCE);
            ARRAYLIST = align(OBJECT + align(REFERENCE) + align(ARRAY) + 8);
            BYTE_BUFFER = align(OBJECT + align(REFERENCE) + align(ARRAY) + 20 + 3 + 8);
            INTEGER = align(OBJECT + 4);
            MAP_ENTRY = align(OBJECT + 5 * REFERENCE + 1);
            TREEMAP = align(OBJECT + 8 + align(7 * REFERENCE));
            STRING = (int)estimateBase(String.class, false);
            CONCURRENT_HASHMAP = (int)estimateBase(ConcurrentHashMap.class, false);
            CONCURRENT_HASHMAP_ENTRY = align(REFERENCE + OBJECT + 3 * REFERENCE + 8);
            CONCURRENT_HASHMAP_SEGMENT = align(REFERENCE + OBJECT + 12 + 4 + ARRAY);
            CONCURRENT_SKIPLISTMAP = (int)estimateBase(ConcurrentSkipListMap.class, false);
            CONCURRENT_SKIPLISTMAP_ENTRY = align(align(OBJECT + 3 * REFERENCE) + align((OBJECT + 3 * REFERENCE) / 2));
            REENTRANT_LOCK = align(OBJECT + 3 * REFERENCE);
            ATOMIC_LONG = align(OBJECT + 8);
            ATOMIC_INTEGER = align(OBJECT + 4);
            ATOMIC_BOOLEAN = align(OBJECT + 1);
            COPYONWRITE_ARRAYSET = align(OBJECT + REFERENCE);
            COPYONWRITE_ARRAYLIST = align(OBJECT + 2 * REFERENCE + ARRAY);
            TIMERANGE = align(OBJECT + 16 + 1);
            TIMERANGE_TRACKER = align(OBJECT + 16);
            CELL_SKIPLIST_SET = align(OBJECT + REFERENCE);
        }
    }
}

