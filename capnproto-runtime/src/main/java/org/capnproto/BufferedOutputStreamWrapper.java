// Copyright (c) 2013-2014 Sandstorm Development Group, Inc. and contributors
// Licensed under the MIT License:
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

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

    public final void flush() throws IOException {
        int pos = this.buf.position();
        this.buf.rewind();
        this.buf.limit(pos);
        this.inner.write(this.buf);
        this.buf.clear();
    }
}
