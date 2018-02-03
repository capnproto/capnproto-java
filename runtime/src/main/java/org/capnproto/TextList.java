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

public final class TextList {
    public static final class Factory extends ListFactory<Builder, Reader> {
        Factory() {super (ElementSize.POINTER); }
        public final Reader constructReader(SegmentReader segment,
                                            int ptr,
                                            int elementCount, int step,
                                            int structDataSize, short structPointerCount,
                                            int nestingLimit) {
            return new Reader(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
        }

        public final Builder constructBuilder(SegmentBuilder segment,
                                                int ptr,
                                                int elementCount, int step,
                                                int structDataSize, short structPointerCount) {
            return new Builder(segment, ptr, elementCount, step, structDataSize, structPointerCount);
        }
    }
    public static final Factory factory = new Factory();

    public static final class Reader extends ListReader implements Iterable<Text.Reader> {
        public Reader(SegmentReader segment,
                      int ptr,
                      int elementCount, int step,
                      int structDataSize, short structPointerCount,
                      int nestingLimit) {
            super(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
        }

        public Text.Reader get(int index) {
            return _getPointerElement(Text.factory, index);
        }

        public final class Iterator implements java.util.Iterator<Text.Reader> {
            public Reader list;
            public int idx = 0;
            public Iterator(Reader list) {
                this.list = list;
            }

            public Text.Reader next() {
                return this.list._getPointerElement(Text.factory, idx++);
            }
            public boolean hasNext() {
                return idx < list.size();
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        }

        public java.util.Iterator<Text.Reader> iterator() {
            return new Iterator(this);
        }

    }

    public static final class Builder extends ListBuilder implements Iterable<Text.Builder> {
        public Builder(SegmentBuilder segment, int ptr,
                       int elementCount, int step,
                       int structDataSize, short structPointerCount){
            super(segment, ptr, elementCount, step, structDataSize, structPointerCount);
        }

        public final Text.Builder get(int index) {
            return _getPointerElement(Text.factory, index);
        }

        public final void set(int index, Text.Reader value) {
            _setPointerElement(Text.factory, index, value);
        }

        public final Reader asReader() {
            return new Reader(this.segment, this.ptr, this.elementCount, this.step,
                              this.structDataSize, this.structPointerCount,
                              java.lang.Integer.MAX_VALUE);
        }

        public final class Iterator implements java.util.Iterator<Text.Builder> {
            public Builder list;
            public int idx = 0;
            public Iterator(Builder list) {
                this.list = list;
            }

            public Text.Builder next() {
                return this.list._getPointerElement(Text.factory, idx++);
            }
            public boolean hasNext() {
                return this.idx < this.list.size();
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        }

        public java.util.Iterator<Text.Builder> iterator() {
            return new Iterator(this);
        }
    }
}
