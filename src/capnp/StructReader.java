package capnp;

public class StructReader {
    public SegmentReader segment;
    public int data; //byte offset to data section

    public int pointers; // word offset of pointer section
    public int dataSize; // in bits
    public short pointerCount;
    public byte bit0Offset;
    public int nestingLimit;


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

    public boolean getBoolField(int offset) {
        // XXX should use unsigned operations
        if (offset < this.dataSize) {
            if (offset == 0) {
                offset = this.bit0Offset;
            }
            byte b = this.segment.ptr.get(offset / 8);
            return (b & (1 << (offset % 8))) != 0;
        } else {
            return false;
        }
    }

    public byte getByteField(int offset) {
        if ((offset + 1) * 8 <= this.dataSize) {
            return this.segment.ptr.get(this.data + offset);
        } else {
            return 0;
        }
    }

    public int getIntField(int offset) {
        if ((offset + 1) * 32 <= this.dataSize) {
            return this.segment.ptr.getInt((this.data / 4) + offset);
        } else {
            return 0;
        }
    }

    public PointerReader getPointerField(int ptrIndex) {
        if (ptrIndex < this.pointerCount) {
            return new PointerReader(this.segment,
                                     this.pointers + ptrIndex,
                                     this.nestingLimit);
        } else {
            return new PointerReader();
        }
    }
}
