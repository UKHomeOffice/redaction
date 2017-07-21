package uk.gov.homeoffice.pontus;

import com.google.common.base.Preconditions;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.SecureRandom;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.io.RawComparator;
import org.apache.hadoop.io.WritableComparator;
import org.apache.hadoop.io.WritableUtils;
import sun.misc.Unsafe;

public class Bytes {
    private static final String UTF8_ENCODING = "UTF-8";
    private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    private static final Log LOG = LogFactory.getLog(Bytes.class);
    public static final int SIZEOF_BOOLEAN = 1;
    public static final int SIZEOF_BYTE = 1;
    public static final int SIZEOF_CHAR = 2;
    public static final int SIZEOF_DOUBLE = 8;
    public static final int SIZEOF_FLOAT = 4;
    public static final int SIZEOF_INT = 4;
    public static final int SIZEOF_LONG = 8;
    public static final int SIZEOF_SHORT = 2;
    public static final long MASK_FOR_LOWER_INT_IN_LONG = -4294967296L;
    public static final int ESTIMATED_HEAP_TAX = 16;
    public static final Comparator<byte[]> BYTES_COMPARATOR = new Bytes.ByteArrayComparator();
    public static final RawComparator<byte[]> BYTES_RAWCOMPARATOR = new Bytes.ByteArrayComparator();
    private static final SecureRandom RNG = new SecureRandom();
    private static final char[] HEX_CHARS = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public Bytes() {
    }

    public static final int len(byte[] b) {
        return b == null?0:b.length;
    }

    public static byte[] readByteArray(DataInput in) throws IOException {
        int len = WritableUtils.readVInt(in);
        if(len < 0) {
            throw new NegativeArraySizeException(Integer.toString(len));
        } else {
            byte[] result = new byte[len];
            in.readFully(result, 0, len);
            return result;
        }
    }

    public static byte[] readByteArrayThrowsRuntime(DataInput in) {
        try {
            return readByteArray(in);
        } catch (Exception var2) {
            throw new RuntimeException(var2);
        }
    }

    public static void writeByteArray(DataOutput out, byte[] b) throws IOException {
        if(b == null) {
            WritableUtils.writeVInt(out, 0);
        } else {
            writeByteArray(out, b, 0, b.length);
        }

    }

    public static void writeByteArray(DataOutput out, byte[] b, int offset, int length) throws IOException {
        WritableUtils.writeVInt(out, length);
        out.write(b, offset, length);
    }

    public static int writeByteArray(byte[] tgt, int tgtOffset, byte[] src, int srcOffset, int srcLength) {
        byte[] vint = vintToBytes((long)srcLength);
        System.arraycopy(vint, 0, tgt, tgtOffset, vint.length);
        int offset = tgtOffset + vint.length;
        System.arraycopy(src, srcOffset, tgt, offset, srcLength);
        return offset + srcLength;
    }

    public static int putBytes(byte[] tgtBytes, int tgtOffset, byte[] srcBytes, int srcOffset, int srcLength) {
        System.arraycopy(srcBytes, srcOffset, tgtBytes, tgtOffset, srcLength);
        return tgtOffset + srcLength;
    }

    public static int putByte(byte[] bytes, int offset, byte b) {
        bytes[offset] = b;
        return offset + 1;
    }

    public static int putByteBuffer(byte[] bytes, int offset, ByteBuffer buf) {
        int len = buf.remaining();
        buf.get(bytes, offset, len);
        return offset + len;
    }

    public static byte[] toBytes(ByteBuffer buf) {
        ByteBuffer dup = buf.duplicate();
        dup.position(0);
        return readBytes(dup);
    }

    private static byte[] readBytes(ByteBuffer buf) {
        byte[] result = new byte[buf.remaining()];
        buf.get(result);
        return result;
    }

    public static String toString(byte[] b) {
        return b == null?null:toString(b, 0, b.length);
    }

    public static String toString(byte[] b1, String sep, byte[] b2) {
        return toString(b1, 0, b1.length) + sep + toString(b2, 0, b2.length);
    }

    public static String toString(byte[] b, int off) {
        if(b == null) {
            return null;
        } else {
            int len = b.length - off;
            return len <= 0?"":new String(b, off, len, UTF8_CHARSET);
        }
    }

    public static String toString(byte[] b, int off, int len) {
        return b == null?null:(len == 0?"":new String(b, off, len, UTF8_CHARSET));
    }

    public static String toStringBinary(byte[] b) {
        return b == null?"null":toStringBinary(b, 0, b.length);
    }

    public static String toStringBinary(ByteBuffer buf) {
        return buf == null?"null":(buf.hasArray()?toStringBinary(buf.array(), buf.arrayOffset(), buf.limit()):toStringBinary(toBytes(buf)));
    }

    public static String toStringBinary(byte[] b, int off, int len) {
        StringBuilder result = new StringBuilder();
        if(off >= b.length) {
            return result.toString();
        } else {
            if(off + len > b.length) {
                len = b.length - off;
            }

            for(int i = off; i < off + len; ++i) {
                int ch = b[i] & 255;
                if((ch < 48 || ch > 57) && (ch < 65 || ch > 90) && (ch < 97 || ch > 122) && " `~!@#$%^&*()-_=+[]{}|;:\'\",.<>/?".indexOf(ch) < 0) {
                    result.append(String.format("\\x%02X", new Object[]{Integer.valueOf(ch)}));
                } else {
                    result.append((char)ch);
                }
            }

            return result.toString();
        }
    }

    private static boolean isHexDigit(char c) {
        return c >= 65 && c <= 70 || c >= 48 && c <= 57;
    }

    public static byte toBinaryFromHex(byte ch) {
        return ch >= 65 && ch <= 70?(byte)(10 + (byte)(ch - 65)):(byte)(ch - 48);
    }

