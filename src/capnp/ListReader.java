package capnp;

public class ListReader {
    SegmentReader segment;
    int ptr; // byte offset to front of list
    int elementCount;
    int step;
    int structDataSize; // in bits
    short structPointerCount;
    int nestingLimit;


    public StructReader getStructElement(int index) {
        // TODO check nesting limit

        int indexBit = index * this.step;

        int structData = this.ptr + (indexBit / 8);
        int structPointers = structData + (this.structDataSize / 8);

        return new StructReader(this.segment, structData, structPointers, this.structDataSize,
                                this.structPointerCount, (byte)(indexBit % 8), this.nestingLimit - 1);
    }
}
