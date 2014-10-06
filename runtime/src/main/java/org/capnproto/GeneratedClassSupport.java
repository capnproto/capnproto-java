package org.capnproto;

public final class GeneratedClassSupport {
    public static SegmentReader decodeRawBytes(String s) {
        try {
            return new SegmentReader(java.nio.ByteBuffer.wrap(s.getBytes("ISO_8859-1")).asReadOnlyBuffer(), null);
        } catch (Exception e) {
            throw new Error("could not decode raw bytes from String");
        }
    }
}
