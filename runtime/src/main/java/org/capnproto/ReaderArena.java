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

import java.util.ArrayList;
import java.nio.ByteBuffer;

public final class ReaderArena implements Arena {

    // Current limit. -1 means no limit.
    public long limit;

    public final ArrayList<SegmentReader> segments;

    public ReaderArena(ByteBuffer[] segmentSlices, long traversalLimitInWords) {
        this.limit = traversalLimitInWords;
        this.segments = new ArrayList<>(segmentSlices.length);
        for(int ii = 0; ii < segmentSlices.length; ++ii) {
            this.segments.add(new SegmentReader(segmentSlices[ii], this));
        }
    }

    @Override
    public SegmentReader tryGetSegment(int id) {
        return segments.get(id);
    }

    @Override
    public final void checkReadLimit(int numWords) {
        if (limit == -1) {
            // No limit.
            return;
        } else if (numWords > limit) {
            throw new DecodeException("Read limit exceeded.");
        } else {
            limit -= numWords;
        }
    }
}
