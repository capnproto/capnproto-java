package org.capnproto;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.ByteBuffer;

public final class PackedInputStream implements ReadableByteChannel {
    final BufferedInputStream inner;

    public PackedInputStream(BufferedInputStream input) {
        this.inner = input;
    }

    public int read(ByteBuffer outBuf) throws IOException {

        int len = outBuf.remaining();
        if (len == 0) { return 0; }

        if (len % 8 != 0) {
            throw new Error("PackedInputStream reads must be word-aligned");
        }

        int out = outBuf.position();
        int outEnd = out + len;

        ByteBuffer inBuf = this.inner.getReadBuffer();


        while (true) {

            byte tag = 0;

            //if (outBuf

            if (inBuf.remaining() < 10) {
                // TODO
            }

            // TODO


            if (out == outEnd) {
                return len;
            }
        }
    }

    public void close() throws IOException {
        inner.close();
    }

    public boolean isOpen() {
        return inner.isOpen();
    }
}
