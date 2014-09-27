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
                if (out >= outEnd) {
                    return len;
                }

                if (inBuf.remaining() == 0) {
                    // refresh buffer...
                    continue;
                }

                //# We have at least 1, but not 10, bytes available. We need to read
                //# slowly, doing a bounds check on each byte.

                // TODO
            } else {

                tag = inBuf.get();
                for (int n = 0; n < 8; ++n) {
                    boolean isNonzero = (tag & (1 << n)) != 0;
                    // ...
                }

            }

            if (tag == 0) {
                if (inBuf.remaining() == 0) {
                    throw new Error("Should always have non-empty buffer here.");
                }

                int runLength = inBuf.get() * 8;

                if (runLength > outEnd - out) {
                    throw new Error("Packed input did not end cleanly on a segment boundary");
                }


            } else if (tag == (byte)0xff) {

                int runLength = inBuf.get() * 8;


                if (inBuf.remaining() >= runLength) {

                } else {

                }

            }

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
