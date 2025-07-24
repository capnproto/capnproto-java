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

public class SegmentReader {

    public final ByteBuffer buffer;
    final Arena arena;

    public SegmentReader(ByteBuffer buffer, Arena arena) {
        this.buffer = buffer;
        this.arena = arena;
    }

    public static final SegmentReader EMPTY = new SegmentReader(ByteBuffer.allocate(8), null);

    public final long get(int index) {
        return buffer.getLong(index * Constants.BYTES_PER_WORD);
    }

    /**
     * Verify that the `size`-long (in words) range starting at word index
     * `start` is within bounds.
     */
    public final boolean isInBounds(int startInWords, int sizeInWords) {
        if (startInWords < 0 || sizeInWords < 0) return false;
        long startInBytes = (long) startInWords * Constants.BYTES_PER_WORD;
        long sizeInBytes = (long) sizeInWords * Constants.BYTES_PER_WORD;
        return startInBytes + sizeInBytes <= this.buffer.capacity();
    }
}
