package org.capnproto;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

public final class ArrayOutputStream implements BufferedOutputStream {

    public final ByteBuffer buf;

    public ArrayOutputStream(ByteBuffer buf) {
        this.buf = buf.duplicate();
    }

    public final int write(ByteBuffer src) throws IOException {
        int available = this.buf.remaining();
        int size = src.remaining();
        if (available < size) {
            throw new IOException("backing buffer was not large enough");
        }
        this.buf.put(src);
        return size;
    }

    public final ByteBuffer getWriteBuffer() {
        return this.buf;
    }

    public final void close() throws IOException {
        return;
    }

    public final boolean isOpen() {
        return true;
    }

    public final void flush() { }
}
