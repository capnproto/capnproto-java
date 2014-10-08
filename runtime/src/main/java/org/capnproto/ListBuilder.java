package org.capnproto;

public class ListBuilder {
    public interface Factory<T> {
        T constructBuilder(SegmentBuilder segment, int ptr,
                           int elementCount, int step,
                           int structDataSize, short structPointerCount);
    }

    final SegmentBuilder segment;
    final int ptr; // byte offset to front of list
    final int elementCount;
    final int step; // in bits
    final int structDataSize; // in bits
    final short structPointerCount;

    public ListBuilder(SegmentBuilder segment, int ptr,
                       int elementCount, int step,
                       int structDataSize, short structPointerCount) {
        this.segment = segment;
        this.ptr = ptr;
        this.elementCount = elementCount;
        this.step = step;
        this.structDataSize = structDataSize;
        this.structPointerCount = structPointerCount;
    }

    public int size() {
        return this.elementCount;
    }

    protected boolean _getBooleanElement(int index) {
        byte b = this.segment.buffer.get(this.ptr + index / 8);
        return (b & (1 << (index % 8))) != 0;
    }

    protected byte _getByteElement(int index) {
        return this.segment.buffer.get(this.ptr + index);
    }

    protected short _getShortElement(int index) {
        return this.segment.buffer.getShort(this.ptr + index * 2);
    }

    protected int _getIntElement(int index) {
        return this.segment.buffer.getInt(this.ptr + index * 4);
    }

    protected long _getLongElement(int index) {
        return this.segment.buffer.getLong(this.ptr + index * 8);
    }

    protected float _getFloatElement(int index) {
        return this.segment.buffer.getFloat(this.ptr + index * 4);
    }

    protected double _getDoubleElement(int index) {
        return this.segment.buffer.getDouble(this.ptr + index * 8);
    }

    protected void _setBooleanElement(int index, boolean value) {
        int bitOffset = index;
        byte bitnum = (byte)(bitOffset % 8);
        int position = this.ptr + (bitOffset / 8);
        byte oldValue = this.segment.buffer.get(position);
        this.segment.buffer.put(position,
                                (byte)((oldValue & (~(1 << bitnum))) | (( value ? 1 : 0) << bitnum)));
    }

    protected void _setByteElement(int index, byte value) {
        this.segment.buffer.put(this.ptr + index, value);
    }

    protected void _setShortElement(int index, short value) {
        this.segment.buffer.putShort(this.ptr + index * 2, value);
    }

    protected void _setIntElement(int index, int value) {
        this.segment.buffer.putInt(this.ptr + index * 4, value);
    }

    protected void _setLongElement(int index, long value) {
        this.segment.buffer.putLong(this.ptr + index * 8, value);
    }

    protected void _setFloatElement(int index, float value) {
        this.segment.buffer.putFloat(this.ptr + index * 4, value);
    }

    protected void _setDoubleElement(int index, double value) {
        this.segment.buffer.putDouble(this.ptr + index * 8, value);
    }

    protected final <T> T _getStructElement(StructBuilder.Factory<T> factory, int index) {
        int indexBit = index * this.step;
        int structData = this.ptr + indexBit / 8 ;
        int structPointers = (structData + (this.structDataSize / 8)) / 8;

        return factory.constructBuilder(this.segment,
                                        structData,
                                        structPointers,
                                        this.structDataSize,
                                        this.structPointerCount,
                                        (byte)(indexBit % 8));
    }

    protected final <T> T _getPointerElement(FromPointerBuilder<T> factory, int index) {
        return factory.fromPointerBuilder(
            this.segment,
            (this.ptr + (index * this.step / Constants.BITS_PER_BYTE)) / Constants.BYTES_PER_WORD);
    }

    protected final <T> T _initPointerElement(InitFromPointerBuilder<T> factory, int index) {
        return factory.initFromPointerBuilder(
            this.segment,
            (this.ptr + (index * this.step / Constants.BITS_PER_BYTE)) / Constants.BYTES_PER_WORD);
    }

    protected final <T> T _initSizedPointerElement(InitSizedFromPointerBuilder<T> factory, int index, int elementCount) {
        return factory.initSizedFromPointerBuilder(
            this.segment,
            (this.ptr + (index * this.step / Constants.BITS_PER_BYTE)) / Constants.BYTES_PER_WORD,
            elementCount);
    }

    protected final <Reader> void _setPointerElement(SetPointerBuilder<Reader> factory, int index, Reader value) {
        factory.setPointerBuilder(this.segment,
                                  (this.ptr + (index * this.step / Constants.BITS_PER_BYTE)) / Constants.BYTES_PER_WORD,
                                  value);
    }

}
