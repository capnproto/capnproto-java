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

public class StructBuilder {
    public interface Factory<T> {
        T constructBuilder(GenericSegmentBuilder segment, int data, int pointers, int dataSize,
                short pointerCount);
        StructSize structSize();
    }

    protected final GenericSegmentBuilder segment;
    protected final int data; // byte offset to data section
    protected final int pointers; // word offset of pointer section
    protected final int dataSize; // in bits
    protected final short pointerCount;

    public StructBuilder(GenericSegmentBuilder segment, int data,
            int pointers, int dataSize, short pointerCount) {
        this.segment = segment;
        this.data = data;
        this.pointers = pointers;
        this.dataSize = dataSize;
        this.pointerCount = pointerCount;
    }

    protected final boolean _getBooleanField(int offset) {
        int bitOffset = offset;
        int position = this.data + (bitOffset / 8);
        return (this.segment.getBuffer().get(position) & (1 << (bitOffset % 8))) != 0;
    }

    protected final boolean _getBooleanField(int offset, boolean mask) {
        return this._getBooleanField(offset) ^ mask;
    }

    protected final void _setBooleanField(int offset, boolean value) {
        int bitOffset = offset;
        byte bitnum = (byte) (bitOffset % 8);
        int position = this.data + (bitOffset / 8);
        byte oldValue = this.segment.getBuffer().get(position);
        this.segment.getBuffer().put(position,
                (byte) ((oldValue & (~(1 << bitnum))) | ((value ? 1 : 0) << bitnum)));
    }

    protected final void _setBooleanField(int offset, boolean value, boolean mask) {
        this._setBooleanField(offset, value ^ mask);
    }

    protected final byte _getByteField(int offset) {
        return this.segment.getBuffer().get(this.data + offset);
    }

    protected final byte _getByteField(int offset, byte mask) {
        return (byte) (this._getByteField(offset) ^ mask);
    }

    protected final void _setByteField(int offset, byte value) {
        this.segment.getBuffer().put(this.data + offset, value);
    }

    protected final void _setByteField(int offset, byte value, byte mask) {
        this._setByteField(offset, (byte) (value ^ mask));
    }

    protected final short _getShortField(int offset) {
        return this.segment.getBuffer().getShort(this.data + offset * 2);
    }

    protected final short _getShortField(int offset, short mask) {
        return (short) (this._getShortField(offset) ^ mask);
    }

    protected final void _setShortField(int offset, short value) {
        this.segment.getBuffer().putShort(this.data + offset * 2, value);
    }

    protected final void _setShortField(int offset, short value, short mask) {
        this._setShortField(offset, (short) (value ^ mask));
    }

    protected final int _getIntField(int offset) {
        return this.segment.getBuffer().getInt(this.data + offset * 4);
    }

    protected final int _getIntField(int offset, int mask) {
        return this._getIntField(offset) ^ mask;
    }

    protected final void _setIntField(int offset, int value) {
        this.segment.getBuffer().putInt(this.data + offset * 4, value);
    }

    protected final void _setIntField(int offset, int value, int mask) {
        this._setIntField(offset, value ^ mask);
    }

    protected final long _getLongField(int offset) {
        return this.segment.getBuffer().getLong(this.data + offset * 8);
    }

    protected final long _getLongField(int offset, long mask) {
        return this._getLongField(offset) ^ mask;
    }

    protected final void _setLongField(int offset, long value) {
        this.segment.getBuffer().putLong(this.data + offset * 8, value);
    }

    protected final void _setLongField(int offset, long value, long mask) {
        this._setLongField(offset, value ^ mask);
    }

    protected final float _getFloatField(int offset) {
        return this.segment.getBuffer().getFloat(this.data + offset * 4);
    }

    protected final float _getFloatField(int offset, int mask) {
        return Float.intBitsToFloat(
                this.segment.getBuffer().getInt(this.data + offset * 4) ^ mask);
    }

    protected final void _setFloatField(int offset, float value) {
        this.segment.getBuffer().putFloat(this.data + offset * 4, value);
    }

    protected final void _setFloatField(int offset, float value, int mask) {
        this.segment.getBuffer().putInt(this.data + offset * 4,
                Float.floatToIntBits(value) ^ mask);
    }

    protected final double _getDoubleField(int offset) {
        return this.segment.getBuffer().getDouble(this.data + offset * 8);
    }

    protected final double _getDoubleField(int offset, long mask) {
        return Double.longBitsToDouble(
                this.segment.getBuffer().getLong(this.data + offset * 8) ^ mask);
    }

    protected final void _setDoubleField(int offset, double value) {
        this.segment.getBuffer().putDouble(this.data + offset * 8, value);
    }

    protected final void _setDoubleField(int offset, double value, long mask) {
        this.segment.getBuffer().putLong(this.data + offset * 8,
                Double.doubleToLongBits(value) ^ mask);
    }

    protected final boolean _pointerFieldIsNull(int ptrIndex) {
        return ptrIndex >= this.pointerCount || this.segment.getBuffer().getLong((this.pointers + ptrIndex) * Constants.BYTES_PER_WORD) == 0;
    }

    protected final void _clearPointerField(int ptrIndex) {
        int pointer = this.pointers + ptrIndex;
        WireHelpers.zeroObject(this.segment, pointer);
        this.segment.getBuffer().putLong(pointer * 8, 0L);
    }

    protected final <T> T _getPointerField(FromPointerBuilder<T> factory, int index) {
        return factory.fromPointerBuilder(this.segment, this.pointers + index);
    }

    protected final <T> T _getPointerField(FromPointerBuilderRefDefault<T> factory, int index,
            SegmentDataContainer defaultSegment, int defaultOffset) {
        return factory.fromPointerBuilderRefDefault(this.segment, this.pointers + index, defaultSegment, defaultOffset);
    }

    protected final <T> T _getPointerField(FromPointerBuilderBlobDefault<T> factory, int index,
            java.nio.ByteBuffer defaultBuffer, int defaultOffset, int defaultSize) {
        return factory.fromPointerBuilderBlobDefault(this.segment, this.pointers + index, defaultBuffer, defaultOffset, defaultSize);
    }

    protected final <T> T _initPointerField(FromPointerBuilder<T> factory, int index, int elementCount) {
        return factory.initFromPointerBuilder(this.segment, this.pointers + index, elementCount);
    }

    protected final <Builder, Reader> void _setPointerField(SetPointerBuilder<Builder, Reader> factory, int index, Reader value) {
        factory.setPointerBuilder(this.segment, this.pointers + index, value);
    }
}
