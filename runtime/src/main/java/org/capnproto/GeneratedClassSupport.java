package org.capnproto;

public final class GeneratedClassSupport {
    public static SegmentReader decodeRawBytes(String s) {
        try {
            java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(s.getBytes("ISO_8859-1")).asReadOnlyBuffer();
            buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
            return new SegmentReader(buffer, new ReaderArena(new java.nio.ByteBuffer[0], 0x7fffffffffffffffL));
        } catch (Exception e) {
            throw new Error("could not decode raw bytes from String");
        }
    }
}
