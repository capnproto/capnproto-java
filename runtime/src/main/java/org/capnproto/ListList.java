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

public final class ListList {
    public static final class Factory<ElementBuilder, ElementReader extends ListReader>
        extends ListFactory<Builder<ElementBuilder>, Reader<ElementReader>> {

        public final ListFactory<ElementBuilder, ElementReader> factory;

        public Factory(ListFactory<ElementBuilder, ElementReader> factory) {
            super(ElementSize.POINTER);
            this.factory = factory;
        }

        public final Reader<ElementReader> constructReader(SegmentReader segment,
                                                             int ptr,
                                                             int elementCount, int step,
                                                             int structDataSize, short structPointerCount,
                                                             int nestingLimit) {
            return new Reader<ElementReader>(factory, segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
        }

        public final Builder<ElementBuilder> constructBuilder(SegmentBuilder segment,
                                                              int ptr,
                                                              int elementCount, int step,
                                                              int structDataSize, short structPointerCount) {
            return new Builder<ElementBuilder>(factory, segment, ptr, elementCount, step, structDataSize, structPointerCount);
        }
    }

    public static final class Reader<T> extends ListReader {
        private final FromPointerReader<T> factory;

        public Reader(FromPointerReader<T> factory,
                      SegmentReader segment,
                      int ptr,
                      int elementCount, int step,
                      int structDataSize, short structPointerCount,
                      int nestingLimit) {
            super(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            this.factory = factory;
        }

        public T get(int index) {
            return _getPointerElement(this.factory, index);
        }
    }

    public static final class Builder<T> extends ListBuilder {
        private final ListFactory<T, ?> factory;

        public Builder(ListFactory<T, ?> factory,
                       SegmentBuilder segment, int ptr,
                       int elementCount, int step,
                       int structDataSize, short structPointerCount){
            super(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            this.factory = factory;
        }

        public final T init(int index, int size) {
            return _initPointerElement(this.factory, index, size);
        }

        public final T get(int index) {
            return _getPointerElement(this.factory, index);
        }

        // TODO: rework generics so that we don't need this factory parameter
        public final <U extends ListReader> Reader<U> asReader(ListFactory<T, U> factor) {
            return new Reader(factor,
                              this.segment, this.ptr, this.elementCount, this.step,
                              this.structDataSize, this.structPointerCount,
                              java.lang.Integer.MAX_VALUE);
        }
    }
}
