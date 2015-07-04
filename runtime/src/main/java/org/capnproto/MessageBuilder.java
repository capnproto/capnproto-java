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

public final class MessageBuilder {

    private final BuilderArena arena;

    public MessageBuilder() {
        this.arena = new BuilderArena(BuilderArena.SUGGESTED_FIRST_SEGMENT_WORDS,
                                      BuilderArena.SUGGESTED_ALLOCATION_STRATEGY);
    }

    public MessageBuilder(int firstSegmentWords) {
        this.arena = new BuilderArena(firstSegmentWords,
                                      BuilderArena.SUGGESTED_ALLOCATION_STRATEGY);
    }

    public MessageBuilder(int firstSegmentWords, BuilderArena.AllocationStrategy allocationStrategy) {
        this.arena = new BuilderArena(firstSegmentWords,
                                      allocationStrategy);
    }

    private AnyPointer.Builder getRootInternal() {
        SegmentBuilder rootSegment = this.arena.segments.get(0);
        if (rootSegment.currentSize() == 0) {
            int location = rootSegment.allocate(1);
            if (location == SegmentBuilder.FAILED_ALLOCATION) {
                throw new Error("could not allocate root pointer");
            }
            if (location != 0) {
                throw new Error("First allocated word of new segment was not at offset 0");
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
}
