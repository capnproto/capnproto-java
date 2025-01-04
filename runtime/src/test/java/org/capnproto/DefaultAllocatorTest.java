package org.capnproto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DefaultAllocatorTest {
    @Test
    public void maxSegmentBytes() {
      DefaultAllocator allocator = new DefaultAllocator();
      Assertions.assertEquals(allocator.allocationStrategy,
                          BuilderArena.AllocationStrategy.GROW_HEURISTICALLY);
      allocator.maxSegmentBytes = (1 << 25) - 1;

      int allocationSize = 1 << 24;
      allocator.setNextAllocationSizeBytes(allocationSize);

      Assertions.assertEquals(allocationSize,
                          allocator.allocateSegment(allocationSize).capacity());

      Assertions.assertEquals(allocator.maxSegmentBytes,
                          allocator.allocateSegment(allocationSize).capacity());

      Assertions.assertEquals(allocator.maxSegmentBytes,
                          allocator.allocateSegment(allocationSize).capacity());
    }
}
