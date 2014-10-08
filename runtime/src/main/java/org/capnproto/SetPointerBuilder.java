package org.capnproto;

public interface SetPointerBuilder<Reader> {
    void setPointerBuilder(SegmentBuilder segment, int pointer, Reader value);
}
