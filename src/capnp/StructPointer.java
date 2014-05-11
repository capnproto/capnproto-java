package capnp;

import java.nio.ByteBuffer;

class StructPointer{
    public WirePointer ptr;

    public StructPointer(WirePointer ptr) {
        this.ptr = ptr;
    }

    public short dataSize() {
        return this.ptr.buffer.getShort(this.ptr.buffer_offset * 8 + 4);
    }

    public short ptrCount() {
        return this.ptr.buffer.getShort(this.ptr.buffer_offset * 8 + 6);
    }

    public int wordSize() {
        return this.dataSize() + this.ptrCount();
    }
}
