package org.capnproto;

public class StructBuilder {
    public interface Factory<T> {
        T constructBuilder(SegmentBuilder segment, int data, int pointers, int dataSize,
                                    short pointerCount, byte bit0Offset);
        StructSize structSize();
    }

    protected final SegmentBuilder segment;
    protected final int data; // byte offset to data section
    protected final int pointers; // word offset of pointer section
    protected final int dataSize; // in bits
    protected final short pointerCount;
    protected final byte bit0Offset;

    public StructBuilder(SegmentBuilder segment, int data,
                         int pointers, int dataSize, short pointerCount,
                         byte bit0Offset) {
        this.segment = segment;
        this.data = data;
        this.pointers = pointers;
        this.dataSize = dataSize;
        this.pointerCount = pointerCount;
        this.bit0Offset = bit0Offset;
    }

    protected final boolean _getBooleanField(int offset) {
        int bitOffset = (offset == 0 ? this.bit0Offset : offset);
        int position = this.data + (bitOffset / 8);
        return (this.segment.buffer.get(position) & (1 << (bitOffset % 8))) != 0;
    }

    protected final boolean _getBooleanField(int offset, boolean mask) {
        return this._getBooleanField(offset) ^ mask;
    }

    protected final void _setBooleanField(int offset, boolean value) {
        int bitOffset = offset;
        if (offset == 0) { bitOffset = this.bit0Offset; }
        byte bitnum = (byte)(bitOffset % 8);
        int position = this.data + (bitOffset / 8);
        byte oldValue = this.segment.buffer.get(position);
        this.segment.buffer.put(position,
                                (byte)((oldValue & (~(1 << bitnum))) | (( value ? 1 : 0) << bitnum)));
    }

    protected final void _setBooleanField(int offset, boolean value, boolean mask) {
        this._setBooleanField(offset, value ^ mask);
    }

    protected final byte _getByteField(int offset) {
        return this.segment.buffer.get(this.data + offset);
    }

    protected final byte _getByteField(int offset, byte mask) {
        return (byte)(this._getByteField(offset) ^ mask);
    }

    protected final void _setByteField(int offset, byte value) {
        this.segment.buffer.put(this.data + offset, value);
    }

    protected final void _setByteField(int offset, byte value, byte mask) {
        this._setByteField(offset, (byte) (value ^ mask));
    }

    protected final short _getShortField(int offset) {
        return this.segment.buffer.getShort(this.data + offset * 2);
    }

    protected final short _getShortField(int offset, short mask) {
        return (short)(this._getShortField(offset) ^ mask);
    }

    protected final void _setShortField(int offset, short value) {
        this.segment.buffer.putShort(this.data + offset * 2, value);
    }

    protected final void _setShortField(int offset, short value, short mask) {
        this._setShortField(offset, (short)(value ^ mask));
    }

    protected final int _getIntField(int offset) {
        return this.segment.buffer.getInt(this.data + offset * 4);
    }

    protected final int _getIntField(int offset, int mask) {
        return this._getIntField(offset) ^ mask;
    }

    protected final void _setIntField(int offset, int value) {
        this.segment.buffer.putInt(this.data + offset * 4, value);
    }

    protected final void _setIntField(int offset, int value, int mask) {
        this._setIntField(offset, value ^ mask);
    }

    protected final long _getLongField(int offset) {
        return this.segment.buffer.getLong(this.data + offset * 8);
    }

    protected final long _getLongField(int offset, long mask) {
        return this._getLongField(offset) ^ mask;
    }

    protected final void _setLongField(int offset, long value) {
        this.segment.buffer.putLong(this.data + offset * 8, value);
    }

    protected final void _setLongField(int offset, long value, long mask) {
        this._setLongField(offset, value ^ mask);
    }

    protected final float _getFloatField(int offset) {
        return this.segment.buffer.getFloat(this.data + offset * 4);
    }

    protected final float _getFloatField(int offset, int mask) {
        return Float.intBitsToFloat(
            this.segment.buffer.getInt(this.data + offset * 4) ^ mask);
    }

    protected final void _setFloatField(int offset, float value) {
        this.segment.buffer.putFloat(this.data + offset * 4, value);
    }

    protected final void _setFloatField(int offset, float value, int mask) {
        this.segment.buffer.putInt(this.data + offset * 4,
                                   Float.floatToIntBits(value) ^ mask);
    }

    protected final double _getDoubleField(int offset) {
        return this.segment.buffer.getDouble(this.data + offset * 8);
    }

    protected final double _getDoubleField(int offset, long mask) {
        return Double.longBitsToDouble(
            this.segment.buffer.getLong(this.data + offset * 8) ^ mask);
    }

    protected final void _setDoubleField(int offset, double value) {
        this.segment.buffer.putDouble(this.data + offset * 8, value);
    }

    protected final void _setDoubleField(int offset, double value, long mask) {
        this.segment.buffer.putLong(this.data + offset * 8,
                                    Double.doubleToLongBits(value) ^ mask);
    }

    protected final boolean _pointerFieldIsNull(int ptrIndex) {
        return this.segment.buffer.getLong((this.pointers + ptrIndex) * Constants.BYTES_PER_WORD) == 0;
    }

    protected final void _clearPointerField(int ptrIndex) {
        int pointer = this.pointers + ptrIndex;
        WireHelpers.zeroObject(this.segment, pointer);
        this.segment.buffer.putLong(pointer * 8, 0L);
    }

    protected final <T> T _getPointerField(FromPointerBuilderRefDefault<T> factory, int index,
                                           SegmentReader defaultSegment, int defaultOffset) {
        return factory.fromPointerBuilderRefDefault(this.segment, this.pointers + index, defaultSegment, defaultOffset);
    }

    protected final <T> T _getPointerField(FromPointerBuilderBlobDefault<T> factory, int index,
                                           java.nio.ByteBuffer defaultBuffer, int defaultOffset, int defaultSize) {
        return factory.fromPointerBuilderBlobDefault(this.segment, this.pointers + index, defaultBuffer, defaultOffset, defaultSize);
    }

    protected final <T> T _initPointerField(InitFromPointerBuilder<T> factory, int index) {
        return factory.initFromPointerBuilder(this.segment, this.pointers + index);
    }

    protected final <T> T _initSizedPointerField(InitSizedFromPointerBuilder<T> factory, int index, int elementCount) {
        return factory.initSizedFromPointerBuilder(this.segment, this.pointers + index, elementCount);
    }

    protected final <Reader> void _setPointerField(SetPointerBuilder<Reader> factory, int index, Reader value) {
        factory.setPointerBuilder(this.segment, this.pointers + index, value);
    }

}
