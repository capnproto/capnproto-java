package org.capnproto;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public final class ArrayInputStream implements BufferedInputStream {

    public final ByteBuffer buf;

    public ArrayInputStream(ByteBuffer buf) {
        this.buf = buf;
    }

    public final int read(ByteBuffer dst) throws IOException {
        int available = this.buf.remaining();
        int size = dst.remaining();

        ByteBuffer slice = buf.slice();
        slice.limit(size);
        dst.put(slice);

        this.buf.position(this.buf.position() + size);
        return size;
    }

    public final ByteBuffer getReadBuffer() {
        return this.buf;
    }

    public final void close() throws IOException {
        return;
    }

    public final boolean isOpen() {
        return true;
    }
}
