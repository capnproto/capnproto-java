package capnp;

import java.nio.ByteBuffer;

class WordPointer {
    public final ByteBuffer buffer;
    public int offset; // in words

    public WordPointer(ByteBuffer buffer, int offset) {
        this.buffer = buffer;
        this.offset = offset;
    }
}
