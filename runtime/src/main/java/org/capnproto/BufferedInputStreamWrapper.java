package org.capnproto;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public final class BufferedInputStreamWrapper implements BufferedInputStream {

    private final ReadableByteChannel inner;
    private final ByteBuffer buf;

    public BufferedInputStreamWrapper(ReadableByteChannel chan) {
        this.inner = chan;
        this.buf = ByteBuffer.allocate(8192);
        this.buf.limit(0);
    }

    public final int read(ByteBuffer dst) throws IOException {
        int numBytes = dst.remaining();
        if (numBytes < this.buf.remaining()) {
            //# Serve from the current buffer.
            ByteBuffer slice = this.buf.slice();
            slice.limit(numBytes);
            dst.put(slice);
            this.buf.position(this.buf.position() + numBytes);
            return numBytes;
        } else {
            //# Copy current available into destination.
            int fromFirstBuffer = this.buf.remaining();
            {
                ByteBuffer slice = this.buf.slice();
                slice.limit(fromFirstBuffer);
                dst.put(slice);
            }

            numBytes -= fromFirstBuffer;
            if (numBytes <= this.buf.capacity()) {
                //# Read the next buffer-full.
                this.buf.clear();
                int n = readAtLeast(this.inner, this.buf, numBytes);

                this.buf.rewind();
                ByteBuffer slice = this.buf.slice();
                slice.limit(numBytes);
                dst.put(slice);

                this.buf.limit(n);
                this.buf.position(numBytes);
                return fromFirstBuffer + numBytes;
            } else {
                //# Forward large read to the underlying stream.

                this.buf.clear();
                return fromFirstBuffer + readAtLeast(this.inner, dst, numBytes);
            }
        }
    }

    public final ByteBuffer getReadBuffer() throws IOException {
        if (this.buf.remaining() == 0) {
            this.buf.clear();
            int n = readAtLeast(this.inner, this.buf, 1);
            this.buf.rewind();
            this.buf.limit(n);
        }
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
