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

public final class SegmentBuilder implements GenericSegmentBuilder {

    public int pos = 0; // in words
    public int id = 0;
    public final ByteBuffer buffer;
    // store the AllocatingArena
    private final AllocatingArena arena;

    public SegmentBuilder(ByteBuffer buf, AllocatingArena arena) {
        this.buffer = buf;
        this.arena = arena;
    }

    // the total number of words the buffer can hold
    private final int capacity() {
        buffer.rewind();
        return buffer.remaining() / 8;
    }

    // return how many words have already been allocated
    @Override
    public final int currentSize() {
        return this.pos;
    }

    /*
       Allocate `amount` words.
     */
    @Override
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

    @Override
    public final AllocatingArena getArena() {
        return this.arena;
    }

    @Override
    public final boolean isWritable() {
        // TODO support external non-writable segments
        return true;
    }

    @Override
    public void put(int index, long value) {
        buffer.putLong(index * Constants.BYTES_PER_WORD, value);
    }

    @Override
    public ByteBuffer getBuffer() {
        return buffer;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public ByteBuffer getSegmentForOutput() {
        buffer.rewind();
        ByteBuffer slice = buffer.slice();
        slice.limit(currentSize() * Constants.BYTES_PER_WORD);
        slice.order(ByteOrder.LITTLE_ENDIAN);
        return slice;
    }

    @Override
    public void setId(int len) {
        this.id = len;
    }

    @Override
    public long get(int index) {
        return buffer.getLong(index * Constants.BYTES_PER_WORD);
    }
}
