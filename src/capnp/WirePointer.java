package org.capnproto;

import java.nio.ByteBuffer;

final class WirePointer {
    public static final byte STRUCT = 0;
    public static final byte LIST = 1;
    public static final byte FAR = 2;
    public static final byte OTHER = 3;

    public static boolean isNull(long wirePointer) {
        return wirePointer == 0;
    }

    public static int offsetAndKind(long wirePointer) {
        return (int)(wirePointer & 0xffffffff);
    }

    public static byte kind(long wirePointer) {
        return (byte)(offsetAndKind(wirePointer) & 3);
    }

    public static int target(int offset, long wirePointer) {
        return offset + 1 + (offsetAndKind(wirePointer) >> 2);
    }

    public static int inlineCompositeListElementCount(long wirePointer) {
        return offsetAndKind(wirePointer) >> 2;
    }

    public static int upper32Bits(long wirePointer) {
        return (int)(wirePointer >> 32);
    }

    public static int listPointer(long wirePointer) {
        return upper32Bits(wirePointer);
    }

    public static int structPointer(long wirePointer) {
        return upper32Bits(wirePointer);
    }

    public static long get(ByteBuffer buffer, int offset) {
        return buffer.getLong(offset * 8);
    }
}
