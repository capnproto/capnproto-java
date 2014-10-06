package org.capnproto;

public final class PointerReader {
    final SegmentReader segment;
    final int pointer; // word offset
    final int nestingLimit;

    public PointerReader() {
        this.segment = SegmentReader.EMPTY;
        this.pointer = 0;
        this.nestingLimit = 0x7fffffff;
    }

    public PointerReader(SegmentReader segment, int pointer, int nestingLimit) {
        this.segment = segment;
        this.pointer = pointer;
        this.nestingLimit = nestingLimit;
    }

    public static PointerReader getRoot(SegmentReader segment,
                                        int location,
                                        int nestingLimit) {
        // TODO bounds check
        return new PointerReader(segment, location, nestingLimit);
    }

    public boolean isNull() {
        return this.segment.buffer.getLong(this.pointer) == 0;
    }

    public StructReader getStruct() {
        return WireHelpers.readStructPointer(this.segment,
                                             this.pointer,
                                             null, 0,
                                             this.nestingLimit);
    }

    public StructReader getStruct(SegmentReader defaultSegment, int defaultOffset) {
        return WireHelpers.readStructPointer(this.segment,
                                             this.pointer,
                                             defaultSegment, defaultOffset,
                                             this.nestingLimit);
    }

    public ListReader getList(byte expectedElementSize) {
        return WireHelpers.readListPointer(this.segment,
                                           this.pointer,
                                           expectedElementSize,
                                           this.nestingLimit);
    }

    public Text.Reader getText() {
        return WireHelpers.readTextPointer(this.segment, this.pointer, null, 0, 0);
    }

    public Text.Reader getText(java.nio.ByteBuffer defaultBuffer, int defaultOffset, int defaultSize) {
        return WireHelpers.readTextPointer(this.segment, this.pointer, defaultBuffer, defaultOffset, defaultSize);
    }

    public Data.Reader getData() {
        return WireHelpers.readDataPointer(this.segment, this.pointer, null, 0, 0);
    }

    public Data.Reader getData(java.nio.ByteBuffer defaultBuffer, int defaultOffset, int defaultSize) {
        return WireHelpers.readDataPointer(this.segment, this.pointer, defaultBuffer, defaultOffset, defaultSize);
    }
}
