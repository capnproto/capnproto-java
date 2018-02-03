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

public class PrimitiveList {
    public static class Void {
        public static final class Factory extends ListFactory<Builder, Reader> {
            Factory() {super (ElementSize.VOID); }

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

        public static final class Reader extends ListReader {
            public Reader(SegmentReader segment,
                          int ptr,
                          int elementCount, int step,
                          int structDataSize, short structPointerCount,
                          int nestingLimit) {
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            }

            public org.capnproto.Void get(int index) {
                return org.capnproto.Void.VOID;
            }
        }

        public static final class Builder extends ListBuilder {
            public Builder(SegmentBuilder segment, int ptr,
                           int elementCount, int step,
                           int structDataSize, short structPointerCount){
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            }


            public final Reader asReader() {
                return new Reader(this.segment, this.ptr, this.elementCount, this.step,
                                  this.structDataSize, this.structPointerCount,
                                  java.lang.Integer.MAX_VALUE);
            }
        }
    }

    public static class Boolean {
        public static final class Factory extends ListFactory<Builder, Reader> {
            Factory() {super (ElementSize.BIT); }
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

        public static final class Reader extends ListReader {
            public Reader(SegmentReader segment,
                          int ptr,
                          int elementCount, int step,
                          int structDataSize, short structPointerCount,
                          int nestingLimit) {
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            }

            public final boolean get(int index) {
                return _getBooleanElement(index);
            }
        }

        public static final class Builder extends ListBuilder {
            public Builder(SegmentBuilder segment, int ptr,
                           int elementCount, int step,
                           int structDataSize, short structPointerCount){
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            }

            public boolean get(int index) {
                return _getBooleanElement(index);
            }

            public void set(int index, boolean value) {
                _setBooleanElement(index, value);
            }

            public final Reader asReader() {
                return new Reader(this.segment, this.ptr, this.elementCount, this.step,
                                  this.structDataSize, this.structPointerCount,
                                  java.lang.Integer.MAX_VALUE);
            }
        }
    }

    public static class Byte {
        public static final class Factory extends ListFactory<Builder, Reader> {
            Factory() {super (ElementSize.BYTE); }
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

        public static final class Reader extends ListReader {
            public Reader(SegmentReader segment,
                          int ptr,
                          int elementCount, int step,
                          int structDataSize, short structPointerCount,
                          int nestingLimit) {
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            }

            public byte get(int index) {
                return _getByteElement(index);
            }
        }

        public static final class Builder extends ListBuilder {
            public Builder(SegmentBuilder segment, int ptr,
                           int elementCount, int step,
                           int structDataSize, short structPointerCount){
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            }

            public byte get(int index) {
                return _getByteElement(index);
            }

            public void set(int index, byte value) {
                _setByteElement(index, value);
            }

            public final Reader asReader() {
                return new Reader(this.segment, this.ptr, this.elementCount, this.step,
                                  this.structDataSize, this.structPointerCount,
                                  java.lang.Integer.MAX_VALUE);
            }
        }
    }

    public static class Short {
        public static final class Factory extends ListFactory<Builder, Reader> {
            Factory() {super (ElementSize.TWO_BYTES); }
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

        public static final class Reader extends ListReader {
            public Reader(SegmentReader segment,
                          int ptr,
                          int elementCount, int step,
                          int structDataSize, short structPointerCount,
                          int nestingLimit) {
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            }

            public short get(int index) {
                return _getShortElement(index);
            }
        }

        public static final class Builder extends ListBuilder {
            public Builder(SegmentBuilder segment, int ptr,
                           int elementCount, int step,
                           int structDataSize, short structPointerCount){
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            }

            public short get(int index) {
                return _getShortElement(index);
            }

            public void set(int index, short value) {
                _setShortElement(index, value);
            }

            public final Reader asReader() {
                return new Reader(this.segment, this.ptr, this.elementCount, this.step,
                                  this.structDataSize, this.structPointerCount,
                                  java.lang.Integer.MAX_VALUE);
            }
        }
    }

