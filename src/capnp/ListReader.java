package capnp;

public class ListReader {
    SegmentReader segment;
    int ptr; // byte offset to front of list
    int elementCount;
    int step;
    int structDataSize; // in bits
    short structPointerCount;
    int nestingLimit;


    public ListReader () {
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

    public StructReader getStructElement(int index) {
        // TODO check nesting limit

        int indexBit = index * this.step;

        int structData = this.ptr + (indexBit / 8);
        int structPointers = structData + (this.structDataSize / 8);

        return new StructReader(this.segment, structData, structPointers, this.structDataSize,
                                this.structPointerCount, (byte)(indexBit % 8), this.nestingLimit - 1);
    }
}
