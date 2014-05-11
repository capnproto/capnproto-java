package capnp;

import java.nio.ByteBuffer;

class StructPointer{
    public WirePointer ptr;

    public StructPointer(WirePointer ptr) {
        this.ptr = ptr;
    }

    public short dataSize() {
        return this.ptr.buffer.getShort(this.ptr.buffer_offset * 4 + 2);
    }

    public short ptrCount() {
        return this.ptr.buffer.getShort(this.ptr.buffer_offset * 4 + 3);
    }

    public int wordSize() {
        return this.dataSize() + this.ptrCount();
    }
}
