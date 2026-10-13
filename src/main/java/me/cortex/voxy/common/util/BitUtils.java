package me.cortex.voxy.common.util;

// 1.20.1 backport: Java 17 compatibility.
// Integer.compress/expand and Long.compress/expand were added in Java 19.
// These are manual implementations for Java 17.
public class BitUtils {
    public static int compressBits(int value, int mask) {
        int result = 0;
        int dstBit = 0;
        for (int i = 0; i < 32; i++) {
            if ((mask & (1 << i)) != 0) {
                if ((value & (1 << i)) != 0) {
                    result |= (1 << dstBit);
                }
                dstBit++;
            }
        }
        return result;
    }

    public static int expandBits(int value, int mask) {
        int result = 0;
        int srcBit = 0;
        for (int i = 0; i < 32; i++) {
            if ((mask & (1 << i)) != 0) {
                if ((value & (1 << srcBit)) != 0) {
                    result |= (1 << i);
                }
                srcBit++;
            }
        }
        return result;
    }

    public static long compressBitsLong(long value, long mask) {
        long result = 0;
        int dstBit = 0;
        for (int i = 0; i < 64; i++) {
            if ((mask & (1L << i)) != 0) {
                if ((value & (1L << i)) != 0) {
                    result |= (1L << dstBit);
                }
                dstBit++;
            }
        }
        return result;
    }

    public static long expandBitsLong(long value, long mask) {
        long result = 0;
        int srcBit = 0;
        for (int i = 0; i < 64; i++) {
            if ((mask & (1L << i)) != 0) {
                if ((value & (1L << srcBit)) != 0) {
                    result |= (1L << i);
                }
                srcBit++;
            }
        }
        return result;
    }
}