    public static byte[] toBytesBinary(String in) {
        byte[] b = new byte[in.length()];
        int size = 0;

        for(int b2 = 0; b2 < in.length(); ++b2) {
            char ch = in.charAt(b2);
            if(ch == 92 && in.length() > b2 + 1 && in.charAt(b2 + 1) == 120) {
                char hd1 = in.charAt(b2 + 2);
                char hd2 = in.charAt(b2 + 3);
                if(isHexDigit(hd1) && isHexDigit(hd2)) {
                    byte d = (byte)((toBinaryFromHex((byte)hd1) << 4) + toBinaryFromHex((byte)hd2));
                    b[size++] = d;
                    b2 += 3;
                }
            } else {
                b[size++] = (byte)ch;
            }
        }

        byte[] var8 = new byte[size];
        System.arraycopy(b, 0, var8, 0, size);
        return var8;
    }

    public static byte[] toBytes(String s) {
        return s.getBytes(UTF8_CHARSET);
    }

    public static byte[] toBytes(boolean b) {
        return new byte[]{(byte)(b?-1:0)};
    }

    public static boolean toBoolean(byte[] b) {
        if(b.length != 1) {
            throw new IllegalArgumentException("Array has wrong size: " + b.length);
        } else {
            return b[0] != 0;
        }
    }

    public static byte[] toBytes(long val) {
        byte[] b = new byte[8];

        for(int i = 7; i > 0; --i) {
            b[i] = (byte)((int)val);
            val >>>= 8;
        }

        b[0] = (byte)((int)val);
        return b;
    }

    public static long toLong(byte[] bytes) {
        return toLong(bytes, 0, 8);
    }

    public static long toLong(byte[] bytes, int offset) {
        return toLong(bytes, offset, 8);
    }

    public static long toLong(byte[] bytes, int offset, int length) {
        if(length == 8 && offset + length <= bytes.length) {
            if(Bytes.LexicographicalComparerHolder.UnsafeComparer.isAvailable()) {
                return toLongUnsafe(bytes, offset);
            } else {
                long l = 0L;

                for(int i = offset; i < offset + length; ++i) {
                    l <<= 8;
                    l ^= (long)(bytes[i] & 255);
                }

                return l;
            }
        } else {
            throw explainWrongLengthOrOffset(bytes, offset, length, 8);
        }
    }

    private static IllegalArgumentException explainWrongLengthOrOffset(byte[] bytes, int offset, int length, int expectedLength) {
        String reason;
        if(length != expectedLength) {
            reason = "Wrong length: " + length + ", expected " + expectedLength;
        } else {
            reason = "offset (" + offset + ") + length (" + length + ") exceed the" + " capacity of the array: " + bytes.length;
        }

        return new IllegalArgumentException(reason);
    }

    public static int putLong(byte[] bytes, int offset, long val) {
        if(bytes.length - offset < 8) {
            throw new IllegalArgumentException("Not enough room to put a long at offset " + offset + " in a " + bytes.length + " byte array");
        } else if(Bytes.LexicographicalComparerHolder.UnsafeComparer.isAvailable()) {
            return putLongUnsafe(bytes, offset, val);
        } else {
            for(int i = offset + 7; i > offset; --i) {
                bytes[i] = (byte)((int)val);
                val >>>= 8;
            }

            bytes[offset] = (byte)((int)val);
            return offset + 8;
        }
    }

    public static int putLongUnsafe(byte[] bytes, int offset, long val) {
        if(Bytes.LexicographicalComparerHolder.UnsafeComparer.littleEndian) {
            val = Long.reverseBytes(val);
        }

        Bytes.LexicographicalComparerHolder.UnsafeComparer.theUnsafe.putLong(bytes, (long)offset + (long)Bytes.LexicographicalComparerHolder.UnsafeComparer.BYTE_ARRAY_BASE_OFFSET, val);
        return offset + 8;
    }

    public static float toFloat(byte[] bytes) {
        return toFloat(bytes, 0);
    }

    public static float toFloat(byte[] bytes, int offset) {
        return Float.intBitsToFloat(toInt(bytes, offset, 4));
    }

    public static int putFloat(byte[] bytes, int offset, float f) {
        return putInt(bytes, offset, Float.floatToRawIntBits(f));
    }

    public static byte[] toBytes(float f) {
        return toBytes(Float.floatToRawIntBits(f));
    }

    public static double toDouble(byte[] bytes) {
        return toDouble(bytes, 0);
    }

    public static double toDouble(byte[] bytes, int offset) {
        return Double.longBitsToDouble(toLong(bytes, offset, 8));
    }

    public static int putDouble(byte[] bytes, int offset, double d) {
        return putLong(bytes, offset, Double.doubleToLongBits(d));
    }

    public static byte[] toBytes(double d) {
        return toBytes(Double.doubleToRawLongBits(d));
    }

    public static byte[] toBytes(int val) {
        byte[] b = new byte[4];

        for(int i = 3; i > 0; --i) {
            b[i] = (byte)val;
            val >>>= 8;
        }

        b[0] = (byte)val;
        return b;
    }

    public static int toInt(byte[] bytes) {
        return toInt(bytes, 0, 4);
    }

    public static int toInt(byte[] bytes, int offset) {
        return toInt(bytes, offset, 4);
    }

    public static int toInt(byte[] bytes, int offset, int length) {
        if(length == 4 && offset + length <= bytes.length) {
            if(Bytes.LexicographicalComparerHolder.UnsafeComparer.isAvailable()) {
                return toIntUnsafe(bytes, offset);
            } else {
                int n = 0;

                for(int i = offset; i < offset + length; ++i) {
                    n <<= 8;
                    n ^= bytes[i] & 255;
                }

                return n;
            }
        } else {
            throw explainWrongLengthOrOffset(bytes, offset, length, 4);
        }
    }

