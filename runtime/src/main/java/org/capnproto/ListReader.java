package org.capnproto;

public class ListReader {
    public interface Factory<T> {
        T constructReader(SegmentReader segment,
                          int ptr,
                          int elementCount, int step,
                          int structDataSize, short structPointerCount,
                          int nestingLimit);
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
    }

    public ListReader(SegmentReader segment, int ptr,
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

    protected <T> T _getStructElement(StructReader.Factory<T> factory, int index) {
        // TODO check nesting limit

        int indexBit = index * this.step;

        int structData = this.ptr + (indexBit / 8);
        int structPointers = structData + (this.structDataSize / 8);

        return factory.constructReader(this.segment, structData, structPointers / 8, this.structDataSize,
                                       this.structPointerCount, (byte) (indexBit % 8), this.nestingLimit - 1);
    }

    protected PointerReader _getPointerElement(int index) {
        return new PointerReader(this.segment,
                                 (this.ptr + (index * this.step / Constants.BITS_PER_BYTE)) / Constants.BYTES_PER_WORD,
                                 this.nestingLimit);
    }

    protected <T> T _getPointerElement(FromPointerReader<T> factory, int index, SegmentReader defaultSegment, int defaultOffset) {
        return factory.fromPointerReader(this.segment,
                                         (this.ptr + (index * this.step / Constants.BITS_PER_BYTE)) / Constants.BYTES_PER_WORD,
                                         defaultSegment,
                                         defaultOffset,
                                         this.nestingLimit);
    }

    protected <T> T _getPointerElement(FromPointerReaderBlobDefault<T> factory, int index,
                                       java.nio.ByteBuffer defaultBuffer, int defaultOffset, int defaultSize) {
        return factory.fromPointerReaderBlobDefault(
            this.segment,
            (this.ptr + (index * this.step / Constants.BITS_PER_BYTE)) / Constants.BYTES_PER_WORD,
            defaultBuffer,
            defaultOffset,
            defaultSize);
    }

}
