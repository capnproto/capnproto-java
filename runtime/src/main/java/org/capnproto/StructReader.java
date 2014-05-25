package org.capnproto;

public final class StructReader {
    public final SegmentReader segment;
    public final int data; //byte offset to data section
    public final int pointers; // word offset of pointer section
    public final int dataSize; // in bits
    public final short pointerCount;
    public final byte bit0Offset;
    public final int nestingLimit;

    public StructReader(SegmentReader segment, int data,
                        int pointers, int dataSize, short pointerCount,
                        byte bit0Offset, int nestingLimit) {
        this.segment = segment;
        this.data = data;
        this.pointers = pointers;
        this.dataSize = dataSize;
        this.pointerCount = pointerCount;
        this.bit0Offset = bit0Offset;
        this.nestingLimit = nestingLimit;
    }

    public final boolean getBoolField(int offset) {
        // XXX should use unsigned operations
        if (offset < this.dataSize) {
            if (offset == 0) {
                offset = this.bit0Offset;
            }
            byte b = this.segment.buffer.get(offset / 8);
            return (b & (1 << (offset % 8))) != 0;
        } else {
            return false;
        }
    }

    public final byte getByteField(int offset) {
        if ((offset + 1) * 8 <= this.dataSize) {
            return this.segment.buffer.get(this.data + offset);
        } else {
            return 0;
        }
    }

    public final byte getShortField(int offset) {
        if ((offset + 1) * 16 <= this.dataSize) {
            return this.segment.buffer.get(this.data + offset * 2);
        } else {
            return 0;
        }
    }

    public final int getIntField(int offset) {
        if ((offset + 1) * 32 <= this.dataSize) {
            return this.segment.buffer.getInt(this.data + offset * 4);
        } else {
            return 0;
        }
    }

    public final long getLongField(int offset) {
        if ((offset + 1) * 64 <= this.dataSize) {
            return this.segment.buffer.getLong(this.data + offset * 8);
        } else {
            return 0;
        }
    }

    public final PointerReader getPointerField(int ptrIndex) {
        if (ptrIndex < this.pointerCount) {
            return new PointerReader(this.segment,
                this.pointers + ptrIndex,
                this.nestingLimit);
        } else {
            return new PointerReader();
        }
    }
}