    public static int toIntUnsafe(byte[] bytes, int offset) {
        return Bytes.LexicographicalComparerHolder.UnsafeComparer.littleEndian?Integer.reverseBytes(Bytes.LexicographicalComparerHolder.UnsafeComparer.theUnsafe.getInt(bytes, (long)offset + (long)Bytes.LexicographicalComparerHolder.UnsafeComparer.BYTE_ARRAY_BASE_OFFSET)):Bytes.LexicographicalComparerHolder.UnsafeComparer.theUnsafe.getInt(bytes, (long)offset + (long)Bytes.LexicographicalComparerHolder.UnsafeComparer.BYTE_ARRAY_BASE_OFFSET);
    }

    public static short toShortUnsafe(byte[] bytes, int offset) {
        return Bytes.LexicographicalComparerHolder.UnsafeComparer.littleEndian?Short.reverseBytes(Bytes.LexicographicalComparerHolder.UnsafeComparer.theUnsafe.getShort(bytes, (long)offset + (long)Bytes.LexicographicalComparerHolder.UnsafeComparer.BYTE_ARRAY_BASE_OFFSET)):Bytes.LexicographicalComparerHolder.UnsafeComparer.theUnsafe.getShort(bytes, (long)offset + (long)Bytes.LexicographicalComparerHolder.UnsafeComparer.BYTE_ARRAY_BASE_OFFSET);
    }

    public static long toLongUnsafe(byte[] bytes, int offset) {
        return Bytes.LexicographicalComparerHolder.UnsafeComparer.littleEndian?Long.reverseBytes(Bytes.LexicographicalComparerHolder.UnsafeComparer.theUnsafe.getLong(bytes, (long)offset + (long)Bytes.LexicographicalComparerHolder.UnsafeComparer.BYTE_ARRAY_BASE_OFFSET)):Bytes.LexicographicalComparerHolder.UnsafeComparer.theUnsafe.getLong(bytes, (long)offset + (long)Bytes.LexicographicalComparerHolder.UnsafeComparer.BYTE_ARRAY_BASE_OFFSET);
    }

    public static int readAsInt(byte[] bytes, int offset, int length) {
        if(offset + length > bytes.length) {
            throw new IllegalArgumentException("offset (" + offset + ") + length (" + length + ") exceed the" + " capacity of the array: " + bytes.length);
        } else {
            int n = 0;

            for(int i = offset; i < offset + length; ++i) {
                n <<= 8;
                n ^= bytes[i] & 255;
            }

            return n;
        }
    }

    public static int putInt(byte[] bytes, int offset, int val) {
        if(bytes.length - offset < 4) {
            throw new IllegalArgumentException("Not enough room to put an int at offset " + offset + " in a " + bytes.length + " byte array");
        } else if(Bytes.LexicographicalComparerHolder.UnsafeComparer.isAvailable()) {
            return putIntUnsafe(bytes, offset, val);
        } else {
            for(int i = offset + 3; i > offset; --i) {
                bytes[i] = (byte)val;
                val >>>= 8;
            }

            bytes[offset] = (byte)val;
            return offset + 4;
        }
    }

    public static int putIntUnsafe(byte[] bytes, int offset, int val) {
        if(Bytes.LexicographicalComparerHolder.UnsafeComparer.littleEndian) {
            val = Integer.reverseBytes(val);
        }

        Bytes.LexicographicalComparerHolder.UnsafeComparer.theUnsafe.putInt(bytes, (long)offset + (long)Bytes.LexicographicalComparerHolder.UnsafeComparer.BYTE_ARRAY_BASE_OFFSET, val);
        return offset + 4;
    }

    public static byte[] toBytes(short val) {
        byte[] b = new byte[]{0, (byte)val};
        val = (short)(val >> 8);
        b[0] = (byte)val;
        return b;
    }

    public static short toShort(byte[] bytes) {
        return toShort(bytes, 0, 2);
    }

    public static short toShort(byte[] bytes, int offset) {
        return toShort(bytes, offset, 2);
    }

    public static short toShort(byte[] bytes, int offset, int length) {
        if(length == 2 && offset + length <= bytes.length) {
            if(Bytes.LexicographicalComparerHolder.UnsafeComparer.isAvailable()) {
                return toShortUnsafe(bytes, offset);
            } else {
                byte n = 0;
                short n1 = (short)(n ^ bytes[offset] & 255);
                n1 = (short)(n1 << 8);
                n1 = (short)(n1 ^ bytes[offset + 1] & 255);
                return n1;
            }
        } else {
            throw explainWrongLengthOrOffset(bytes, offset, length, 2);
        }
    }

    public static byte[] getBytes(ByteBuffer buf) {
        return readBytes(buf.duplicate());
    }

    public static int putShort(byte[] bytes, int offset, short val) {
        if(bytes.length - offset < 2) {
            throw new IllegalArgumentException("Not enough room to put a short at offset " + offset + " in a " + bytes.length + " byte array");
        } else if(Bytes.LexicographicalComparerHolder.UnsafeComparer.isAvailable()) {
            return putShortUnsafe(bytes, offset, val);
        } else {
            bytes[offset + 1] = (byte)val;
            val = (short)(val >> 8);
            bytes[offset] = (byte)val;
            return offset + 2;
        }
    }

    public static int putShortUnsafe(byte[] bytes, int offset, short val) {
        if(Bytes.LexicographicalComparerHolder.UnsafeComparer.littleEndian) {
            val = Short.reverseBytes(val);
        }

        Bytes.LexicographicalComparerHolder.UnsafeComparer.theUnsafe.putShort(bytes, (long)offset + (long)Bytes.LexicographicalComparerHolder.UnsafeComparer.BYTE_ARRAY_BASE_OFFSET, val);
        return offset + 2;
    }

