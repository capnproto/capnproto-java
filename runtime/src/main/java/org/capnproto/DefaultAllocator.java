package org.capnproto;

import java.nio.ByteBuffer;

import org.capnproto.BuilderArena.AllocationStrategy;

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
                this.nextSize += size;
                break;
            case FIXED_SIZE:
                break;
        }

        this.nextSize += size;
        return result;
    }
}
