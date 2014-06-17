package org.capnproto;

import java.nio.ByteBuffer;

final class StructPointer{
    public static short dataSize(long ref) {
        return (short)(WirePointer.upper32Bits(ref) & 0xffff);
    }

    public static short ptrCount(long ref) {
        return (short)(WirePointer.upper32Bits(ref) >>> 16);
    }

    public static int wordSize(long ref) {
        return (int)dataSize(ref) + (int)ptrCount(ref);
    }

    public static void setFromStructSize(ByteBuffer buffer, int offset, StructSize size) {
        buffer.putShort(8 * offset + 4, size.data);
        buffer.putShort(8 * offset + 6, size.pointers);
    }
}
