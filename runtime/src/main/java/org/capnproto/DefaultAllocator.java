package org.capnproto;

import java.nio.ByteBuffer;


public class DefaultAllocator implements Allocator {

    // (minimum) number of bytes in the next allocation
    private int nextSize = BuilderArena.SUGGESTED_FIRST_SEGMENT_WORDS;

    public enum ByteBufferAllocationStyle {
        REGULAR,
        DIRECT
    }
    public ByteBufferAllocationStyle allocationStyle = ByteBufferAllocationStyle.REGULAR;

    public AllocationStrategy allocationStrategy =
        AllocationStrategy.GROW_HEURISTICALLY;

    /**
       The largest number of bytes to try allocating when using `GROW_HEURISTICALLY`.

       Set this value smaller if you get the error:

           java.lang.OutOfMemoryError: Requested array size exceeds VM limit

       Experimentally, `Integer.MAX_VALUE - 2` seems to work on most systems.
    */
    public int maxSegmentBytes = Integer.MAX_VALUE - 2;

    public DefaultAllocator() {}

    public DefaultAllocator(AllocationStrategy allocationStrategy) {
        this.allocationStrategy = allocationStrategy;
    }

    public DefaultAllocator(ByteBufferAllocationStyle style) {
        this.allocationStyle = style;
    }

    public DefaultAllocator(AllocationStrategy allocationStrategy,
                            ByteBufferAllocationStyle style) {
        this.allocationStrategy = allocationStrategy;
        this.allocationStyle = style;
    }

    @Override
    public void setNextAllocationSizeBytes(int nextSize) {
        this.nextSize = nextSize;
    }

    @Override
    public java.nio.ByteBuffer allocateSegment(int minimumSize) {
        int size = Math.max(minimumSize, this.nextSize);
        ByteBuffer result = null;
        switch (allocationStyle) {
          case REGULAR:
            result = ByteBuffer.allocate(size);
            break;
          case DIRECT:
            result = ByteBuffer.allocateDirect(size);
        }

        switch (this.allocationStrategy) {
            case GROW_HEURISTICALLY:
                if (size < this.maxSegmentBytes - this.nextSize) {
                    this.nextSize += size;
                } else {
                    this.nextSize = maxSegmentBytes;
                }
                break;
            case FIXED_SIZE:
                break;
        }

        return result;
    }
}
