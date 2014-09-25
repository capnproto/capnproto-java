package org.capnproto;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.nio.ByteBuffer;

public final class PackedOutputStream implements WritableByteChannel {
    final BufferedOutputStream inner;

    public PackedOutputStream(BufferedOutputStream output) {
        this.inner = output;
    }

    public int write(ByteBuffer src) throws IOException {
        this.inner.getWriteBuffer();

        // TODO

        return 0;
    }

    public void close() throws IOException {
        this.inner.close();
    }

    public boolean isOpen() {
        return this.inner.isOpen();
    }
}
