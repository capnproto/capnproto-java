package capnp;

import java.nio.ByteBuffer;

class ListPointer extends WirePointer {

    public ListPointer(ByteBuffer buffer, int buffer_offset) {
        super(buffer, buffer_offset);
    }

    public int elementCount() {
        return this.buffer.getInt(buffer_offset * 2) >> 3;
    }
}
