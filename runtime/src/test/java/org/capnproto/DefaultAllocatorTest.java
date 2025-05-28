package org.capnproto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class DefaultAllocatorTest {
    @Test
    public void maxSegmentBytes() {
      DefaultAllocator allocator = new DefaultAllocator();
      assertEquals(Allocator.AllocationStrategy.GROW_HEURISTICALLY, allocator.allocationStrategy);
      allocator.maxSegmentBytes = (1 << 25) - 1;

      int allocationSize = 1 << 24;
      allocator.setNextAllocationSizeBytes(allocationSize);

      assertEquals(allocationSize,
                          allocator.allocateSegment(allocationSize).capacity());

      assertEquals(allocator.maxSegmentBytes,
                          allocator.allocateSegment(allocationSize).capacity());

      assertEquals(allocator.maxSegmentBytes,
                          allocator.allocateSegment(allocationSize).capacity());
    }
}
