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
            slice.order(ByteOrder.LITTLE_ENDIAN);
            result[ii] = slice;
        }
        return result;
    }
}
