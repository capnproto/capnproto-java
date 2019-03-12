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
import java.util.List;

public final class BuilderArena implements AllocatingArena {

    public enum AllocationStrategy {
        FIXED_SIZE,
        GROW_HEURISTICALLY
    }

    public static final int SUGGESTED_FIRST_SEGMENT_WORDS = 1_024;
    public static final AllocationStrategy SUGGESTED_ALLOCATION_STRATEGY
            = AllocationStrategy.GROW_HEURISTICALLY;

    public final ArrayList<GenericSegmentBuilder> segments;

    public int nextSize;
    public final AllocationStrategy allocationStrategy;

    public BuilderArena(int firstSegmentSizeWords, AllocationStrategy allocationStrategy) {
        this.segments = new ArrayList<>();
        this.nextSize = firstSegmentSizeWords;
        this.allocationStrategy = allocationStrategy;
        GenericSegmentBuilder segment0 = new SegmentBuilder(
                ByteBuffer.allocate(firstSegmentSizeWords * Constants.BYTES_PER_WORD), this);
        segment0.getBuffer().order(ByteOrder.LITTLE_ENDIAN);
        this.segments.add(segment0);
    }

    @Override
    public List<GenericSegmentBuilder> getSegments() {
        return segments;
    }

    @Override
    public final GenericSegmentBuilder tryGetSegment(int id) {
        return this.segments.get(id);
    }

    @Override
    public final void checkReadLimit(int numBytes) {
    }

    public static class AllocateResult {

        public final GenericSegmentBuilder segment;

        // offset to the beginning the of allocated memory
        public final int offset;

        public AllocateResult(GenericSegmentBuilder segment, int offset) {
            this.segment = segment;
            this.offset = offset;
        }
    }

    @Override
    public AllocateResult allocate(int amount) {

        int len = this.segments.size();
        // we allocate the first segment in the constructor.

        int result = this.segments.get(len - 1).allocate(amount);
        if (result != GenericSegmentBuilder.FAILED_ALLOCATION) {
            return new AllocateResult(this.segments.get(len - 1), result);
        }

        // allocate_owned_memory
        int size = Math.max(amount, this.nextSize);
        GenericSegmentBuilder newSegment = new SegmentBuilder(
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
        newSegment.getBuffer().order(ByteOrder.LITTLE_ENDIAN);
        newSegment.setId(len);
        this.segments.add(newSegment);

        return new AllocateResult(newSegment, newSegment.allocate(amount));
    }

    @Override
    public final ByteBuffer[] getSegmentsForOutput() {
        ByteBuffer[] result = new ByteBuffer[this.segments.size()];
        for (int ii = 0; ii < this.segments.size(); ++ii) {
            GenericSegmentBuilder segment = segments.get(ii);
            result[ii] = segment.getSegmentForOutput();
        }
        return result;
    }
}
