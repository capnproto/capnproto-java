package org.capnproto;

public final class ListBuilder {
    final SegmentBuilder segment;
    final int ptr; // byte offset to front of list
    final int elementCount;
    final int step; // in bits
    final int structDataSize; // in bits
    final short structPointerCount;

    public ListBuilder(SegmentBuilder segment, int ptr,
                       int elementCount, int step,
                       int structDataSize, short structPointerCount) {
        this.segment = segment;
        this.ptr = ptr;
        this.elementCount = elementCount;
        this.step = step;
        this.structDataSize = structDataSize;
        this.structPointerCount = structPointerCount;
    }

    public int size() {
        return this.elementCount;
    }

    public final StructBuilder getStructElement(int index) {
        int indexBit = index * this.step;
        int structData = this.ptr + indexBit / 8 ;
        int structPointers = (structData + (this.structDataSize / 8)) / 8;

        return new StructBuilder(this.segment,
                                 structData,
                                 structPointers,
                                 this.structDataSize,
                                 this.structPointerCount,
                                 (byte)(indexBit % 8));
    }


    public final PointerBuilder getPointerElement(int index) {
        return new PointerBuilder(
            this.segment,
            (this.ptr + (index * this.step / Constants.BITS_PER_BYTE)) / Constants.BYTES_PER_WORD);
    }

}
