package org.capnproto;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public final class BuilderArena implements Arena {

    public enum AllocationStrategy {
        FIXED_SIZE,
        GROW_HEURISTICALLY
    }

    public static final int SUGGESTED_FIRST_SEGMENT_WORDS = 1024;
    public static final AllocationStrategy SUGGESTED_ALLOCATION_STRATEGY =
        AllocationStrategy.GROW_HEURISTICALLY;

    public final ArrayList<SegmentBuilder> segments;

    public int nextSize;
    public final AllocationStrategy allocationStrategy;


    public BuilderArena(int firstSegmentSizeWords, AllocationStrategy allocationStrategy) {
        this.segments = new ArrayList<SegmentBuilder>();
        this.nextSize = firstSegmentSizeWords;
        this.allocationStrategy = allocationStrategy;
        SegmentBuilder segment0 = new SegmentBuilder(
            ByteBuffer.allocate(firstSegmentSizeWords * Constants.BYTES_PER_WORD), this);
        segment0.buffer.order(ByteOrder.LITTLE_ENDIAN);
        this.segments.add(segment0);
    }

    public final SegmentReader tryGetSegment(int id) {
        return this.segments.get(id);
    }
    public final SegmentBuilder getSegment(int id) {
        return this.segments.get(id);
    }

    public final void checkReadLimit(int numBytes) { }

    public static class AllocateResult {
        public final SegmentBuilder segment;

        // offset to the beginning the of allocated memory
        public final int offset;

        public AllocateResult(SegmentBuilder segment, int offset) {
            this.segment = segment;
            this.offset = offset;
        }
    }

    public AllocateResult allocate(int amount) {

        int len = this.segments.size();
        // we allocate the first segment in the constructor.

        int result = this.segments.get(len - 1).allocate(amount);
        if (result != SegmentBuilder.FAILED_ALLOCATION) {
            return new AllocateResult(this.segments.get(len - 1), result);
        }

        // allocate_owned_memory

        int size = Math.max(amount, this.nextSize);
        SegmentBuilder newSegment = new SegmentBuilder(
            ByteBuffer.allocate(size * Constants.BYTES_PER_WORD),
            this);

        switch (this.allocationStrategy) {
        case GROW_HEURISTICALLY:
            this.nextSize += size;
            break;
        default:
            break;
        }

        // --------

        newSegment.buffer.order(ByteOrder.LITTLE_ENDIAN);
        newSegment.id = len;
        this.segments.add(newSegment);

        return new AllocateResult(newSegment, newSegment.allocate(amount));
    }

    public final ByteBuffer[] getSegmentsForOutput() {
        ByteBuffer[] result = new ByteBuffer[this.segments.size()];
        for (int ii = 0; ii < this.segments.size(); ++ii) {
            SegmentBuilder segment = segments.get(ii);
            segment.buffer.rewind();
            ByteBuffer slice = segment.buffer.slice();
            slice.limit(segment.currentSize() * Constants.BYTES_PER_WORD);
            result[ii] = slice;
        }
        return result;
    }
}
