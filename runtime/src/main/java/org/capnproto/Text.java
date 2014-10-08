package org.capnproto;

import java.nio.ByteBuffer;

public final class Text {
    public static final class Factory implements FromPointerReaderBlobDefault<Reader>,
                                      FromPointerBuilderBlobDefault<Builder>,
                                      SetPointerBuilder<Reader>,
                                      InitSizedFromPointerBuilder<Builder> {
        public final Reader fromPointerReaderBlobDefault(SegmentReader segment, int pointer, java.nio.ByteBuffer defaultBuffer,
                                                   int defaultOffset, int defaultSize) {
            return WireHelpers.readTextPointer(segment, pointer, defaultBuffer, defaultOffset, defaultSize);
        }

        public final Builder fromPointerBuilderBlobDefault(SegmentBuilder segment, int pointer,
                                                     java.nio.ByteBuffer defaultBuffer, int defaultOffset, int defaultSize) {
            return WireHelpers.getWritableTextPointer(pointer,
                                                      segment,
                                                      defaultBuffer,
                                                      defaultOffset,
                                                      defaultSize);
        }

        public final Builder initSizedFromPointerBuilder(SegmentBuilder segment, int pointer, int size) {
            return WireHelpers.initTextPointer(pointer, segment, size);
        }

        public final void setPointerBuilder(SegmentBuilder segment, int pointer, Reader value) {
            WireHelpers.setTextPointer(pointer, segment, value);
        }
    }
    public static final Factory factory = new Factory();

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

        public final int size() {
            return this.size;
        }

        @Override
        public final String toString() {
            byte[] bytes = new byte[this.size];

            ByteBuffer dup = this.buffer.duplicate();
            dup.position(this.offset);
            dup.get(bytes, 0, this.size);

            try {
                return new String(bytes, "UTF-8");
            } catch (java.io.UnsupportedEncodingException e) {
                throw new Error("UTF-8 is unsupported");
            }
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

        @Override
        public final String toString() {
            byte[] bytes = new byte[this.size];

            ByteBuffer dup = this.buffer.duplicate();
            dup.position(this.offset);
            dup.get(bytes, 0, this.size);

            try {
                return new String(bytes, "UTF-8");
            } catch (java.io.UnsupportedEncodingException e) {
                throw new Error("UTF-8 is unsupported");
            }
        }

    }

}