    public static int putAsShort(byte[] bytes, int offset, int val) {
        if(bytes.length - offset < 2) {
            throw new IllegalArgumentException("Not enough room to put a short at offset " + offset + " in a " + bytes.length + " byte array");
        } else {
            bytes[offset + 1] = (byte)val;
            val >>= 8;
            bytes[offset] = (byte)val;
            return offset + 2;
        }
    }

    public static byte[] toBytes(BigDecimal val) {
        byte[] valueBytes = val.unscaledValue().toByteArray();
        byte[] result = new byte[valueBytes.length + 4];
        int offset = putInt(result, 0, val.scale());
        putBytes(result, offset, valueBytes, 0, valueBytes.length);
        return result;
    }

    public static BigDecimal toBigDecimal(byte[] bytes) {
        return toBigDecimal(bytes, 0, bytes.length);
    }

    public static BigDecimal toBigDecimal(byte[] bytes, int offset, int length) {
        if(bytes != null && length >= 5 && offset + length <= bytes.length) {
            int scale = toInt(bytes, offset);
            byte[] tcBytes = new byte[length - 4];
            System.arraycopy(bytes, offset + 4, tcBytes, 0, length - 4);
            return new BigDecimal(new BigInteger(tcBytes), scale);
        } else {
            return null;
        }
    }

    public static int putBigDecimal(byte[] bytes, int offset, BigDecimal val) {
        if(bytes == null) {
            return offset;
        } else {
            byte[] valueBytes = val.unscaledValue().toByteArray();
            byte[] result = new byte[valueBytes.length + 4];
            offset = putInt(result, offset, val.scale());
            return putBytes(result, offset, valueBytes, 0, valueBytes.length);
        }
    }

    public static byte[] vintToBytes(long vint) {
        long i = vint;
        int size = WritableUtils.getVIntSize(vint);
        byte[] result = new byte[size];
        byte offset = 0;
        if(vint >= -112L && vint <= 127L) {
            result[offset] = (byte)((int)vint);
            return result;
        } else {
            int len = -112;
            if(vint < 0L) {
                i = ~vint;
                len = -120;
            }

            for(long tmp = i; tmp != 0L; --len) {
                tmp >>= 8;
            }

            int var14 = offset + 1;
            result[offset] = (byte)len;
            len = len < -120?-(len + 120):-(len + 112);

            for(int idx = len; idx != 0; --idx) {
                int shiftbits = (idx - 1) * 8;
                long mask = 255L << shiftbits;
                result[var14++] = (byte)((int)((i & mask) >> shiftbits));
            }

            return result;
        }
    }

    public static long bytesToVint(byte[] buffer) {
        byte offset = 0;
        int var8 = offset + 1;
        byte firstByte = buffer[offset];
        int len = WritableUtils.decodeVIntSize(firstByte);
        if(len == 1) {
            return (long)firstByte;
        } else {
            long i = 0L;

            for(int idx = 0; idx < len - 1; ++idx) {
                byte b = buffer[var8++];
                i <<= 8;
                i |= (long)(b & 255);
            }

            return WritableUtils.isNegativeVInt(firstByte)?~i:i;
        }
    }

    /** @deprecated */
    @Deprecated
    public static long readVLong(byte[] buffer, int offset) throws IOException {
        return readAsVLong(buffer, offset);
    }

    public static long readAsVLong(byte[] buffer, int offset) {
        byte firstByte = buffer[offset];
        int len = WritableUtils.decodeVIntSize(firstByte);
        if(len == 1) {
            return (long)firstByte;
        } else {
            long i = 0L;

            for(int idx = 0; idx < len - 1; ++idx) {
                byte b = buffer[offset + 1 + idx];
                i <<= 8;
                i |= (long)(b & 255);
            }

            return WritableUtils.isNegativeVInt(firstByte)?~i:i;
        }
    }

    public static int compareTo(byte[] left, byte[] right) {
        return Bytes.LexicographicalComparerHolder.BEST_COMPARER.compareTo(left, 0, left.length, right, 0, right.length);
    }

    public static int compareTo(byte[] buffer1, int offset1, int length1, byte[] buffer2, int offset2, int length2) {
        return Bytes.LexicographicalComparerHolder.BEST_COMPARER.compareTo(buffer1, offset1, length1, buffer2, offset2, length2);
    }

    static Bytes.Comparer<byte[]> lexicographicalComparerJavaImpl() {
        return Bytes.LexicographicalComparerHolder.PureJavaComparer.INSTANCE;
    }

    public static boolean equals(byte[] left, byte[] right) {
        return left == right?true:(left != null && right != null?(left.length != right.length?false:(left.length == 0?true:(left[left.length - 1] != right[right.length - 1]?false:compareTo(left, right) == 0))):false);
    }

    public static boolean equals(byte[] left, int leftOffset, int leftLen, byte[] right, int rightOffset, int rightLen) {
        return left == right && leftOffset == rightOffset && leftLen == rightLen?true:(leftLen != rightLen?false:(leftLen == 0?true:(left[leftOffset + leftLen - 1] != right[rightOffset + rightLen - 1]?false:Bytes.LexicographicalComparerHolder.BEST_COMPARER.compareTo(left, leftOffset, leftLen, right, rightOffset, rightLen) == 0)));
    }

    public static boolean equals(byte[] a, ByteBuffer buf) {
        if(a == null) {
            return buf == null;
        } else if(buf == null) {
            return false;
        } else if(a.length != buf.remaining()) {
            return false;
        } else {
            ByteBuffer b = buf.duplicate();
            byte[] arr$ = a;
            int len$ = a.length;

            for(int i$ = 0; i$ < len$; ++i$) {
                byte anA = arr$[i$];
                if(anA != b.get()) {
                    return false;
                }
            }

            return true;
        }
    }

