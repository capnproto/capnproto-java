package org.capnproto;

import org.junit.Assert;
import org.junit.Test;

public class DefaultAllocatorTest {
    @Test
    public void checkNoOverflow() {
      DefaultAllocator allocator = new DefaultAllocator();
      Assert.assertEquals(allocator.allocationStrategy,
                          BuilderArena.AllocationStrategy.GROW_HEURISTICALLY);
      allocator.setNextAllocationSizeBytes(1 << 30);

      Assert.assertEquals(1 << 30,
                          allocator.allocateSegment(1 << 30).capacity());

      Assert.assertEquals(allocator.maxSegmentBytes,
                          allocator.allocateSegment(1 << 30).capacity());

      Assert.assertEquals(allocator.maxSegmentBytes,
                          allocator.allocateSegment(1 << 30).capacity());
    }
}
