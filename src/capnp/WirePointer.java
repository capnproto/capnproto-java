package org.capnproto;

import java.nio.ByteBuffer;

final class WirePointer {
    public final ByteBuffer buffer;
    public final int buffer_offset; // in words

    public static final byte STRUCT = 0;
    public static final byte LIST = 1;
    public static final byte FAR = 2;
    public static final byte OTHER = 3;

    public WirePointer(ByteBuffer buffer, int offset) {
        this.buffer = buffer;
        this.buffer_offset = offset;
    }

    public WirePointer(WordPointer word) {
        this.buffer = word.buffer;
        this.buffer_offset = word.offset;
    }

    public boolean isNull() {
        return this.buffer.getLong(this.buffer_offset * 8) == 0;
    }

    public static boolean isNull(long wirePointer) {
        return wirePointer == 0;
    }

    public int offsetAndKind() {
        return this.buffer.getInt(this.buffer_offset * 8);
    }

    public static int offsetAndKind(long wirePointer) {
        return (int)(wirePointer & 0xffffffff);
    }

    public byte kind() {
        return (byte) (this.offsetAndKind() & 3);
    }

    public static byte kind(long wirePointer) {
        return (byte)(offsetAndKind(wirePointer) & 3);
    }

    public WordPointer target() {
        return new WordPointer(buffer,
                               this.buffer_offset + 1 + (this.offsetAndKind() >> 2));
    }

    public static int target(int offset, long wirePointer) {
        return offset + 1 + (offsetAndKind(wirePointer) >> 2);
    }

    public int inlineCompositeListElementCount() {
        return this.offsetAndKind() >> 2;
    }

    public static int inlineCompositeListElementCount(long wirePointer) {
        return offsetAndKind(wirePointer) >> 2;
    }

    // offset is in words
    public static int upper32Bits(ByteBuffer buffer, int offset) {
        return buffer.getInt(offset * 8 + 4);
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
