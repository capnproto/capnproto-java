package capnp;

import java.nio.ByteBuffer;

class StructPointer extends WirePointer {

    public StructPointer(ByteBuffer buffer, int buffer_offset) {
        super(buffer, buffer_offset);
    }

    public short dataSize() {
        return this.buffer.getShort(this.buffer_offset * 4 + 2);
    }

    public short ptrCount() {
        return this.buffer.getShort(this.buffer_offset * 4 + 3);
    }

    public int wordSize() {
        return this.dataSize() + this.ptrCount();
    }
}
