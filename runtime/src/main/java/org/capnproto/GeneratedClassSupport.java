package org.capnproto;

public final class GeneratedClassSupport {
    public static byte[] decodeRawBytes(String s) {
        try {
            return s.getBytes("ISO_8859-1");
        } catch (Exception e) {
            throw new Error("could not decode raw bytes from String");
        }
    }
}
