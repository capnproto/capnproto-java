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

import java.util.Objects;

public final class MessageBuilder {

    private final BuilderArena arena;

    public MessageBuilder() {
        this(new BuilderArena(BuilderArena.SUGGESTED_FIRST_SEGMENT_WORDS,
                BuilderArena.SUGGESTED_ALLOCATION_STRATEGY));
    }

    public MessageBuilder(int firstSegmentWords) {
        this(new BuilderArena(firstSegmentWords,
                BuilderArena.SUGGESTED_ALLOCATION_STRATEGY));
    }

    public MessageBuilder(int firstSegmentWords, BuilderArena.AllocationStrategy allocationStrategy) {
        this(new BuilderArena(firstSegmentWords, allocationStrategy));
    }

    /**
     * Constructs a new MessageBuilder from an Allocator.
     */
    public MessageBuilder(Allocator allocator) {
        this(new BuilderArena(allocator));
    }

    /**
     * Constructs a new MessageBuilder from a ReaderArena. Used to create a Builder on an already read Message.
     *
     * @param arena The Arena created by the Reader.
     */
    public MessageBuilder(ReaderArena arena) {
        this.arena = new BuilderArena(arena);
    }

    /**
     * Constructs a new MessageBuilder from an Allocator and a given first segment buffer.
     * This is useful for reusing the first segment buffer between messages, to avoid
     * repeated allocations.
     *
     * You MUST ensure that firstSegment contains only zeroes before calling this method.
     * If you are reusing firstSegment from another message, then it suffices to call
     * clearFirstSegment() on that message.
     */
    public MessageBuilder(Allocator allocator, java.nio.ByteBuffer firstSegment) {
        this(new BuilderArena(allocator, firstSegment));
    }

    /**
     * Like the previous constructor, but uses a DefaultAllocator.
     *
     * You MUST ensure that firstSegment contains only zeroes before calling this method.
     * If you are reusing firstSegment from another message, then it suffices to call
     * clearFirstSegment() on that message.
     */
    public MessageBuilder(java.nio.ByteBuffer firstSegment) {
        this(new BuilderArena(new DefaultAllocator(), firstSegment));
    }

    /**
     * Constructs a new MessageBuilder from an {@link BuilderArena}.
     *
     * @param arena The arena.
     */
    MessageBuilder(BuilderArena arena) {
        this.arena = Objects.requireNonNull(arena);
    }

    private AnyPointer.Builder getRootInternal() {
        if (this.arena.segments.isEmpty()) {
            this.arena.allocate(1);
        }
        SegmentBuilder rootSegment = this.arena.segments.get(0);
        if (rootSegment.currentSize() == 0) {
            int location = rootSegment.allocate(1);
            if (location == SegmentBuilder.FAILED_ALLOCATION) {
                throw new RuntimeException("could not allocate root pointer");
            }
            if (location != 0) {
                throw new RuntimeException("First allocated word of new segment was not at offset 0");
            }
            return new AnyPointer.Builder(rootSegment, location);
        } else {
            return new AnyPointer.Builder(rootSegment, 0);
        }
    }

    public <T> T getRoot(FromPointerBuilder<T> factory) {
        return this.getRootInternal().getAs(factory);
    }

    public <T, U> void setRoot(SetPointerBuilder<T, U> factory, U reader) {
        this.getRootInternal().setAs(factory, reader);
    }

    public <T> T initRoot(FromPointerBuilder<T> factory) {
        return this.getRootInternal().initAs(factory);
    }

    public final java.nio.ByteBuffer[] getSegmentsForOutput() {
        return this.arena.getSegmentsForOutput();
    }

    /**
     * Sets the first segment buffer to contain all zeros so that it can be reused in
     * another message. (See the MessageBuilder(Allocator, ByteBuffer) constructor above.)
     *
     * After calling this method, the message will be corrupted. Therefore, you need to make
     * sure to write the message (via getSegmentsForOutput()) before calling this.
     */
    public final void clearFirstSegment() {
        this.arena.segments.get(0).clear();
    }
}
