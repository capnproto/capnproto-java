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

/**
 * Representation of the SegmentBuilder. This Builder is responsible to manage
 * one Segment for building a new Message.
 */
public interface GenericSegmentBuilder extends SegmentDataContainer {

    static final int FAILED_ALLOCATION = -1;

    /**
     * Puts the long value into the buffer at word index.
     *
     * @param index The word index.
     * @param value The value to add.
     */
    void put(int index, long value);

    /**
     * The current size of the Segment.
     *
     * @return size
     */
    int currentSize();

    /**
     * allocate more memory in this segment.
     *
     * @param words
     * @return the start position of the allocated words. -1 means there was not
     * enough space in the segment.
     */
    int allocate(int words);

    /**
     * Retrieve the AllocatingArena.
     *
     * @return the arena.
     */
    @Override
    AllocatingArena getArena();

    /**
     * Checks if the Segment is writable.
     *
     * @return {@code true}
     */
    boolean isWritable();

    /**
     * Retrieve the ID of this segment.
     *
     * @return the ID
     */
    int getId();

    /**
     * Sets the ID of the Segment.
     *
     * @param id the new ID.
     */
    void setId(int id);

    /**
     * Prepares the underlying ByteBuffer to be written.
     *
     * @return
     */
    ByteBuffer getSegmentForOutput();

}
