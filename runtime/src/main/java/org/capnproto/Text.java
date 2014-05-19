package org.capnproto;

import java.nio.ByteBuffer;

public final class Text {

    public static final class Reader {
        public final ByteBuffer buffer;
        public final int offset; // in bytes
        public final int size; // in bytes, not including NUL terminator

        public Reader(ByteBuffer buffer, int offset, int size) {
            this.buffer = buffer;
            this.offset = offset * 8;
            this.size = size;
        }

        public Reader(String value) {
            try {
                byte[] bytes = value.getBytes("UTF-8");
                this.buffer = ByteBuffer.wrap(bytes);
                this.offset = 0;
                this.size = bytes.length;
            } catch (java.io.UnsupportedEncodingException e) {
                throw new Error("UTF-8 is unsupported");
            }
        }

        @Override
        public final String toString() {
            byte[] bytes = new byte[this.size];

            this.buffer.position(this.offset);
            this.buffer.get(bytes, 0, this.size);

            try {
                return new String(bytes, "UTF-8");
            } catch (java.io.UnsupportedEncodingException e) {
                return "unsupported encoding"; // XXX
            }
        }

    }

    public static final class Builder {
        public final ByteBuffer buffer;
        public final int offset;

        // size not including the NUL terminator
        public final int size;

        public Builder(ByteBuffer buffer, int offset, int size) {
            this.buffer = buffer;
            this.offset = offset;
            this.size = size;
        }
    }

}
