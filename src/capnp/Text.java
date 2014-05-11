package capnp;

import java.nio.ByteBuffer;

public class Text {

    public static class Reader {
        public final ByteBuffer buffer;
        public final int offset; // in bytes
        public final int size; // in bytes

        public Reader(ListPointer ptr) {
            this.buffer = ptr.ptr.buffer;
            this.offset = ptr.ptr.buffer_offset * 8;
            this.size = ptr.elementCount();
        }

        public String toString() {
            byte[] bytes = new byte[this.size];
            buffer.get(bytes, this.offset, this.size);
            try {
                return new String(bytes, "UTF-8");
            } catch(java.io.UnsupportedEncodingException e) {
                return "unsupported encoding"; // XXX
            }
        }
    }

}
