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

public class ArrayInputStream implements BufferedInputStream {

    private final ByteBuffer buf;

    public ArrayInputStream(ByteBuffer buf) {
        this.buf = buf.asReadOnlyBuffer();
    }

    @Override
    public final int read(ByteBuffer dst) throws IOException {
        int available = this.buf.remaining();
        int size = java.lang.Math.min(dst.remaining(), available);
        if (size == 0) {
            // end of stream
            return -1;
        }

        ByteBuffer slice = this.buf.slice();
        slice.limit(size);
        dst.put(slice);

        this.buf.position(this.buf.position() + size);
        return size;
    }

    @Override
    public final ByteBuffer getReadBuffer() {
        if (buf.remaining() > 0) {
            return buf;
        } else {
            throw new DecodeException("Premature EOF while reading buffer");
        }
    }

    @Override
    public final void close() throws IOException {
        return;
    }

    @Override
    public final boolean isOpen() {
        return true;
    }
}
