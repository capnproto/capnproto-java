package org.capnproto;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public final class BufferedInputStreamWrapper implements BufferedInputStream {

    private final ReadableByteChannel inner;
    private final ByteBuffer buf;
    private int cap = 0;

    public BufferedInputStreamWrapper(ReadableByteChannel chan) {
        this.inner = chan;
        this.buf = ByteBuffer.allocate(8192);
    }

    public final int read(ByteBuffer dst) throws IOException {
        int numBytes = dst.remaining();
        if (numBytes < cap - this.buf.position()) {
            //# Serve from the current buffer.
            ByteBuffer slice = this.buf.slice();
            slice.limit(numBytes);
            dst.put(slice);
            this.buf.position(this.buf.position() + numBytes);
            return numBytes;
        } else {
            //# Copy current available into destination.
            ByteBuffer slice = this.buf.slice();
            int fromFirstBuffer = cap - this.buf.position();
            slice.limit(fromFirstBuffer);
            dst.put(slice);

            numBytes -= fromFirstBuffer;
            if (numBytes <= this.buf.capacity()) {
                //# Read the next buffer-full.
                this.buf.rewind();
                int n = readAtLeast(this.inner, this.buf, numBytes);

                // ...
                //ByteBuffer slice =
                //dst.put(

                this.cap = n;
                this.buf.position(numBytes);
                return fromFirstBuffer + numBytes;
            } else {
                //# Forward large read to the underlying stream.
            }
        }
        throw new Error("unimplemented");
    }

    public final ByteBuffer getReadBuffer() {
        return this.buf;
    }

    public final void close() throws IOException {
        this.inner.close();
    }

    public final boolean isOpen() {
        return this.inner.isOpen();
    }

    public static int readAtLeast(ReadableByteChannel reader, ByteBuffer buf, int minBytes) throws IOException {
        int numRead = 0;
        while (numRead < minBytes) {
            int res = reader.read(buf);
            if (res < 0) {
                throw new Error("premature EOF");
            }
            numRead += res;
        }
        return numRead;
    }
}
