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

public final class AnyList {

    public static final class Factory extends ListFactory<Builder, Reader> {
        Factory() {
            super(ElementSize.VOID);
        }

        public final Reader asReader(Builder builder) {
            return builder.asReader();
        }

        @Override
        public Builder constructBuilder(SegmentBuilder segment, int ptr, int elementCount, int step, int structDataSize, short structPointerCount) {
            return new Builder(segment, ptr, elementCount, step, structDataSize, structPointerCount);
        }

        @Override
        public Reader constructReader(SegmentReader segment, int ptr, int elementCount, int step, int structDataSize, short structPointerCount, int nestingLimit) {
            return new Reader(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
        }
    }

    public static final Factory factory = new Factory();

    public static final ListList.Factory<Builder,Reader> listFactory = new ListList.Factory<>(factory);

    public static final class Builder extends ListBuilder {
        Builder(SegmentBuilder segment, int ptr, int elementCount, int step, int structDataSize, short structPointerCount) {
            super(segment, ptr, elementCount, step, structDataSize, structPointerCount);
        }

        public final Reader asReader() {
            return new Reader(this.segment, this.ptr, this.elementCount, this.step, this.structDataSize, this.structPointerCount, 0x7fffffff);
        }

        public final <T> T initAs(Factory<T> factory) {
            return factory.constructBuilder(this.segment, this.ptr, this.elementCount, this.step, this.structDataSize, this.structPointerCount);
        }

        public final <T> T setAs(Factory<T> factory) {
            return factory.constructBuilder(this.segment, this.ptr, this.elementCount, this.step, this.structDataSize, this.structPointerCount);
        }
    }

    public static final class Reader extends ListReader {
        Reader(SegmentReader segment, int ptr, int elementCount, int step, int structDataSize, short structPointerCount, int nestingLimit) {
            super(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
        }

        public final <T> T getAs(Factory<T> factory) {
            return factory.constructReader(this.segment, this.ptr, this.elementCount, this.step, this.structDataSize, this.structPointerCount, this.nestingLimit);
        }
    }
}
