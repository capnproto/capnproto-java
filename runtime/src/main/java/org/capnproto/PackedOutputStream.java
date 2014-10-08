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
import java.nio.channels.WritableByteChannel;
import java.nio.ByteBuffer;

public final class PackedOutputStream implements WritableByteChannel {
    final BufferedOutputStream inner;

    public PackedOutputStream(BufferedOutputStream output) {
        this.inner = output;
    }

    public int write(ByteBuffer inBuf) throws IOException {
        int length = inBuf.remaining();
        ByteBuffer out = this.inner.getWriteBuffer();

        ByteBuffer slowBuffer = ByteBuffer.allocate(20);

        int inPtr = inBuf.position();
        int inEnd = inPtr + length;
        while (inPtr < inEnd) {
            if (out.remaining() < 10) {
                //# Oops, we're out of space. We need at least 10
                //# bytes for the fast path, since we don't
                //# bounds-check on every byte.

                if (out == slowBuffer) {
                    int oldLimit = out.limit();
                    out.limit(out.position());
                    out.rewind();
                    this.inner.write(out);
                    out.limit(oldLimit);
                }

                out = slowBuffer;
                out.rewind();
            }

            int tagPos = out.position();
            out.position(tagPos + 1);

            byte curByte;

            curByte = inBuf.get(inPtr);
            byte bit0 = (curByte != 0) ? (byte)1 : (byte)0;
            out.put(curByte);
            out.position(out.position() + bit0 - 1);
            inPtr += 1;

            curByte = inBuf.get(inPtr);
            byte bit1 = (curByte != 0) ? (byte)1 : (byte)0;
            out.put(curByte);
            out.position(out.position() + bit1 - 1);
            inPtr += 1;

            curByte = inBuf.get(inPtr);
            byte bit2 = (curByte != 0) ? (byte)1 : (byte)0;
            out.put(curByte);
            out.position(out.position() + bit2 - 1);
            inPtr += 1;

            curByte = inBuf.get(inPtr);
            byte bit3 = (curByte != 0) ? (byte)1 : (byte)0;
            out.put(curByte);
            out.position(out.position() + bit3 - 1);
            inPtr += 1;

            curByte = inBuf.get(inPtr);
            byte bit4 = (curByte != 0) ? (byte)1 : (byte)0;
            out.put(curByte);
            out.position(out.position() + bit4 - 1);
            inPtr += 1;

            curByte = inBuf.get(inPtr);
            byte bit5 = (curByte != 0) ? (byte)1 : (byte)0;
            out.put(curByte);
            out.position(out.position() + bit5 - 1);
            inPtr += 1;

            curByte = inBuf.get(inPtr);
            byte bit6 = (curByte != 0) ? (byte)1 : (byte)0;
            out.put(curByte);
            out.position(out.position() + bit6 - 1);
            inPtr += 1;

            curByte = inBuf.get(inPtr);
            byte bit7 = (curByte != 0) ? (byte)1 : (byte)0;
            out.put(curByte);
            out.position(out.position() + bit7 - 1);
            inPtr += 1;

            byte tag = (byte)((bit0 << 0) | (bit1 << 1) | (bit2 << 2) | (bit3 << 3) |
                              (bit4 << 4) | (bit5 << 5) | (bit6 << 6) | (bit7 << 7));

            out.put(tagPos, tag);

            if (tag == 0) {
                //# An all-zero word is followed by a count of
                //# consecutive zero words (not including the first
                //# one).
                int runStart = inPtr;
                int limit = inEnd;
                if (limit - inPtr > 255 * 8) {
                    limit = inPtr + 255 * 8;
                }
                while(inPtr < limit && inBuf.getLong(inPtr) == 0){
                    inPtr += 8;
                }
                out.put((byte)((inPtr - runStart)/8));

            } else if (tag == (byte)0xff) {
                //# An all-nonzero word is followed by a count of
                //# consecutive uncompressed words, followed by the
                //# uncompressed words themselves.

                //# Count the number of consecutive words in the input
                //# which have no more than a single zero-byte. We look
                //# for at least two zeros because that's the point
                //# where our compression scheme becomes a net win.

                int runStart = inPtr;
                int limit = inEnd;
                if (limit - inPtr > 255 * 8) {
                    limit = inPtr + 255 * 8;
                }

                while (inPtr < limit) {
                    byte c = 0;
                    for (int ii = 0; ii < 8; ++ii) {
                        c += (inBuf.get(inPtr) == 0 ? 1 : 0);
                        inPtr += 1;
                    }
                    if (c >= 2) {
                        //# Un-read the word with multiple zeros, since
                        //# we'll want to compress that one.
                        inPtr -= 8;
                        break;
                    }
                }

                int count = inPtr - runStart;
                out.put((byte)(count / 8));

                if (count <= out.remaining()) {
                    //# There's enough space to memcpy.
                    inBuf.position(runStart);
                    ByteBuffer slice = inBuf.slice();
                    slice.limit(count);
                    out.put(slice);
                } else {
                    //# Input overruns the output buffer. We'll give it
                    //# to the output stream in one chunk and let it
                    //# decide what to do.

                    if (out == slowBuffer) {
                        int oldLimit = out.limit();
                        out.limit(out.position());
                        out.rewind();
                        this.inner.write(out);
                        out.limit(oldLimit);
                    }

                    inBuf.position(runStart);
                    ByteBuffer slice = inBuf.slice();
                    slice.limit(count);
                    while(slice.hasRemaining()) {
                        this.inner.write(slice);
                    }

                    out = this.inner.getWriteBuffer();
                }
            }
        }

        if (out == slowBuffer) {
            out.limit(out.position());
            out.rewind();
            this.inner.write(out);
        }

        inBuf.position(inPtr);
        return length;
    }

    public void close() throws IOException {
        this.inner.close();
    }

    public boolean isOpen() {
        return this.inner.isOpen();
    }
}
