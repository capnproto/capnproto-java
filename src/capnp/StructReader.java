package capnp;

public class StructReader {
    public SegmentReader segment;
    public long data; //byte offset to data section

    // public WirePointer pointers;
    public int dataSize; // in bits
    public short pointerCount;
    public short bit0Offset;
    public int nestingLimit;


    public <T> T getDataField() {
        throw new Error();
    }
}
