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
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Optional;

/**
 * Serialization using the standard (unpacked) stream encoding:
 * https://capnproto.org/encoding.html#serialization-over-a-stream
 */
public final class Serialize {

    static ByteBuffer makeByteBuffer(int bytes) {
        ByteBuffer result = ByteBuffer.allocate(bytes);
        result.order(ByteOrder.LITTLE_ENDIAN);
        result.mark();
        return result;
    }

    static final int MAX_SEGMENT_WORDS = (1 << 28) - 1;

    static ByteBuffer makeByteBufferForWords(int words) throws IOException {
        if (words > MAX_SEGMENT_WORDS) {
             // Trying to construct the segment would cause overflow.
             throw new DecodeException("segment has too many words (" + words + ")");
        }
        return makeByteBuffer(words * Constants.BYTES_PER_WORD);
    }

    /**
     * Attempts to fill the provided byte buffer using bytes from the provided ReadableByteChannel. Once the buffer has
     * been filled *or* the ReadableByteChannel has reached end-of-stream, returns the number of bytes read.
     */
    private static int tryFillBuffer(ByteBuffer buffer, ReadableByteChannel bc) throws IOException {
        int initialPosition = buffer.position();

        while (buffer.hasRemaining()) {
            int r = bc.read(buffer);
            if (r == 0) {
                throw new IOException("Read zero bytes. Is the channel in non-blocking mode?");
            } else if (r < 0) {
                break;
            }
        }

        return buffer.position() - initialPosition;
    }

    public static void fillBuffer(ByteBuffer buffer, ReadableByteChannel bc) throws IOException {
        while (buffer.hasRemaining()) {
            int r = bc.read(buffer);
            if (r < 0) {
                throw new IOException("premature EOF");
            } else if (r == 0) {
                throw new IOException("Read zero bytes. Is the channel in non-blocking mode?");
            }
        }
    }

    /**
     * Attempts to read a message from the provided BufferedInputStream with default options. Returns null if the input
     * stream reached end-of-stream on first read.
     */
    public static Optional<MessageReader> tryRead(ReadableByteChannel bc) throws IOException {
        return tryRead(bc, ReaderOptions.DEFAULT_READER_OPTIONS);
    }

    /**
     * Attempts to read a message from the provided BufferedInputStream with the provided options. Returns null if the
     * input stream reached end-of-stream on first read.
     */
    public static Optional<MessageReader> tryRead(ReadableByteChannel bc, ReaderOptions options) throws IOException {
        ByteBuffer firstWord = makeByteBufferForWords(1);
        int nBytes = tryFillBuffer(firstWord, bc);
        if (firstWord.hasRemaining()) {
            // We failed to read a whole word
            if (0 == nBytes) {
                // We were unable to read anything at all: the byte channel has reached end-of-stream
                return Optional.empty();
            } else {
                // We read fewer than 1 word's worth of bytes
                throw new IOException("premature EOF");
            }
        } else {
            // We filled the buffer
            return Optional.of(doRead(bc, options, firstWord));
        }
    }

    public static MessageReader read(ReadableByteChannel bc) throws IOException {
        return read(bc, ReaderOptions.DEFAULT_READER_OPTIONS);
    }

    public static MessageReader read(ReadableByteChannel bc, ReaderOptions options) throws IOException {
        ByteBuffer firstWord = makeByteBufferForWords(1);
        fillBuffer(firstWord, bc);
        return doRead(bc, options, firstWord);
    }

    private static MessageReader doRead(ReadableByteChannel bc,
                                        ReaderOptions options,
                                        ByteBuffer firstWord) throws IOException {
        int rawSegmentCount = firstWord.getInt(0);
        if (rawSegmentCount < 0 || rawSegmentCount > 511) {
            throw new DecodeException("segment count must be between 0 and 512");
        }

        int segmentCount = 1 + rawSegmentCount;

        int segment0Size = firstWord.getInt(4);

        if (segment0Size < 0) {
            throw new DecodeException("segment 0 has more than 2^31 words, which is unsupported");
        }

        long totalWords = segment0Size;

        // in words
        ArrayList<Integer> moreSizes = new ArrayList<>(segmentCount -1);

        if (segmentCount > 1) {
            ByteBuffer moreSizesRaw = makeByteBuffer(4 * (segmentCount & ~1));
            fillBuffer(moreSizesRaw, bc);
            for (int ii = 0; ii < segmentCount - 1; ++ii) {
                int size = moreSizesRaw.getInt(ii * 4);
                if (size < 0) {
                    throw new DecodeException("segment " + (ii + 1) +
                                              " has more than 2^31 words, which is unsupported");
                }

                moreSizes.add(size);
                totalWords += size;
            }
        }

        if (totalWords > options.traversalLimitInWords) {
            throw new DecodeException("Message size exceeds traversal limit.");
        }

        ByteBuffer[] segmentSlices = new ByteBuffer[segmentCount];

        segmentSlices[0] = makeByteBufferForWords(segment0Size);
        fillBuffer(segmentSlices[0], bc);
        segmentSlices[0].rewind();

        for (int ii = 1; ii < segmentCount; ++ii) {
            segmentSlices[ii] = makeByteBufferForWords(moreSizes.get(ii - 1));
            fillBuffer(segmentSlices[ii], bc);
            segmentSlices[ii].rewind();
        }

        return new MessageReader(segmentSlices, options);
    }

