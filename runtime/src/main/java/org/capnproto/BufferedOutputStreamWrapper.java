package org.capnproto;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

public final class BufferedOutputStreamWrapper implements BufferedOutputStream {

    private final WritableByteChannel inner;
    private final ByteBuffer buf;

    public BufferedOutputStreamWrapper(WritableByteChannel w) {
        this.inner = w;
        this.buf = ByteBuffer.allocate(8192);
    }

    public final int write(ByteBuffer src) throws IOException {
        int available = this.buf.remaining();
        int size = src.remaining();
        if (size <= available) {
            this.buf.put(src);
        } else if (size <= this.buf.capacity()) {
            //# Too much for this buffer, but not a full buffer's worth,
            //# so we'll go ahead and copy.
            ByteBuffer slice = src.slice();
            slice.limit(available);
            this.buf.put(slice);

            this.buf.rewind();
            while(this.buf.hasRemaining()) {
                this.inner.write(this.buf);
            }
            this.buf.rewind();

            src.position(src.position() + available);
            this.buf.put(src);
        } else {
            //# Writing so much data that we might as well write
            //# directly to avoid a copy.

            int pos = this.buf.position();
            this.buf.rewind();
            ByteBuffer slice = this.buf.slice();
            slice.limit(pos);
            while (slice.hasRemaining()) {
                this.inner.write(slice);
            }
            while (src.hasRemaining()) {
                this.inner.write(src);
            }
        }
        return size;
    }

    public final ByteBuffer getWriteBuffer() {
        return this.buf;
    }

    public final void close() throws IOException {
        this.inner.close();
    }

    public final boolean isOpen() {
        return this.inner.isOpen();
    }
}
