package capnp;

import java.nio.ByteBuffer;

class WirePointer {
    public final ByteBuffer buffer;
    public final int buffer_offset; // in words

    public final byte STRUCT = 0;
    public final byte LIST = 1;
    public final byte FAR = 2;
    public final byte OTHER = 3;

    public WirePointer(ByteBuffer buffer, int offset) {
        this.buffer = buffer;
        this.buffer_offset = offset;
    }

    public int offset_and_kind() {
        return this.buffer.getInt(buffer_offset * 2);
    }

    public byte kind() {
        return (byte) (this.offset_and_kind() & 3);
    }

    public WordPointer target() {
        return new WordPointer(buffer,
                               1 + (this.offset_and_kind() >> 2));

    }
}
