package capnp;

public class PointerReader {
    public SegmentReader segment;
    public int pointer; // word offset
    public int nestingLimit;

    public PointerReader() {
        this.segment = null;
        this.pointer = 0; // XXX
        this.nestingLimit = 0x7fffffff;
    }

    public PointerReader(SegmentReader segment, int pointer, int nestingLimit) {
        this.segment = segment;
        this.pointer = pointer;
        this.nestingLimit = nestingLimit;
    }

    public void getText() {
        throw new Error();
    }
}
