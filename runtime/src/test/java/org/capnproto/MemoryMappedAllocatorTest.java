// Copyright (c) 2018 Sandstorm Development Group, Inc. and contributors
// Licensed under the MIT License:
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package org.capnproto;

import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;


public class MemoryMappedAllocatorTest {
  @Test
  public void testInitializey() throws java.io.IOException {
    MemoryMappedAllocator allocator = new MemoryMappedAllocator("test");
  }
  
  @Test
  public void maxSegmentBytes() {
    MemoryMappedAllocator allocator = new MemoryMappedAllocator("test");
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

  @Test
  public void writeToBuffer() {
    MemoryMappedAllocator allocator = new MemoryMappedAllocator("test");
    allocator.maxSegmentBytes = (1 << 25) - 1;

    int allocationSize = 1 << 24;
    allocator.setNextAllocationSizeBytes(allocationSize);

    ByteBuffer buffer = allocator.allocateSegment(allocationSize);
    assertEquals(allocationSize, buffer.capacity());
    byte[] aBytes = new byte[Math.min(allocationSize, 8192)];
    Arrays.fill(aBytes, (byte) 'A');
    int remaining = allocationSize;

    while (remaining > 0) {
      int chunkSize = Math.min(aBytes.length, remaining);
      buffer.put(aBytes, 0, chunkSize); // updates the position on the buffer after the put
      remaining -= chunkSize;
    }
    allocator.close();
  }

  private void fillBuffer(ByteBuffer buffer, int allocationSize, byte filling) {
    byte[] aBytes = new byte[Math.min(allocationSize, 8192)];
    Arrays.fill(aBytes, filling);
    int remaining = allocationSize;

    while (remaining > 0) {
      int chunkSize = Math.min(aBytes.length, remaining);
      buffer.put(aBytes, 0, chunkSize); // updates the position on the buffer after the put
      remaining -= chunkSize;
    }
  }

  @Test
  public void checkBufferOverflow() {
    MemoryMappedAllocator allocator = new MemoryMappedAllocator("tmpFileBuffered");
    allocator.maxSegmentBytes = (1 << 25) - 1;

    int allocationSize = 1 << 24;
    allocator.setNextAllocationSizeBytes(allocationSize);

    ByteBuffer buffer = allocator.allocateSegment(allocationSize);
    assertEquals(allocationSize, buffer.capacity());
    fillBuffer(buffer, allocationSize, (byte) 'A');
    // Now test that writing 1 more byte over the allocated size,
    // it causes BufferOverflowException
    assertThrows(BufferOverflowException.class, () -> {
        fillBuffer(buffer, 1, (byte) 'A');
    });
  }

  

  @Test
  public void verifyBufferContent() {
    MemoryMappedAllocator allocator = new MemoryMappedAllocator("tmpFileBuffered");
    allocator.maxSegmentBytes = (1 << 25) - 1;

    int allocationSize = 1 << 24;
    allocator.setNextAllocationSizeBytes(allocationSize);

    ByteBuffer buffer = allocator.allocateSegment(allocationSize);
    assertEquals(allocationSize, buffer.capacity());
    fillBuffer(buffer, allocationSize, (byte) 'C');


    Map<Integer, FileChannel> channelMap = TestUtils.getChannelMap(allocator);
    assertNotNull(channelMap);
    
    for (FileChannel channel : channelMap.values()) {
        // Veryfi content
        try {
          TestUtils.assertAllBytes(channel, 'C');
        } catch (IOException e) {
          assertEquals(false, true);
        }
    }
  }
}