    public static class Int {
        public static final class Factory extends ListFactory<Builder, Reader> {
            Factory() {super (ElementSize.FOUR_BYTES); }
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

        public static final class Reader extends ListReader {
            public Reader(SegmentReader segment,
                          int ptr,
                          int elementCount, int step,
                          int structDataSize, short structPointerCount,
                          int nestingLimit) {
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            }

            public int get(int index) {
                return _getIntElement(index);
            }
        }

        public static final class Builder extends ListBuilder {
            public Builder(SegmentBuilder segment, int ptr,
                           int elementCount, int step,
                           int structDataSize, short structPointerCount){
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            }

            public int get(int index) {
                return _getIntElement(index);
            }

            public void set(int index, int value) {
                _setIntElement(index, value);
            }

            public final Reader asReader() {
                return new Reader(this.segment, this.ptr, this.elementCount, this.step,
                                  this.structDataSize, this.structPointerCount,
                                  java.lang.Integer.MAX_VALUE);
            }
        }
    }

    public static class Float {
        public static final class Factory extends ListFactory<Builder, Reader> {
            Factory() {super (ElementSize.FOUR_BYTES); }
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

        public static final class Reader extends ListReader {
            public Reader(SegmentReader segment,
                          int ptr,
                          int elementCount, int step,
                          int structDataSize, short structPointerCount,
                          int nestingLimit) {
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            }

            public float get(int index) {
                return _getFloatElement(index);
            }
        }

        public static final class Builder extends ListBuilder {
            public Builder(SegmentBuilder segment, int ptr,
                           int elementCount, int step,
                           int structDataSize, short structPointerCount){
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            }

            public float get(int index) {
                return _getFloatElement(index);
            }

            public void set(int index, float value) {
                _setFloatElement(index, value);
            }

            public final Reader asReader() {
                return new Reader(this.segment, this.ptr, this.elementCount, this.step,
                                  this.structDataSize, this.structPointerCount,
                                  java.lang.Integer.MAX_VALUE);
            }
        }
    }


    public static class Long {
        public static final class Factory extends ListFactory<Builder, Reader> {
            Factory() {super (ElementSize.EIGHT_BYTES); }
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

        public static final class Reader extends ListReader {
            public Reader(SegmentReader segment,
                          int ptr,
                          int elementCount, int step,
                          int structDataSize, short structPointerCount,
                          int nestingLimit) {
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            }

            public long get(int index) {
                return _getLongElement(index);
            }
        }

        public static final class Builder extends ListBuilder {
            public Builder(SegmentBuilder segment, int ptr,
                           int elementCount, int step,
                           int structDataSize, short structPointerCount){
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            }

            public long get(int index) {
                return _getLongElement(index);
            }

            public void set(int index, long value) {
                _setLongElement(index, value);
            }

            public final Reader asReader() {
                return new Reader(this.segment, this.ptr, this.elementCount, this.step,
                                  this.structDataSize, this.structPointerCount,
                                  java.lang.Integer.MAX_VALUE);
            }
        }
    }

    public static class Double {
        public static final class Factory extends ListFactory<Builder, Reader> {
            Factory() {super (ElementSize.EIGHT_BYTES); }
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

        public static final class Reader extends ListReader {
            public Reader(SegmentReader segment,
                          int ptr,
                          int elementCount, int step,
                          int structDataSize, short structPointerCount,
                          int nestingLimit) {
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            }

            public double get(int index) {
                return _getDoubleElement(index);
            }
        }

        public static final class Builder extends ListBuilder {
            public Builder(SegmentBuilder segment, int ptr,
                           int elementCount, int step,
                           int structDataSize, short structPointerCount){
                super(segment, ptr, elementCount, step, structDataSize, structPointerCount);
            }

            public double get(int index) {
                return _getDoubleElement(index);
            }

            public void set(int index, double value) {
                _setDoubleElement(index, value);
            }

            public final Reader asReader() {
                return new Reader(this.segment, this.ptr, this.elementCount, this.step,
                                  this.structDataSize, this.structPointerCount,
                                  java.lang.Integer.MAX_VALUE);
            }
        }
    }
}
