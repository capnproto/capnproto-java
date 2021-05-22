// Copyright (c) 2013-2014 Sandstorm Development Group, Inc. and contributors
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
    private Allocator allocator;

    public BuilderArena(int firstSegmentSizeWords, AllocationStrategy allocationStrategy) {
        this.segments = new ArrayList<SegmentBuilder>();
        {
            DefaultAllocator allocator = new DefaultAllocator(allocationStrategy);
            allocator.setNextAllocationSizeBytes(firstSegmentSizeWords * Constants.BYTES_PER_WORD);
            this.allocator = allocator;
        }
    }

    public BuilderArena(Allocator allocator) {
        this.segments = new ArrayList<SegmentBuilder>();
        this.allocator = allocator;
    }

    /**
     * Constructs a BuilderArena from a ReaderArena and uses the size of the largest segment as next allocation size.
     *
     * This is deemed unsafe and should only be used by {@link UnsafeOperations }
     *
     * @param arena The arena containing the Capnp data.
     */
    BuilderArena(ReaderArena arena) {
        this.segments = new ArrayList<SegmentBuilder>();
        int largestSegment = SUGGESTED_FIRST_SEGMENT_WORDS*Constants.BYTES_PER_WORD;
        for (SegmentReader segment : arena.segments) {
            segments.add(new SegmentBuilder(segment.buffer, this));
            // find the biggest segment for the allocation strategy
            largestSegment = Math.max(largestSegment, segment.buffer.capacity());
        }
        DefaultAllocator defaultAllocator = new DefaultAllocator(SUGGESTED_ALLOCATION_STRATEGY);
        // use biggest segment as next size
        defaultAllocator.setNextAllocationSizeBytes(largestSegment);
        this.allocator = defaultAllocator;
    }

    public BuilderArena(Allocator allocator, ByteBuffer firstSegment) {
        this.segments = new ArrayList<SegmentBuilder>();
        SegmentBuilder newSegment = new SegmentBuilder(
            firstSegment,
            this);
        newSegment.buffer.order(ByteOrder.LITTLE_ENDIAN);
        newSegment.id = 0;
        this.segments.add(newSegment);

        this.allocator = allocator;
    }

    @Override
    public final SegmentReader tryGetSegment(int id) {
        return this.segments.get(id);
    }
    public final SegmentBuilder getSegment(int id) {
        return this.segments.get(id);
    }

    @Override
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

    /**
     *  Allocates `amount` words in an existing segment or, if no suitable segment
     *  exists, in a new segment.
     */
    public AllocateResult allocate(int amount) {
        int len = this.segments.size();

        // we allocate the first segment in the constructor.
        if (len > 0) {
            int result = this.segments.get(len - 1).allocate(amount);
            if (result != SegmentBuilder.FAILED_ALLOCATION) {
                return new AllocateResult(this.segments.get(len - 1), result);
            }
        }
        if (amount >= 1 << 28) {
            // Computing `amount * Constants.BYTES_PER_WORD` would overflow.
            throw new RuntimeException("Too many words to allocate: " + amount);
        }
        SegmentBuilder newSegment = new SegmentBuilder(
            this.allocator.allocateSegment(amount * Constants.BYTES_PER_WORD),
            this);

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
            slice.order(ByteOrder.LITTLE_ENDIAN);
            result[ii] = slice;
        }
        return result;
    }
}
