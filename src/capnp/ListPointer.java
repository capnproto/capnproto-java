package capnp;

import java.nio.ByteBuffer;

class ListPointer {
    public WirePointer ptr;

    public ListPointer(WirePointer ptr) {
        this.ptr = ptr;
    }

    public byte elementSize() {
        return (byte)(this.ptr.buffer.getInt(this.ptr.buffer_offset * 8 + 4) & 7);
    }

    public int elementCount() {
        return this.ptr.buffer.getInt(this.ptr.buffer_offset * 8 + 4) >> 3;
    }

    public int inlineCompositeWordCount() {
        return this.elementCount();
    }
}
