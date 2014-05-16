package org.capnproto;

import java.nio.ByteBuffer;

final class StructPointer{
    public static short dataSize(int structRef) {
        return (short)(structRef & 0xffff);
    }

    public static short ptrCount(int structRef) {
        return (short)(structRef >> 16);
    }

    public static int wordSize(int structRef) {
        return (int)dataSize(structRef) + (int)ptrCount(structRef);
    }
}
