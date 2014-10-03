package org.capnproto;

import java.nio.ByteBuffer;

public final class Data {

    public static final class Reader {
        public final ByteBuffer buffer;
        public final int offset; // in bytes
        public final int size; // in bytes

        public Reader(ByteBuffer buffer, int offset, int size) {
            this.buffer = buffer;
            this.offset = offset * 8;
            this.size = size;
        }

        public final int size() {
            return this.size;
        }

        public ByteBuffer asByteBuffer() {
            ByteBuffer dup = this.buffer.duplicate();
            dup.position(this.offset);
            ByteBuffer result = dup.slice();
            result.limit(this.size);
            return result;
        }

        public byte[] asArray() {
            ByteBuffer dup = this.buffer.duplicate();
            byte result[] = new byte[this.size];
            dup.position(this.offset);
            dup.get(result, 0, this.size);
            return result;
        }
    }

    public static final class Builder {
        public final ByteBuffer buffer;
        public final int offset; // in bytes
        public final int size; // in bytes

        public Builder(ByteBuffer buffer, int offset, int size) {
            this.buffer = buffer;
            this.offset = offset;
            this.size = size;
        }
    }
}
