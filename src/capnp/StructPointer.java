package org.capnproto;

import java.nio.ByteBuffer;

final class StructPointer{
    public WirePointer ptr;

    public StructPointer(WirePointer ptr) {
        this.ptr = ptr;
    }

    public short dataSize() {
        return this.ptr.buffer.getShort(this.ptr.buffer_offset * 8 + 4);
    }

    public static short dataSize(int structRef) {
        return (short)(structRef & 0xffff);
    }

    public short ptrCount() {
        return this.ptr.buffer.getShort(this.ptr.buffer_offset * 8 + 6);
    }

    public static short ptrCount(int structRef) {
        return (short)(structRef >> 16);
    }

    public int wordSize() {
        return this.dataSize() + this.ptrCount();
    }

    public static int wordSize(int structRef) {
        return (int)dataSize(structRef) + (int)ptrCount(structRef);
    }
}
