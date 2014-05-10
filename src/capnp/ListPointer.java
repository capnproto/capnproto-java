package capnp;

import java.nio.ByteBuffer;

class ListPointer extends WirePointer {

    public ListPointer(ByteBuffer buffer, int buffer_offset) {
        super(buffer, buffer_offset);
    }

    public byte elementSize() {
        return (byte)(this.buffer.getInt(buffer_offset * 2 + 1) & 7);
    }

    public int elementCount() {
        return this.buffer.getInt(buffer_offset * 2 + 1) >> 3;
    }

    public int inlineCompositeWordCount() {
        return this.elementCount();
    }
}
