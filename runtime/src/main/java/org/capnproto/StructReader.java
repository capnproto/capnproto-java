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

    public final boolean getBooleanField(int offset) {
        // XXX should use unsigned operations
        if (offset < this.dataSize) {
            if (offset == 0) {
                offset = this.bit0Offset;
            }
            byte b = this.segment.buffer.get(this.data + offset / 8);

            return (b & (1 << (offset % 8))) != 0;
        } else {
            return false;
        }
    }

    public final boolean getBooleanField(int offset, boolean mask) {
        return this.getBooleanField(offset) ^ mask;
    }

    public final byte getByteField(int offset) {
        if ((offset + 1) * 8 <= this.dataSize) {
            return this.segment.buffer.get(this.data + offset);
        } else {
            return 0;
        }
    }

    public final byte getByteField(int offset, byte mask) {
        return (byte)(this.getByteField(offset) ^ mask);
    }

    public final short getShortField(int offset) {
        if ((offset + 1) * 16 <= this.dataSize) {
            return this.segment.buffer.getShort(this.data + offset * 2);
        } else {
            return 0;
        }
    }

    public final short getShortField(int offset, short mask) {
        return (short)(this.getShortField(offset) ^ mask);
    }

    public final int getIntField(int offset) {
        if ((offset + 1) * 32 <= this.dataSize) {
            return this.segment.buffer.getInt(this.data + offset * 4);
        } else {
            return 0;
        }
    }

    public final int getIntField(int offset, int mask) {
        return this.getIntField(offset) ^ mask;
    }

    public final long getLongField(int offset) {
        if ((offset + 1) * 64 <= this.dataSize) {
            return this.segment.buffer.getLong(this.data + offset * 8);
        } else {
            return 0;
        }
    }

    public final long getLongField(int offset, long mask) {
        return this.getLongField(offset) ^ mask;
    }

    public final float getFloatField(int offset) {
        if ((offset + 1) * 32 <= this.dataSize) {
            return this.segment.buffer.getFloat(this.data + offset * 4);
        } else {
            return 0;
        }
    }

    public final float getFloatField(int offset, int mask) {
        if ((offset + 1) * 32 <= this.dataSize) {
            return Float.intBitsToFloat(this.segment.buffer.getInt(this.data + offset * 4) ^ mask);
        } else {
            return Float.intBitsToFloat(mask);
        }
    }

    public final double getDoubleField(int offset) {
        if ((offset + 1) * 64 <= this.dataSize) {
            return this.segment.buffer.getDouble(this.data + offset * 8);
        } else {
            return 0;
        }
    }

    public final double getDoubleField(int offset, long mask) {
        if ((offset + 1) * 64 <= this.dataSize) {
            return Double.longBitsToDouble(this.segment.buffer.getLong(this.data + offset * 8) ^ mask);
        } else {
            return Double.longBitsToDouble(mask);
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