    public static MessageReader read(ByteBuffer bb) throws IOException {
        return read(bb, ReaderOptions.DEFAULT_READER_OPTIONS);
    }

    /**
     * Upon return, `bb.position()` will be at the end of the message.
     */
    public static MessageReader read(ByteBuffer bb, ReaderOptions options) throws IOException {
        bb.order(ByteOrder.LITTLE_ENDIAN);

        int rawSegmentCount = bb.getInt();
        int segmentCount = 1 + rawSegmentCount;
        if (rawSegmentCount < 0 || rawSegmentCount > 511) {
            throw new DecodeException("segment count must be between 0 and 512");
        }

        ByteBuffer[] segmentSlices = new ByteBuffer[segmentCount];

        int segmentSizesBase = bb.position();
        int segmentSizesSize = segmentCount * 4;

        int align = Constants.BYTES_PER_WORD - 1;
        int segmentBase = (segmentSizesBase + segmentSizesSize + align) & ~align;

        int totalWords = 0;

        for (int ii = 0; ii < segmentCount; ++ii) {
            int segmentSize = bb.getInt(segmentSizesBase + ii * 4);
            if (segmentSize > MAX_SEGMENT_WORDS -
                (totalWords + segmentBase / Constants.BYTES_PER_WORD)) {
                throw new DecodeException("segment size is too large");
            }

            bb.position(segmentBase + totalWords * Constants.BYTES_PER_WORD);
            segmentSlices[ii] = bb.slice();
            segmentSlices[ii].limit(segmentSize * Constants.BYTES_PER_WORD);
            segmentSlices[ii].order(ByteOrder.LITTLE_ENDIAN);

            totalWords += segmentSize;
        }
        bb.position(segmentBase + totalWords * Constants.BYTES_PER_WORD);

        if (options.traversalLimitInWords != -1 && totalWords > options.traversalLimitInWords) {
            throw new DecodeException("Message size exceeds traversal limit.");
        }

        return new MessageReader(segmentSlices, options);
    }

    public static long computeSerializedSizeInWords(MessageBuilder message) {
        return computeSerializedSizeInWords(message.getSegmentsForOutput());
    }

    //VisibleForTesting
    static long computeSerializedSizeInWords(ByteBuffer[] segments) {
        // From the capnproto documentation (https://capnproto.org/encoding.html#serialization-over-a-stream):
        // "When transmitting over a stream, the following should be sent..."
        long bytes = 0;
        // "(4 bytes) The number of segments, minus one..."
        bytes += 4;
        // "(N * 4 bytes) The size of each segment, in words."
        bytes += segments.length * 4L;
        // "(0 or 4 bytes) Padding up to the next word boundary."
        if (bytes % 8 != 0) {
            bytes += 4;
        }

        // The content of each segment, in order.
        for (int i = 0; i < segments.length; ++i) {
            ByteBuffer s = segments[i];
            bytes += s.limit();
        }

        return bytes / Constants.BYTES_PER_WORD;
    }

    private static void writeSegmentTable(WritableByteChannel outputChannel,
                              ByteBuffer[] segments) throws IOException {
        int tableSize = (segments.length + 2) & (~1);

        ByteBuffer table = ByteBuffer.allocate(4 * tableSize);
        table.order(ByteOrder.LITTLE_ENDIAN);

        table.putInt(0, segments.length - 1);

        for (int i = 0; i < segments.length; ++i) {
            table.putInt(4 * (i + 1), segments[i].limit() / 8);
        }

        // Any padding is already zeroed.
        while (table.hasRemaining()) {
            outputChannel.write(table);
        }
    }

    /**
     * Serializes a MessageBuilder to a WritableByteChannel.
     */
    public static void write(WritableByteChannel outputChannel,
                             MessageBuilder message) throws IOException {
        ByteBuffer[] segments = message.getSegmentsForOutput();
        writeSegmentTable(outputChannel, segments);

        for (ByteBuffer buffer : segments) {
            while(buffer.hasRemaining()) {
                outputChannel.write(buffer);
            }
        }
    }

    /**
     * Serializes a MessageReader to a WritableByteChannel.
     */
    public static void write(WritableByteChannel outputChannel,
                             MessageReader message) throws IOException {
        ByteBuffer[] segments = new ByteBuffer[message.arena.segments.size()];
        for (int ii = 0 ; ii < message.arena.segments.size(); ++ii) {
            segments[ii] = message.arena.segments.get(ii).buffer.duplicate();
        }

        writeSegmentTable(outputChannel, segments);

        for (ByteBuffer buffer : segments) {
            while(buffer.hasRemaining()) {
                outputChannel.write(buffer);
            }
        }
    }
}
