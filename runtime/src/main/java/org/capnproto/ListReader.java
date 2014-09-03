package org.capnproto;

public final class ListReader {
    final SegmentReader segment;
    final int ptr; // byte offset to front of list
    final int elementCount;
    final int step; // in bits
    final int structDataSize; // in bits
    final short structPointerCount;
    final int nestingLimit;

    public ListReader() {
        this.segment = null;
        this.ptr = 0;
        this.elementCount = 0;
        this.step = 0;
        this.structDataSize = 0;
        this.structPointerCount = 0;
        this.nestingLimit = 0x7fffffff;
    }

    public ListReader(SegmentReader segment, int ptr,
                      int elementCount, int step,
                      int structDataSize, short structPointerCount,
                      int nestingLimit) {
        this.segment = segment;
        this.ptr = ptr;
        this.elementCount = elementCount;
        this.step = step;
        this.structDataSize = structDataSize;
        this.structPointerCount = structPointerCount;
        this.nestingLimit = nestingLimit;

    }

    public int size() {
        return this.elementCount;
    }

    public StructReader getStructElement(int index) {
        // TODO check nesting limit

        int indexBit = index * this.step;

        int structData = this.ptr + (indexBit / 8);
        int structPointers = structData + (this.structDataSize / 8);

        return new StructReader(this.segment, structData, structPointers / 8, this.structDataSize,
            this.structPointerCount, (byte) (indexBit % 8), this.nestingLimit - 1);
    }

    public PointerReader getPointerElement(int index) {
        return new PointerReader(this.segment,
                                 (this.ptr + (index * this.step / Constants.BITS_PER_BYTE)) / Constants.BYTES_PER_WORD,
                                 this.nestingLimit);
    }
}
