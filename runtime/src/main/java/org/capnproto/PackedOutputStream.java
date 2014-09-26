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

                out = slowBuffer;
                out.rewind();
            }

            int tagPos = out.position();
            out.position(tagPos + 1);

            byte curByte;

            curByte = inBuf.get(inPtr);
            byte bit0 = (curByte != 0) ? (byte)1 : (byte)0;
            out.put(curByte);
            out.position(out.position() - bit0);
            inPtr += 1;

            curByte = inBuf.get(inPtr);
            byte bit1 = (curByte != 0) ? (byte)1 : (byte)0;
            out.put(curByte);
            out.position(out.position() - bit1);
            inPtr += 1;

            curByte = inBuf.get(inPtr);
            byte bit2 = (curByte != 0) ? (byte)1 : (byte)0;
            out.put(curByte);
            out.position(out.position() - bit2);
            inPtr += 1;

            curByte = inBuf.get(inPtr);
            byte bit3 = (curByte != 0) ? (byte)1 : (byte)0;
            out.put(curByte);
            out.position(out.position() - bit3);
            inPtr += 1;

            curByte = inBuf.get(inPtr);
            byte bit4 = (curByte != 0) ? (byte)1 : (byte)0;
            out.put(curByte);
            out.position(out.position() - bit4);
            inPtr += 1;

            curByte = inBuf.get(inPtr);
            byte bit5 = (curByte != 0) ? (byte)1 : (byte)0;
            out.put(curByte);
            out.position(out.position() - bit5);
            inPtr += 1;

            curByte = inBuf.get(inPtr);
            byte bit6 = (curByte != 0) ? (byte)1 : (byte)0;
            out.put(curByte);
            out.position(out.position() - bit6);
            inPtr += 1;

            curByte = inBuf.get(inPtr);
            byte bit7 = (curByte != 0) ? (byte)1 : (byte)0;
            out.put(curByte);
            out.position(out.position() - bit7);
            inPtr += 1;

            byte tag = (byte)((bit0 << 0) | (bit1 << 1) | (bit2 << 2) | (bit3 << 3) |
                              (bit4 << 4) | (bit5 << 5) | (bit6 << 6) | (bit7 << 7));

            out.put(tagPos, tag);

            if (tag == 0) {
                //# An all-zero word is followed by a count of
                //# consecutive zero words (not including the first
                //# one).

                inBuf.position(inPtr);

                long inWord = inBuf.getLong();
                int limit = inEnd;
                if (limit - inPtr > 255) {
                    limit = inPtr + 255;
                }
                while(inBuf.position() < limit && inWord == 0) {
                    inWord = inBuf.getLong();
                }

                out.put((byte)(inPtr - inBuf.position()));
                inPtr = inBuf.position();
            } else if (tag == 0xff) {
                //# An all-nonzero word is followed by a count of
                //# consecutive uncompressed words, followed by the
                //# uncompressed words themselves.

                //# Count the number of consecutive words in the input
                //# which have no more than a single zero-byte. We look
                //# for at least two zeros because that's the point
                //# where our compression scheme becomes a net win.


                // TODO
            }


        }
        return length;
    }

    public void close() throws IOException {
        this.inner.close();
    }

    public boolean isOpen() {
        return this.inner.isOpen();
    }
}
