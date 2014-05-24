package org.capnproto;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Vector;

public final class BuilderArena implements Arena {
    public final Vector<SegmentBuilder> segments;

    public BuilderArena() {
        this.segments = new Vector<SegmentBuilder>();
        SegmentBuilder segment0 = new SegmentBuilder(ByteBuffer.allocate(1024 * 8));
        segment0.buffer.mark();
        this.segments.add(segment0);
    }

    public SegmentReader tryGetSegment(int id) {
        throw new Error("unimplemented");
    }
    public SegmentBuilder getSegment(int id) {
        throw new Error("unimplemented");
    }

    public static class AllocateResult {
        public final SegmentBuilder segment;
        public final int offset;
        public AllocateResult(SegmentBuilder segment, int offset) {
            this.segment = segment;
            this.offset = offset;
        }
    }

    public AllocateResult allocate(int amount) {
        throw new Error("unimplemented");
    }

    public final Vector<ByteBuffer> getSegmentsForOutput() {
        throw new Error();
    }
}
