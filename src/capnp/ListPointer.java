package org.capnproto;

import java.nio.ByteBuffer;

final class ListPointer {
    public WirePointer ptr;

    public ListPointer(WirePointer ptr) {
        this.ptr = ptr;
    }

    public byte elementSize() {
        return (byte)(this.ptr.buffer.getInt(this.ptr.buffer_offset * 8 + 4) & 7);
    }

    public static byte elementSize(int elementSizeAndCount) {
        return (byte) (elementSizeAndCount & 7);
    }

    public int elementCount() {
        return this.ptr.buffer.getInt(this.ptr.buffer_offset * 8 + 4) >> 3;
    }

    public static int elementCount(int elementSizeAndCount) {
        return elementSizeAndCount >> 3;
    }

    public int inlineCompositeWordCount() {
        return this.elementCount();
    }

    public static int inlineCompositeWordCount(int elementSizeAndCount) {
        return elementCount(elementSizeAndCount);
    }
}
