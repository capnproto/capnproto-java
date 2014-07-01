package org.capnproto;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

public final class BufferedWritableByteChannelWrapper implements BufferedWritableByteChannel {

    public final WritableByteChannel inner;
    public final ByteBuffer buf;

    public BufferedWritableByteChannelWrapper(WritableByteChannel w) {
        this.inner = w;
        this.buf = ByteBuffer.allocate(8192);
    }

    public final int write(ByteBuffer buf) throws IOException {
        int available = this.buf.remaining();
        int size = buf.remaining();
        if (size <= available) {
            this.buf.put(buf);
        } else if (size <= this.buf.capacity()) {
            //# Too much for this buffer, but not a full buffer's worth,
            //# so we'll go ahead and copy.
            //this.buf.put(buf); // XXX
            //this.inner.write();
        } else {
            //# Writing so much data that we might as well write
            //# directly to avoid a copy.
        }
        throw new Error("unimplemented");
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
