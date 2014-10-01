package org.capnproto;

public final class GeneratedClassSupport {

    public static <T> T clampOrdinal(T values[], short ordinal) {
        int index = ordinal;
        if (ordinal < 0 || ordinal >= values.length) {
            index = values.length - 1;
        }
        return values[index];
    }

    public static byte[] decodeRawBytes(String s) {
        try {
            return s.getBytes("ISO_8859-1");
        } catch (Exception e) {
            throw new Error("could not decode raw bytes from String");
        }
    }
}
