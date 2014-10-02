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
            // not thread safe
            this.buffer.position(this.offset);
            ByteBuffer result = this.buffer.slice();
            result.limit(this.size);
            return result;
        }

        public byte[] asArray() {
            // not thread safe
            byte result[] = new byte[this.size];
            this.buffer.position(this.offset);
            this.buffer.get(result, 0, this.size);
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
