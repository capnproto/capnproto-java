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

public class ListReader extends CapTableReader.ReaderContext {
    public interface Factory<T> {
        T constructReader(SegmentReader segment,
                          int ptr,
                          int elementCount, int step,
                          int structDataSize, short structPointerCount,
                          int nestingLimit);
        default T constructReader(SegmentReader segment, CapTableReader capTable, int ptr,
                                  int elementCount, int step,
                                  int structDataSize, short structPointerCount, int nestingLimit) {
            T result = constructReader(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
            if (result instanceof CapTableReader.ReaderContext) {
                ((CapTableReader.ReaderContext) result).capTable = capTable;
            }
            return result;
        }
    }

    final SegmentReader segment;
    final int ptr; // byte offset to front of list
    final int elementCount;
    final int step; // in bits
    final int structDataSize; // in bits
    final short structPointerCount;
    final int nestingLimit;

    public ListReader() {
        this.segment = null;
        this.ptr = 0;
        this.elementCount = 0;
        this.step = 0;
        this.structDataSize = 0;
        this.structPointerCount = 0;
        this.nestingLimit = 0x7fffffff;
        this.capTable = null;
    }

    public ListReader(SegmentReader segment, int ptr,
                      int elementCount, int step,
                      int structDataSize, short structPointerCount,
                      int nestingLimit) {
        this(segment, null, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
    }

    public ListReader(SegmentReader segment,
            CapTableReader capTable,
            int ptr,
            int elementCount, int step,
            int structDataSize, short structPointerCount,
            int nestingLimit) {
        this.segment = segment;
        this.ptr = ptr;
        this.elementCount = elementCount;
        this.step = step;
        this.structDataSize = structDataSize;
        this.structPointerCount = structPointerCount;
        this.nestingLimit = nestingLimit;
        this.capTable = capTable;
    }

    ListReader imbue(CapTableReader capTable) {
        return new ListReader(segment, capTable, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
    }

    public int size() {
        return this.elementCount;
    }

    protected boolean _getBooleanElement(int index) {
        long bindex = (long)index * this.step;
        byte b = this.segment.buffer.get(this.ptr + (int)(bindex / Constants.BITS_PER_BYTE));
        return (b & (1 << (bindex % 8))) != 0;
    }

    protected byte _getByteElement(int index) {
        return this.segment.buffer.get(this.ptr + (int)((long)index * this.step / Constants.BITS_PER_BYTE));
    }

    protected short _getShortElement(int index) {
        return this.segment.buffer.getShort(this.ptr + (int)((long)index * this.step / Constants.BITS_PER_BYTE));
    }

    protected int _getIntElement(int index) {
        return this.segment.buffer.getInt(this.ptr + (int)((long)index * this.step / Constants.BITS_PER_BYTE));
    }

    protected long _getLongElement(int index) {
        return this.segment.buffer.getLong(this.ptr + (int)((long)index * this.step / Constants.BITS_PER_BYTE));
    }

    protected float _getFloatElement(int index) {
        return this.segment.buffer.getFloat(this.ptr + (int)((long)index * this.step / Constants.BITS_PER_BYTE));
    }

    protected double _getDoubleElement(int index) {
        return this.segment.buffer.getDouble(this.ptr + (int)((long)index * this.step / Constants.BITS_PER_BYTE));
    }

    protected <T> T _getStructElement(StructReader.Factory<T> factory, int index) {
        // TODO check nesting limit

        long indexBit = (long)index * this.step;
        int structData = this.ptr + (int)(indexBit / Constants.BITS_PER_BYTE);
        int structPointers = structData + (this.structDataSize / Constants.BITS_PER_BYTE);

        return factory.constructReader(this.segment, this.capTable, structData, structPointers / 8, this.structDataSize,
                                       this.structPointerCount, this.nestingLimit - 1);
    }

    protected <T> T _getPointerElement(FromPointerReader<T> factory, int index) {
      return factory.fromPointerReader(
        this.segment,
        this.capTable,
        (this.ptr +
         (this.structDataSize / Constants.BITS_PER_BYTE) +
         (int)((long)index * this.step / Constants.BITS_PER_BYTE))
          / Constants.BYTES_PER_WORD,
        this.nestingLimit);
    }

    protected <T> T _getPointerElement(FromPointerReaderBlobDefault<T> factory, int index,
                                       java.nio.ByteBuffer defaultBuffer, int defaultOffset, int defaultSize) {
        return factory.fromPointerReaderBlobDefault(
            this.segment,
            (this.ptr + (int)((long)index * this.step / Constants.BITS_PER_BYTE)) / Constants.BYTES_PER_WORD,
            defaultBuffer,
            defaultOffset,
            defaultSize);
    }

}
