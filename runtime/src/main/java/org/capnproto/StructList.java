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

public final class StructList {
    public static final class Factory<ElementBuilder extends StructBuilder, ElementReader extends StructReader>
        extends ListFactory<Builder<ElementBuilder>, Reader<ElementReader>> {

        public final StructFactory<ElementBuilder, ElementReader> factory;

        public Factory(StructFactory<ElementBuilder, ElementReader> factory) {
            super(ElementSize.INLINE_COMPOSITE);
            this.factory = factory;
        }

        public final Reader<ElementReader> constructReader(SegmentReader segment,
                                                           int ptr,
                                                           int elementCount, int step,
                                                           int structDataSize, short structPointerCount,
                                                           int nestingLimit) {
            return new Reader<ElementReader>(factory,
                                             segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
        }

        public final Builder<ElementBuilder> constructBuilder(SegmentBuilder segment,
                                                              int ptr,
                                                              int elementCount, int step,
                                                              int structDataSize, short structPointerCount) {
            return new Builder<ElementBuilder> (factory, segment, ptr, elementCount, step, structDataSize, structPointerCount);
        }

        public final Builder<ElementBuilder> fromPointerBuilderRefDefault(SegmentBuilder segment, int pointer,
                                                                          SegmentReader defaultSegment, int defaultOffset) {
            return WireHelpers.getWritableStructListPointer(this,
                                                            pointer,
                                                            segment,
                                                            factory.structSize(),
                                                            defaultSegment,
                                                            defaultOffset);
        }

        @Override
        public final Builder<ElementBuilder> fromPointerBuilder(SegmentBuilder segment, int pointer) {
                     return WireHelpers.getWritableStructListPointer(this,
                                                                     pointer,
                                                                     segment,
                                                                     factory.structSize(),
                                                                     null, 0);
        }

        @Override
        public final Builder<ElementBuilder> initFromPointerBuilder(SegmentBuilder segment, int pointer,
                                                                    int elementCount) {
            return WireHelpers.initStructListPointer(this, pointer, segment, elementCount, factory.structSize());
        }
    }

    public static final class Reader<T> extends ListReader implements Iterable<T> {
        public final StructReader.Factory<T> factory;

        public Reader(StructReader.Factory<T> factory,
                      SegmentReader segment,
                      int ptr,
                      int elementCount, int step,
                      int structDataSize, short structPointerCount,
                      int nestingLimit) {
            super(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            this.factory = factory;
        }

        public T get(int index) {
            return _getStructElement(factory, index);
        }


        public final class Iterator implements java.util.Iterator<T> {
            public Reader<T> list;
            public int idx = 0;
            public Iterator(Reader<T> list) {
                this.list = list;
            }

            public T next() {
                return list._getStructElement(factory, idx++);
            }
            public boolean hasNext() {
                return idx < list.size();
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        }

        public java.util.Iterator<T> iterator() {
            return new Iterator(this);
        }
    }

    public static final class Builder<T> extends ListBuilder implements Iterable<T> {
        public final StructBuilder.Factory<T> factory;

        public Builder(StructBuilder.Factory<T> factory,
                       SegmentBuilder segment, int ptr,
                       int elementCount, int step,
                       int structDataSize, short structPointerCount){
            super(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            this.factory = factory;
        }

        public final T get(int index) {
            return _getStructElement(factory, index);
        }

        // TODO: rework generics so that we don't need this factory parameter
        public final <U extends StructReader> Reader<U> asReader(StructFactory<T, U> factory) {
            return new Reader(factory,
                              this.segment, this.ptr, this.elementCount, this.step,
                              this.structDataSize, this.structPointerCount,
                              java.lang.Integer.MAX_VALUE);
        }

        public final class Iterator implements java.util.Iterator<T> {
            public Builder<T> list;
            public int idx = 0;
            public Iterator(Builder<T> list) {
                this.list = list;
            }

            public T next() {
                return list._getStructElement(factory, idx++);
            }
            public boolean hasNext() {
                return idx < list.size();
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        }

        public java.util.Iterator<T> iterator() {
            return new Iterator(this);
        }
    }
}
