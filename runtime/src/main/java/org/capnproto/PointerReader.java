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
}
