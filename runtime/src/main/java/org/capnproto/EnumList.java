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

public class EnumList {
    static <T> T clampOrdinal(T values[], short ordinal) {
        int index = ordinal;
        if (ordinal < 0 || ordinal >= values.length) {
            index = values.length - 1;
        }
        return values[index];
    }

    public static final class Factory<T extends java.lang.Enum> extends ListFactory<Builder<T>, Reader<T>>{
        public final T values[];

        public Factory(T values[]) {
            super(ElementSize.TWO_BYTES);
            this.values = values;
        }
        public final Reader<T> constructReader(SegmentReader segment,
                                               int ptr,
                                               int elementCount, int step,
                                               int structDataSize, short structPointerCount,
                                               int nestingLimit) {
            return new Reader<T>(values,
                                 segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
        }

        public final Builder<T> constructBuilder(SegmentBuilder segment,
                                                 int ptr,
                                                 int elementCount, int step,
                                                 int structDataSize, short structPointerCount) {
            return new Builder<T> (values, segment, ptr, elementCount, step, structDataSize, structPointerCount);
        }
    }

    public static final class Reader<T extends java.lang.Enum> extends ListReader {
        public final T values[];

        public Reader(T values[],
                      SegmentReader segment,
                      int ptr,
                      int elementCount, int step,
                      int structDataSize, short structPointerCount,
                      int nestingLimit) {
            super(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            this.values = values;
        }

        public T get(int index) {
            return clampOrdinal(this.values, _getShortElement(index));
        }
    }

    public static final class Builder<T extends java.lang.Enum> extends ListBuilder {
        public final T values[];

        public Builder(T values[],
                       SegmentBuilder segment,
                       int ptr,
                       int elementCount, int step,
                       int structDataSize, short structPointerCount) {
            super(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            this.values = values;
        }

        public T get(int index) {
            return clampOrdinal(this.values, _getShortElement(index));
        }

        public void set(int index, T value) {
            _setShortElement(index, (short)value.ordinal());
        }

        public final Reader<T> asReader() {
            return new Reader(this.values,
                              this.segment, this.ptr, this.elementCount, this.step,
                              this.structDataSize, this.structPointerCount,
                              java.lang.Integer.MAX_VALUE);
        }
    }
}
