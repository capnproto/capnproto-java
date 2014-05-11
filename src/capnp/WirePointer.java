package capnp;

import java.nio.ByteBuffer;

class WirePointer {
    public final ByteBuffer buffer;
    public final int buffer_offset; // in words

    public static final byte STRUCT = 0;
    public static final byte LIST = 1;
    public static final byte FAR = 2;
    public static final byte OTHER = 3;

    public WirePointer(ByteBuffer buffer, int offset) {
        this.buffer = buffer;
        this.buffer_offset = offset;
    }

    public WirePointer(WordPointer word) {
        this.buffer = word.buffer;
        this.buffer_offset = word.offset;
    }

    public boolean isNull() {
        return this.buffer.getLong(this.buffer_offset * 8) == 0;
    }

    public int offsetAndKind() {
        return this.buffer.getInt(this.buffer_offset * 8);
    }

    public byte kind() {
        return (byte) (this.offsetAndKind() & 3);
    }

    public WordPointer target() {
        return new WordPointer(buffer,
                               this.buffer_offset + 1 + (this.offsetAndKind() >> 2));
    }

    public int inlineCompositeListElementCount() {
        return this.offsetAndKind() >> 2;
    }
}
