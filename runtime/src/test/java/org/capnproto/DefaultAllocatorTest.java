package org.capnproto;

import org.junit.Assert;
import org.junit.Test;

public class DefaultAllocatorTest {
    @Test
    public void maxSegmentBytes() {
      DefaultAllocator allocator = new DefaultAllocator();
      Assert.assertEquals(allocator.allocationStrategy,
                          BuilderArena.AllocationStrategy.GROW_HEURISTICALLY);
      allocator.maxSegmentBytes = (1 << 25) - 1;

      int allocationSize = 1 << 24;
      allocator.setNextAllocationSizeBytes(allocationSize);

      Assert.assertEquals(allocationSize,
                          allocator.allocateSegment(allocationSize).capacity());

      Assert.assertEquals(allocator.maxSegmentBytes,
                          allocator.allocateSegment(allocationSize).capacity());

      Assert.assertEquals(allocator.maxSegmentBytes,
                          allocator.allocateSegment(allocationSize).capacity());
    }
}
