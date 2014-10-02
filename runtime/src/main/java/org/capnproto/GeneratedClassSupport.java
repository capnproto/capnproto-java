package org.capnproto;

public final class GeneratedClassSupport {
    public static java.nio.ByteBuffer decodeRawBytes(String s) {
        try {
            return java.nio.ByteBuffer.wrap(s.getBytes("ISO_8859-1")).asReadOnlyBuffer();
        } catch (Exception e) {
            throw new Error("could not decode raw bytes from String");
        }
    }
}
