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

public final class Data {
    public static final class Factory implements FromPointerReaderBlobDefault<Reader>,
                                      PointerFactory<Builder, Reader>,
                                      FromPointerBuilderBlobDefault<Builder>,
                                      SetPointerBuilder<Builder, Reader> {
        public final Reader fromPointerReaderBlobDefault(SegmentReader segment, int pointer, java.nio.ByteBuffer defaultBuffer,
                                                   int defaultOffset, int defaultSize) {
            return WireHelpers.readDataPointer(segment, pointer, defaultBuffer, defaultOffset, defaultSize);
        }
        public final Reader fromPointerReader(SegmentReader segment, int pointer, int nestingLimit) {
            return WireHelpers.readDataPointer(segment, pointer, null, 0, 0);
        }
        public final Builder fromPointerBuilderBlobDefault(SegmentBuilder segment, int pointer,
                                                     java.nio.ByteBuffer defaultBuffer, int defaultOffset, int defaultSize) {
            return WireHelpers.getWritableDataPointer(pointer,
                                                      segment,
                                                      defaultBuffer,
                                                      defaultOffset,
                                                      defaultSize);
        }
        public final Builder fromPointerBuilder(SegmentBuilder segment, int pointer) {
            return WireHelpers.getWritableDataPointer(pointer,
                                                      segment,
                                                      null, 0, 0);
        }

        public final Builder initFromPointerBuilder(SegmentBuilder segment, int pointer, int size) {
            return WireHelpers.initDataPointer(pointer, segment, size);
        }

        public final void setPointerBuilder(SegmentBuilder segment, int pointer, Reader value) {
            WireHelpers.setDataPointer(pointer, segment, value);
        }
    }
    public static final Factory factory = new Factory();

    public static final class Reader {
        public final ByteBuffer buffer;
        public final int offset; // in bytes
        public final int size; // in bytes

        public Reader() {
            this.buffer = ByteBuffer.allocate(0);
            this.offset = 0;
            this.size = 0;
        }

        public Reader(ByteBuffer buffer, int offset, int size) {
            this.buffer = buffer;
            this.offset = offset * 8;
            this.size = size;
        }

        public Reader(byte[] bytes) {
            this.buffer = ByteBuffer.wrap(bytes);
            this.offset = 0;
            this.size = bytes.length;
        }

        public final int size() {
            return this.size;
        }

        public ByteBuffer asByteBuffer() {
            ByteBuffer dup = this.buffer.asReadOnlyBuffer();
            dup.position(this.offset);
            ByteBuffer result = dup.slice();
            result.limit(this.size);
            return result;
        }

        public byte[] toArray() {
            ByteBuffer dup = this.buffer.duplicate();
            byte result[] = new byte[this.size];
            dup.position(this.offset);
            dup.get(result, 0, this.size);
            return result;
        }
    }

    public static final class Builder {
        public final ByteBuffer buffer;
        public final int offset; // in bytes
        public final int size; // in bytes

        public Builder() {
            this.buffer = ByteBuffer.allocate(0);
            this.offset = 0;
            this.size = 0;
        }

        public Builder(ByteBuffer buffer, int offset, int size) {
            this.buffer = buffer;
            this.offset = offset;
            this.size = size;
        }

        public ByteBuffer asByteBuffer() {
            ByteBuffer dup = this.buffer.duplicate();
            dup.position(this.offset);
            ByteBuffer result = dup.slice();
            result.limit(this.size);
            return result;
        }

        public byte[] toArray() {
            ByteBuffer dup = this.buffer.duplicate();
            byte result[] = new byte[this.size];
            dup.position(this.offset);
            dup.get(result, 0, this.size);
            return result;
        }
    }
}
