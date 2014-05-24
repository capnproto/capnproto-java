package org.capnproto;

public interface Arena {
    public SegmentReader tryGetSegment(int id);
}
