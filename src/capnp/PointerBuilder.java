package org.capnproto;

public final class PointerBuilder {
    public SegmentBuilder segment;
    public int pointer; // word offset

    public PointerBuilder(SegmentBuilder segment, int pointer) {
        this.segment = segment;
        this.pointer = pointer;
    }
}
