package org.capnproto;

public class StructBuilder {
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

    public final boolean _getBooleanField(int offset) {
        int bitOffset = (offset == 0 ? this.bit0Offset : offset);
        int position = this.data + (bitOffset / 8);
        return (this.segment.buffer.get(position) & (1 << (bitOffset % 8))) != 0;
    }

    public final boolean getBooleanField(int offset, boolean mask) {
        return this._getBooleanField(offset) ^ mask;
    }

    public final void _setBooleanField(int offset, boolean value) {
        int bitOffset = offset;
        if (offset == 0) { bitOffset = this.bit0Offset; }
        byte bitnum = (byte)(bitOffset % 8);
        int position = this.data + (bitOffset / 8);
        byte oldValue = this.segment.buffer.get(position);
        this.segment.buffer.put(position,
                                (byte)((oldValue & (~(1 << bitnum))) | (( value ? 1 : 0) << bitnum)));
    }

    public final void _setBooleanField(int offset, boolean value, boolean mask) {
        this._setBooleanField(offset, value ^ mask);
    }

    public final byte _getByteField(int offset) {
        return this.segment.buffer.get(this.data + offset);
    }

    public final byte _getByteField(int offset, byte mask) {
        return (byte)(this._getByteField(offset) ^ mask);
    }

    public final void _setByteField(int offset, byte value) {
        this.segment.buffer.put(this.data + offset, value);
    }

    public final void _setByteField(int offset, byte value, byte mask) {
        this._setByteField(offset, (byte) (value ^ mask));
    }

    public final short _getShortField(int offset) {
        return this.segment.buffer.getShort(this.data + offset * 2);
    }

    public final short _getShortField(int offset, short mask) {
        return (short)(this._getShortField(offset) ^ mask);
    }

    public final void _setShortField(int offset, short value) {
        this.segment.buffer.putShort(this.data + offset * 2, value);
    }

    public final void _setShortField(int offset, short value, short mask) {
        this._setShortField(offset, (short)(value ^ mask));
    }

    public final int _getIntField(int offset) {
        return this.segment.buffer.getInt(this.data + offset * 4);
    }

    public final int _getIntField(int offset, int mask) {
        return this._getIntField(offset) ^ mask;
    }

    public final void _setIntField(int offset, int value) {
        this.segment.buffer.putInt(this.data + offset * 4, value);
    }

    public final void _setIntField(int offset, int value, int mask) {
        this._setIntField(offset, value ^ mask);
    }

    public final long _getLongField(int offset) {
        return this.segment.buffer.getLong(this.data + offset * 8);
    }

    public final long _getLongField(int offset, long mask) {
        return this._getLongField(offset) ^ mask;
    }

    public final void _setLongField(int offset, long value) {
        this.segment.buffer.putLong(this.data + offset * 8, value);
    }

    public final void _setLongField(int offset, long value, long mask) {
        this._setLongField(offset, value ^ mask);
    }

    public final float _getFloatField(int offset) {
        return this.segment.buffer.getFloat(this.data + offset * 4);
    }

    public final float _getFloatField(int offset, int mask) {
        return Float.intBitsToFloat(
            this.segment.buffer.getInt(this.data + offset * 4) ^ mask);
    }

    public final void _setFloatField(int offset, float value) {
        this.segment.buffer.putFloat(this.data + offset * 4, value);
    }

    public final void _setFloatField(int offset, float value, int mask) {
        this.segment.buffer.putInt(this.data + offset * 4,
                                   Float.floatToIntBits(value) ^ mask);
    }

    public final double _getDoubleField(int offset) {
        return this.segment.buffer.getDouble(this.data + offset * 8);
    }

    public final double _getDoubleField(int offset, long mask) {
        return Double.longBitsToDouble(
            this.segment.buffer.getLong(this.data + offset * 8) ^ mask);
    }

    public final void _setDoubleField(int offset, double value) {
        this.segment.buffer.putDouble(this.data + offset * 8, value);
    }

    public final void _setDoubleField(int offset, double value, long mask) {
        this.segment.buffer.putLong(this.data + offset * 8,
                                    Double.doubleToLongBits(value) ^ mask);
    }

    public final PointerBuilder _getPointerField(int index) {
        return new PointerBuilder(this.segment, this.pointers + index);
    }

}
