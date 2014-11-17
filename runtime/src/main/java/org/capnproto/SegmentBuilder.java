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

public final class SegmentBuilder extends SegmentReader {

    public static final int FAILED_ALLOCATION = -1;

    public int pos = 0; // in words
    public int id = 0;

    public SegmentBuilder(ByteBuffer buf, Arena arena) {
        super(buf, arena);
    }

    // the total number of words the buffer can hold
    private final int capacity() {
        this.buffer.rewind();
        return this.buffer.remaining() / 8;
    }

    // return how many words have already been allocated
    public final int currentSize() {
        return this.pos;
    }

    /*
       Allocate `amount` words.
     */
    public final int allocate(int amount) {
        assert amount >= 0 : "tried to allocate a negative number of words";

        if (amount > this.capacity() - this.currentSize()) {
            return FAILED_ALLOCATION; // no space left;
        } else {
            int result = this.pos;
            this.pos += amount;
            return result;
        }
    }

    public final BuilderArena getArena() {
        return (BuilderArena)this.arena;
    }

    public final boolean isWritable() {
        // TODO support external non-writable segments
        return true;
    }

    public final void put(int index, long value) {
        buffer.putLong(index * Constants.BYTES_PER_WORD, value);
    }
}