    public static boolean startsWith(byte[] bytes, byte[] prefix) {
        return bytes != null && prefix != null && bytes.length >= prefix.length && Bytes.LexicographicalComparerHolder.BEST_COMPARER.compareTo(bytes, 0, prefix.length, prefix, 0, prefix.length) == 0;
    }

    public static int hashCode(byte[] b) {
        return hashCode(b, b.length);
    }

    public static int hashCode(byte[] b, int length) {
        return WritableComparator.hashBytes(b, length);
    }

    public static Integer mapKey(byte[] b) {
        return Integer.valueOf(hashCode(b));
    }

    public static Integer mapKey(byte[] b, int length) {
        return Integer.valueOf(hashCode(b, length));
    }

    public static byte[] add(byte[] a, byte[] b) {
        return add(a, b, EMPTY_BYTE_ARRAY);
    }

    public static byte[] add(byte[] a, byte[] b, byte[] c) {
        byte[] result = new byte[a.length + b.length + c.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        System.arraycopy(c, 0, result, a.length + b.length, c.length);
        return result;
    }

    public static byte[] add(byte[][] arrays) {
        int length = 0;

        for(int result = 0; result < arrays.length; ++result) {
            length += arrays[result].length;
        }

        byte[] var5 = new byte[length];
        int index = 0;

        for(int i = 0; i < arrays.length; ++i) {
            System.arraycopy(arrays[i], 0, var5, index, arrays[i].length);
            index += arrays[i].length;
        }

        return var5;
    }

    public static byte[] head(byte[] a, int length) {
        if(a.length < length) {
            return null;
        } else {
            byte[] result = new byte[length];
            System.arraycopy(a, 0, result, 0, length);
            return result;
        }
    }

    public static byte[] tail(byte[] a, int length) {
        if(a.length < length) {
            return null;
        } else {
            byte[] result = new byte[length];
            System.arraycopy(a, a.length - length, result, 0, length);
            return result;
        }
    }

    public static byte[] padHead(byte[] a, int length) {
        byte[] padding = new byte[length];

        for(int i = 0; i < length; ++i) {
            padding[i] = 0;
        }

        return add(padding, a);
    }

    public static byte[] padTail(byte[] a, int length) {
        byte[] padding = new byte[length];

        for(int i = 0; i < length; ++i) {
            padding[i] = 0;
        }

        return add(a, padding);
    }

    public static byte[][] split(byte[] a, byte[] b, int num) {
        return split(a, b, false, num);
    }

    public static byte[][] split(byte[] a, byte[] b, boolean inclusive, int num) {
        byte[][] ret = new byte[num + 2][];
        int i = 0;
        Iterable iter = iterateOnSplits(a, b, inclusive, num);
        if(iter == null) {
            return (byte[][])null;
        } else {
            byte[] elem;
            for(Iterator i$ = iter.iterator(); i$.hasNext(); ret[i++] = elem) {
                elem = (byte[])i$.next();
            }

            return ret;
        }
    }

    public static Iterable<byte[]> iterateOnSplits(byte[] a, byte[] b, int num) {
        return iterateOnSplits(a, b, false, num);
    }

    public static Iterable<byte[]> iterateOnSplits(final byte[] a, final byte[] b, boolean inclusive, final int num) {
        byte[] aPadded;
        byte[] bPadded;
        if(a.length < b.length) {
            aPadded = padTail(a, b.length - a.length);
            bPadded = b;
        } else if(b.length < a.length) {
            aPadded = a;
            bPadded = padTail(b, a.length - b.length);
        } else {
            aPadded = a;
            bPadded = b;
        }

        if(compareTo(aPadded, bPadded) >= 0) {
            throw new IllegalArgumentException("b <= a");
        } else if(num <= 0) {
            throw new IllegalArgumentException("num cannot be <= 0");
        } else {
            byte[] prependHeader = new byte[]{1, 0};
            final BigInteger startBI = new BigInteger(add(prependHeader, aPadded));
            BigInteger stopBI = new BigInteger(add(prependHeader, bPadded));
            BigInteger diffBI = stopBI.subtract(startBI);
            if(inclusive) {
                diffBI = diffBI.add(BigInteger.ONE);
            }

            BigInteger splitsBI = BigInteger.valueOf((long)(num + 1));
            if(diffBI.compareTo(splitsBI) >= 0) {
                final BigInteger var15;
                try {
                    var15 = diffBI.divide(splitsBI);
                } catch (Exception var14) {
                    LOG.error("Exception caught during division", var14);
                    return null;
                }

                final Iterator var16 = new Iterator() {
                    private int i = -1;

                    public boolean hasNext() {
                        return this.i < num + 1;
                    }

                    public byte[] next() {
                        ++this.i;
                        if(this.i == 0) {
                            return a;
                        } else if(this.i == num + 1) {
                            return b;
                        } else {
                            BigInteger curBI = startBI.add(var15.multiply(BigInteger.valueOf((long)this.i)));
                            byte[] padded = curBI.toByteArray();
                            if(padded[1] == 0) {
                                padded = Bytes.tail(padded, padded.length - 2);
                            } else {
                                padded = Bytes.tail(padded, padded.length - 1);
                            }

                            return padded;
                        }
                    }

                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
                return new Iterable() {
                    public Iterator<byte[]> iterator() {
                        return var16;
                    }
                };
            } else {
                byte[] intervalBI = new byte[aPadded.length + 1];
                byte[] iterator = new byte[bPadded.length + 1];

                int j;
                for(j = 0; j < aPadded.length; ++j) {
                    intervalBI[j] = aPadded[j];
                }

                for(j = 0; j < bPadded.length; ++j) {
                    iterator[j] = bPadded[j];
                }

                intervalBI[aPadded.length] = 0;
                iterator[bPadded.length] = 0;
                return iterateOnSplits(intervalBI, iterator, inclusive, num);
            }
        }
    }

    public static int hashCode(byte[] bytes, int offset, int length) {
        int hash = 1;

        for(int i = offset; i < offset + length; ++i) {
            hash = 31 * hash + bytes[i];
        }

        return hash;
    }

    public static byte[][] toByteArrays(String[] t) {
        byte[][] result = new byte[t.length][];

        for(int i = 0; i < t.length; ++i) {
            result[i] = toBytes(t[i]);
        }

        return result;
    }

    public static byte[][] toBinaryByteArrays(String[] t) {
        byte[][] result = new byte[t.length][];

        for(int i = 0; i < t.length; ++i) {
            result[i] = toBytesBinary(t[i]);
        }

        return result;
    }

    public static byte[][] toByteArrays(String column) {
        return toByteArrays(toBytes(column));
    }

    public static byte[][] toByteArrays(byte[] column) {
        byte[][] result = new byte[][]{column};
        return result;
    }

    public static int binarySearch(byte[][] arr, byte[] key, int offset, int length, RawComparator<?> comparator) {
        int low = 0;
        int high = arr.length - 1;

        while(low <= high) {
            int mid = low + high >>> 1;
            int cmp = comparator.compare(key, offset, length, arr[mid], 0, arr[mid].length);
            if(cmp > 0) {
                low = mid + 1;
            } else {
                if(cmp >= 0) {
                    return mid;
                }

                high = mid - 1;
            }
        }

        return -(low + 1);
    }

    public static byte[] incrementBytes(byte[] value, long amount) {
        byte[] val = value;
        if(value.length < 8) {
            byte[] newvalue;
            if(value[0] < 0) {
                newvalue = new byte[]{-1, -1, -1, -1, -1, -1, -1, -1};
            } else {
                newvalue = new byte[8];
            }

            System.arraycopy(value, 0, newvalue, newvalue.length - value.length, value.length);
            val = newvalue;
        } else if(value.length > 8) {
            throw new IllegalArgumentException("Increment Bytes - value too big: " + value.length);
        }

        return amount == 0L?val:(val[0] < 0?binaryIncrementNeg(val, amount):binaryIncrementPos(val, amount));
    }

    private static byte[] binaryIncrementPos(byte[] value, long amount) {
        long amo = amount;
        byte sign = 1;
        if(amount < 0L) {
            amo = -amount;
            sign = -1;
        }

        for(int i = 0; i < value.length; ++i) {
            int cur = (int)amo % 256 * sign;
            amo >>= 8;
            int val = value[value.length - i - 1] & 255;
            int total = val + cur;
            if(total > 255) {
                amo += (long)sign;
                total %= 256;
            } else if(total < 0) {
                amo -= (long)sign;
            }

            value[value.length - i - 1] = (byte)total;
            if(amo == 0L) {
                return value;
            }
        }

        return value;
    }

    private static byte[] binaryIncrementNeg(byte[] value, long amount) {
        long amo = amount;
        byte sign = 1;
        if(amount < 0L) {
            amo = -amount;
            sign = -1;
        }

        for(int i = 0; i < value.length; ++i) {
            int cur = (int)amo % 256 * sign;
            amo >>= 8;
            int val = (~value[value.length - i - 1] & 255) + 1;
            int total = cur - val;
            if(total >= 0) {
                amo += (long)sign;
            } else if(total < -256) {
                amo -= (long)sign;
                total %= 256;
            }

            value[value.length - i - 1] = (byte)total;
            if(amo == 0L) {
                return value;
            }
        }

        return value;
    }

    public static void writeStringFixedSize(DataOutput out, String s, int size) throws IOException {
        byte[] b = toBytes(s);
        if(b.length > size) {
            throw new IOException("Trying to write " + b.length + " bytes (" + toStringBinary(b) + ") into a field of length " + size);
        } else {
            out.writeBytes(s);

            for(int i = 0; i < size - s.length(); ++i) {
                out.writeByte(0);
            }

        }
    }

    public static String readStringFixedSize(DataInput in, int size) throws IOException {
        byte[] b = new byte[size];
        in.readFully(b);

        int n;
        for(n = b.length; n > 0 && b[n - 1] == 0; --n) {
            ;
        }

        return toString(b, 0, n);
    }

    public static byte[] copy(byte[] bytes) {
        if(bytes == null) {
            return null;
        } else {
            byte[] result = new byte[bytes.length];
            System.arraycopy(bytes, 0, result, 0, bytes.length);
            return result;
        }
    }

    public static byte[] copy(byte[] bytes, int offset, int length) {
        if(bytes == null) {
            return null;
        } else {
            byte[] result = new byte[length];
            System.arraycopy(bytes, offset, result, 0, length);
            return result;
        }
    }

    public static int unsignedBinarySearch(byte[] a, int fromIndex, int toIndex, byte key) {
        int unsignedKey = key & 255;
        int low = fromIndex;
        int high = toIndex - 1;

        while(low <= high) {
            int mid = low + high >>> 1;
            int midVal = a[mid] & 255;
            if(midVal < unsignedKey) {
                low = mid + 1;
            } else {
                if(midVal <= unsignedKey) {
                    return mid;
                }

                high = mid - 1;
            }
        }

        return -(low + 1);
    }

    public static byte[] unsignedCopyAndIncrement(byte[] input) {
        byte[] copy = copy(input);
        if(copy == null) {
            throw new IllegalArgumentException("cannot increment null array");
        } else {
            for(int out = copy.length - 1; out >= 0; --out) {
                if(copy[out] != -1) {
                    ++copy[out];
                    return copy;
                }

                copy[out] = 0;
            }

            byte[] var3 = new byte[copy.length + 1];
            var3[0] = 1;
            System.arraycopy(copy, 0, var3, 1, copy.length);
            return var3;
        }
    }

    public static boolean equals(List<byte[]> a, List<byte[]> b) {
        if(a == null) {
            return b == null;
        } else if(b == null) {
            return false;
        } else if(a.size() != b.size()) {
            return false;
        } else {
            for(int i = 0; i < a.size(); ++i) {
                if(!equals((byte[])a.get(i), (byte[])b.get(i))) {
                    return false;
                }
            }

            return true;
        }
    }

//    public static boolean isSorted(Collection<byte[]> arrays) {
//        byte[] previous = new byte[0];
//
//        byte[] array;
//        for(Iterator i$ = IterableUtils.nullSafe(arrays).iterator(); i$.hasNext(); previous = array) {
//            array = (byte[])i$.next();
//            if(compareTo(previous, array) > 0) {
//                return false;
//            }
//        }
//
//        return true;
//    }

//    public static List<byte[]> getUtf8ByteArrays(List<String> strings) {
//        ArrayList byteArrays = Lists.newArrayListWithCapacity(CollectionUtils.nullSafeSize(strings));
//        Iterator i$ = IterableUtils.nullSafe(strings).iterator();
//
//        while(i$.hasNext()) {
//            String s = (String)i$.next();
//            byteArrays.add(toBytes(s));
//        }
//
//        return byteArrays;
//    }

    public static int indexOf(byte[] array, byte target) {
        for(int i = 0; i < array.length; ++i) {
            if(array[i] == target) {
                return i;
            }
        }

        return -1;
    }

    public static int indexOf(byte[] array, byte[] target) {
        Preconditions.checkNotNull(array, "array");
        Preconditions.checkNotNull(target, "target");
        if(target.length == 0) {
            return 0;
        } else {
            label28:
            for(int i = 0; i < array.length - target.length + 1; ++i) {
                for(int j = 0; j < target.length; ++j) {
                    if(array[i + j] != target[j]) {
                        continue label28;
                    }
                }

                return i;
            }

            return -1;
        }
    }

    public static boolean contains(byte[] array, byte target) {
        return indexOf(array, target) > -1;
    }

    public static boolean contains(byte[] array, byte[] target) {
        return indexOf(array, target) > -1;
    }

//    public static void zero(byte[] b) {
//        zero(b, 0, b.length);
//    }

//    public static void zero(byte[] b, int offset, int length) {
//        Preconditions.checkPositionIndex(offset, b.length, "offset");
//        Preconditions.checkArgument(length > 0, "length must be greater than 0");
//        Preconditions.checkPositionIndex(offset + length, b.length, "offset + length");
//        Arrays.fill(b, offset, offset + length, 0);
//    }

    public static void random(byte[] b) {
        RNG.nextBytes(b);
    }

    public static void random(byte[] b, int offset, int length) {
        Preconditions.checkPositionIndex(offset, b.length, "offset");
        Preconditions.checkArgument(length > 0, "length must be greater than 0");
        Preconditions.checkPositionIndex(offset + length, b.length, "offset + length");
        byte[] buf = new byte[length];
        RNG.nextBytes(buf);
        System.arraycopy(buf, 0, b, offset, length);
    }

    public static byte[] createMaxByteArray(int maxByteCount) {
        byte[] maxByteArray = new byte[maxByteCount];

        for(int i = 0; i < maxByteArray.length; ++i) {
            maxByteArray[i] = -1;
        }

        return maxByteArray;
    }

    public static byte[] multiple(byte[] srcBytes, int multiNum) {
        if(multiNum <= 0) {
            return new byte[0];
        } else {
            byte[] result = new byte[srcBytes.length * multiNum];

            for(int i = 0; i < multiNum; ++i) {
                System.arraycopy(srcBytes, 0, result, i * srcBytes.length, srcBytes.length);
            }

            return result;
        }
    }

    public static String toHex(byte[] b, int offset, int length) {
        Preconditions.checkArgument(length <= 1073741823);
        int numChars = length * 2;
        char[] ch = new char[numChars];

        for(int i = 0; i < numChars; i += 2) {
            byte d = b[offset + i / 2];
            ch[i] = HEX_CHARS[d >> 4 & 15];
            ch[i + 1] = HEX_CHARS[d & 15];
        }

        return new String(ch);
    }

    public static String toHex(byte[] b) {
        return toHex(b, 0, b.length);
    }

    private static int hexCharToNibble(char ch) {
        if(ch <= 57 && ch >= 48) {
            return ch - 48;
        } else if(ch >= 97 && ch <= 102) {
            return ch - 97 + 10;
        } else if(ch >= 65 && ch <= 70) {
            return ch - 65 + 10;
        } else {
            throw new IllegalArgumentException("Invalid hex char: " + ch);
        }
    }

    private static byte hexCharsToByte(char c1, char c2) {
        return (byte)(hexCharToNibble(c1) << 4 | hexCharToNibble(c2));
    }

    public static byte[] fromHex(String hex) {
        Preconditions.checkArgument(hex.length() % 2 == 0, "length must be a multiple of 2");
        int len = hex.length();
        byte[] b = new byte[len / 2];

        for(int i = 0; i < len; i += 2) {
            b[i / 2] = hexCharsToByte(hex.charAt(i), hex.charAt(i + 1));
        }

        return b;
    }

    static class LexicographicalComparerHolder {
        static final String UNSAFE_COMPARER_NAME = Bytes.LexicographicalComparerHolder.class.getName() + "$UnsafeComparer";
        static final Bytes.Comparer<byte[]> BEST_COMPARER = getBestComparer();

        LexicographicalComparerHolder() {
        }

        static Bytes.Comparer<byte[]> getBestComparer() {
            try {
                Class t = Class.forName(UNSAFE_COMPARER_NAME);
                Bytes.Comparer comparer = (Bytes.Comparer)t.getEnumConstants()[0];
                return comparer;
            } catch (Throwable var2) {
                return Bytes.lexicographicalComparerJavaImpl();
            }
        }

        static enum UnsafeComparer implements Bytes.Comparer<byte[]> {
            INSTANCE;

            static final Unsafe theUnsafe = (Unsafe)AccessController.doPrivileged(new PrivilegedAction() {
                public Object run() {
                    try {
                        Field e = Unsafe.class.getDeclaredField("theUnsafe");
                        e.setAccessible(true);
                        return e.get((Object)null);
                    } catch (NoSuchFieldException var2) {
                        throw new Error();
                    } catch (IllegalAccessException var3) {
                        throw new Error();
                    }
                }
            });
            static final int BYTE_ARRAY_BASE_OFFSET = theUnsafe.arrayBaseOffset(byte[].class);
            static final boolean littleEndian;

            private UnsafeComparer() {
            }

            static boolean lessThanUnsignedLong(long x1, long x2) {
                if(littleEndian) {
                    x1 = Long.reverseBytes(x1);
                    x2 = Long.reverseBytes(x2);
                }

                return x1 + -9223372036854775808L < x2 + -9223372036854775808L;
            }

            static boolean lessThanUnsignedInt(int x1, int x2) {
                if(littleEndian) {
                    x1 = Integer.reverseBytes(x1);
                    x2 = Integer.reverseBytes(x2);
                }

                return ((long)x1 & 4294967295L) < ((long)x2 & 4294967295L);
            }

            static boolean lessThanUnsignedShort(short x1, short x2) {
                if(littleEndian) {
                    x1 = Short.reverseBytes(x1);
                    x2 = Short.reverseBytes(x2);
                }

                return (x1 & '\uffff') < (x2 & '\uffff');
            }

            public static boolean isAvailable() {
                return theUnsafe != null;
            }

            public int compareTo(byte[] buffer1, int offset1, int length1, byte[] buffer2, int offset2, int length2) {
                if(buffer1 == buffer2 && offset1 == offset2 && length1 == length2) {
                    return 0;
                } else {
                    int minLength = Math.min(length1, length2);
                    int minWords = minLength / 8;
                    long offset1Adj = (long)(offset1 + BYTE_ARRAY_BASE_OFFSET);
                    long offset2Adj = (long)(offset2 + BYTE_ARRAY_BASE_OFFSET);
                    int j = minWords << 3;

                    int offset;
                    for(offset = 0; offset < j; offset += 8) {
                        long a = theUnsafe.getLong(buffer1, offset1Adj + (long)offset);
                        long rw = theUnsafe.getLong(buffer2, offset2Adj + (long)offset);
                        long diff = a ^ rw;
                        if(diff != 0L) {
                            return lessThanUnsignedLong(a, rw)?-1:1;
                        }
                    }

                    offset = j;
                    int b;
                    int a1;
                    if(minLength - j >= 4) {
                        a1 = theUnsafe.getInt(buffer1, offset1Adj + (long)j);
                        b = theUnsafe.getInt(buffer2, offset2Adj + (long)j);
                        if(a1 != b) {
                            return lessThanUnsignedInt(a1, b)?-1:1;
                        }

                        offset = j + 4;
                    }

                    if(minLength - offset >= 2) {
                        short a2 = theUnsafe.getShort(buffer1, offset1Adj + (long)offset);
                        short b1 = theUnsafe.getShort(buffer2, offset2Adj + (long)offset);
                        if(a2 != b1) {
                            return lessThanUnsignedShort(a2, b1)?-1:1;
                        }

                        offset += 2;
                    }

                    if(minLength - offset == 1) {
                        a1 = buffer1[offset1 + offset] & 255;
                        b = buffer2[offset2 + offset] & 255;
                        if(a1 != b) {
                            return a1 - b;
                        }
                    }

                    return length1 - length2;
                }
            }

            static {
                if(theUnsafe.arrayIndexScale(byte[].class) != 1) {
                    throw new AssertionError();
                } else {
                    littleEndian = ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN);
                }
            }
        }

        static enum PureJavaComparer implements Bytes.Comparer<byte[]> {
            INSTANCE;

            private PureJavaComparer() {
            }

            public int compareTo(byte[] buffer1, int offset1, int length1, byte[] buffer2, int offset2, int length2) {
                if(buffer1 == buffer2 && offset1 == offset2 && length1 == length2) {
                    return 0;
                } else {
                    int end1 = offset1 + length1;
                    int end2 = offset2 + length2;
                    int i = offset1;

                    for(int j = offset2; i < end1 && j < end2; ++j) {
                        int a = buffer1[i] & 255;
                        int b = buffer2[j] & 255;
                        if(a != b) {
                            return a - b;
                        }

                        ++i;
                    }

                    return length1 - length2;
                }
            }
        }
    }

    interface Comparer<T> {
        int compareTo(T var1, int var2, int var3, T var4, int var5, int var6);
    }

    public static class RowEndKeyComparator extends Bytes.ByteArrayComparator {
        public RowEndKeyComparator() {
        }

        public int compare(byte[] left, byte[] right) {
            return this.compare(left, 0, left.length, right, 0, right.length);
        }

        public int compare(byte[] b1, int s1, int l1, byte[] b2, int s2, int l2) {
            return b1 == b2 && s1 == s2 && l1 == l2?0:(l1 == 0?l2:(l2 == 0?-1:super.compare(b1, s1, l1, b2, s2, l2)));
        }
    }

    public static class ByteArrayComparator implements RawComparator<byte[]> {
        public ByteArrayComparator() {
        }

        public int compare(byte[] left, byte[] right) {
            return Bytes.compareTo(left, right);
        }

        public int compare(byte[] b1, int s1, int l1, byte[] b2, int s2, int l2) {
            return Bytes.LexicographicalComparerHolder.BEST_COMPARER.compareTo(b1, s1, l1, b2, s2, l2);
        }
    }
}
