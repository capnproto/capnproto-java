package org.capnproto;

public final class ListBuilder {
    SegmentBuilder segment;
    int ptr; // byte offset to front of list
    int elementCount;
    int step; // in bits
    int structDataSize; // in bits
    short structPointerCount;

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



}
