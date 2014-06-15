package org.capnproto;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Vector;

public final class BuilderArena implements Arena {

    public enum AllocationStrategy {
        FIXED_SIZE,
        GROW_HEURISTICALLY
    }

    public static final int SUGGESTED_FIRST_SEGMENT_WORDS = 1024;
    public static final AllocationStrategy SUGGESTED_ALLOCATION_STRATEGY =
        AllocationStrategy.GROW_HEURISTICALLY;

    // Maybe this should be ArrayList?
    public final Vector<SegmentBuilder> segments;

    public int nextSize;
    public final AllocationStrategy allocationStrategy;


    public BuilderArena(int firstSegmentSizeWords, AllocationStrategy allocationStrategy) {
        this.segments = new Vector<SegmentBuilder>();
        this.nextSize = firstSegmentSizeWords;
        this.allocationStrategy = allocationStrategy;
        SegmentBuilder segment0 = new SegmentBuilder(
            ByteBuffer.allocate(firstSegmentSizeWords * Constants.BYTES_PER_WORD));
        segment0.buffer.mark();
        segment0.buffer.order(ByteOrder.LITTLE_ENDIAN);
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

    public final ByteBuffer[] getSegmentsForOutput() {
        ByteBuffer[] result = new ByteBuffer[this.segments.size()];
        for (int ii = 0; ii < this.segments.size(); ++ii) {
            SegmentBuilder segment = segments.get(ii);
            segment.buffer.reset();
            ByteBuffer slice = segment.buffer.slice();
            slice.limit(segment.currentSize() * 8);
            result[ii] = slice;
        }
        return result;
    }
}
