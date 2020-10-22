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
import java.nio.ByteOrder;
import java.nio.channels.AsynchronousByteChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

public final class Serialize {

    static ByteBuffer makeByteBuffer(int bytes) {
        ByteBuffer result = ByteBuffer.allocate(bytes);
        result.order(ByteOrder.LITTLE_ENDIAN);
        result.mark();
        return result;
    }

    public static void fillBuffer(ByteBuffer buffer, ReadableByteChannel bc) throws IOException {
        while(buffer.hasRemaining()) {
            int r = bc.read(buffer);
            if (r < 0) {
                throw new IOException("premature EOF");
            } else if (r == 0) {
                throw new IOException("Read zero bytes. Is the channel in non-blocking mode?");
            }
        }
    }

    public static MessageReader read(ReadableByteChannel bc) throws IOException {
        return read(bc, ReaderOptions.DEFAULT_READER_OPTIONS);
    }

    public static MessageReader read(ReadableByteChannel bc, ReaderOptions options) throws IOException {
        ByteBuffer firstWord = makeByteBuffer(Constants.BYTES_PER_WORD);
        fillBuffer(firstWord, bc);

        int segmentCount = 1 + firstWord.getInt(0);

        int segment0Size = 0;
        if (segmentCount > 0) {
            segment0Size = firstWord.getInt(4);
        }

        int totalWords = segment0Size;

        if (segmentCount > 512) {
            throw new IOException("too many segments");
        }

        // in words
        ArrayList<Integer> moreSizes = new ArrayList<Integer>();

        if (segmentCount > 1) {
            ByteBuffer moreSizesRaw = makeByteBuffer(4 * (segmentCount & ~1));
            fillBuffer(moreSizesRaw, bc);
            for (int ii = 0; ii < segmentCount - 1; ++ii) {
                int size = moreSizesRaw.getInt(ii * 4);
                moreSizes.add(size);
                totalWords += size;
            }
        }

        if (totalWords > options.traversalLimitInWords) {
            throw new DecodeException("Message size exceeds traversal limit.");
        }

        ByteBuffer allSegments = makeByteBuffer(totalWords * Constants.BYTES_PER_WORD);
        fillBuffer(allSegments, bc);

        ByteBuffer[] segmentSlices = new ByteBuffer[segmentCount];

        allSegments.rewind();
        segmentSlices[0] = allSegments.slice();
        segmentSlices[0].limit(segment0Size * Constants.BYTES_PER_WORD);
        segmentSlices[0].order(ByteOrder.LITTLE_ENDIAN);

        int offset = segment0Size;
        for (int ii = 1; ii < segmentCount; ++ii) {
            allSegments.position(offset * Constants.BYTES_PER_WORD);
            segmentSlices[ii] = allSegments.slice();
            segmentSlices[ii].limit(moreSizes.get(ii - 1) * Constants.BYTES_PER_WORD);
            segmentSlices[ii].order(ByteOrder.LITTLE_ENDIAN);
            offset += moreSizes.get(ii - 1);
        }

        return new MessageReader(segmentSlices, options);
    }

    public static MessageReader read(ByteBuffer bb) throws IOException {
        return read(bb, ReaderOptions.DEFAULT_READER_OPTIONS);
    }

    /*
     * Upon return, `bb.position()` will be at the end of the message.
     */
    public static MessageReader read(ByteBuffer bb, ReaderOptions options) throws IOException {
        bb.order(ByteOrder.LITTLE_ENDIAN);

        int segmentCount = 1 + bb.getInt();
        if (segmentCount > 512) {
            throw new IOException("too many segments");
        }

        ByteBuffer[] segmentSlices = new ByteBuffer[segmentCount];

        int segmentSizesBase = bb.position();
        int segmentSizesSize = segmentCount * 4;

        int align = Constants.BYTES_PER_WORD - 1;
        int segmentBase = (segmentSizesBase + segmentSizesSize + align) & ~align;

        int totalWords = 0;

        for (int ii = 0; ii < segmentCount; ++ii) {
            int segmentSize = bb.getInt(segmentSizesBase + ii * 4);

            bb.position(segmentBase + totalWords * Constants.BYTES_PER_WORD);
            segmentSlices[ii] = bb.slice();
            segmentSlices[ii].limit(segmentSize * Constants.BYTES_PER_WORD);
            segmentSlices[ii].order(ByteOrder.LITTLE_ENDIAN);

            totalWords += segmentSize;
        }
        bb.position(segmentBase + totalWords * Constants.BYTES_PER_WORD);

        if (totalWords > options.traversalLimitInWords) {
            throw new DecodeException("Message size exceeds traversal limit.");
        }

        return new MessageReader(segmentSlices, options);
    }

    public static long computeSerializedSizeInWords(MessageBuilder message) {
        final ByteBuffer[] segments = message.getSegmentsForOutput();

        // From the capnproto documentation:
        // "When transmitting over a stream, the following should be sent..."
        long bytes = 0;
        // "(4 bytes) The number of segments, minus one..."
        bytes += 4;
        // "(N * 4 bytes) The size of each segment, in words."
        bytes += segments.length * 4;
        // "(0 or 4 bytes) Padding up to the next word boundary."
        if (bytes % 8 != 0) {
            bytes += 4;
        }

        // The content of each segment, in order.
        for (int i = 0; i < segments.length; ++i) {
            final ByteBuffer s = segments[i];
            bytes += s.limit();
        }

        return bytes / Constants.BYTES_PER_WORD;
    }

    public static void write(WritableByteChannel outputChannel,
                             MessageBuilder message) throws IOException {
        ByteBuffer[] segments = message.getSegmentsForOutput();
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

        for (ByteBuffer buffer : segments) {
            while(buffer.hasRemaining()) {
                outputChannel.write(buffer);
            }
        }
    }

    static final class AsyncMessageReader {

        private final AsynchronousByteChannel channel;
        private final ReaderOptions options;
        private final CompletableFuture<MessageReader> readCompleted = new CompletableFuture<>();

        public AsyncMessageReader(AsynchronousByteChannel channel, ReaderOptions options) {
            this.channel = channel;
            this.options = options;
        }

        public CompletableFuture<MessageReader> getMessage() {
            readHeader();
            return readCompleted;
        }

        private void readHeader() {
            read(Constants.BYTES_PER_WORD, firstWord -> {
                final var segmentCount = 1 + firstWord.getInt(0);
                final var segment0Size = firstWord.getInt(4);

                if (segmentCount == 1) {
                    readSegments(segment0Size, segmentCount, segment0Size, null);
                    return;
                }

                // check before allocating segment size buffer
                if (segmentCount > 512) {
                    readCompleted.completeExceptionally(new IOException("Too many segments"));
                    return;
                }

                read(4 * (segmentCount & ~1), moreSizesRaw -> {
                    final var moreSizes = new int[segmentCount - 1];
                    var totalWords = segment0Size;

                    for (int ii = 0; ii < segmentCount - 1; ++ii) {
                        int size = moreSizesRaw.getInt(ii * 4);
                        moreSizes[ii] = size;
                        totalWords += size;
                    }

                    readSegments(totalWords, segmentCount, segment0Size, moreSizes);
                });
            });
        }

        private void readSegments(int totalWords, int segmentCount, int segment0Size, int[] moreSizes) {
            if (totalWords > options.traversalLimitInWords) {
                readCompleted.completeExceptionally(
                        new DecodeException("Message size exceeds traversal limit."));
                return;
            }

            final var segmentSlices = new ByteBuffer[segmentCount];
            if (totalWords == 0) {
                for (int ii = 0; ii < segmentCount; ++ii) {
                    segmentSlices[ii] = ByteBuffer.allocate(0);
                }
                readCompleted.complete(new MessageReader(segmentSlices, options));
                return;
            }

            read(totalWords * Constants.BYTES_PER_WORD, allSegments -> {
                allSegments.rewind();
                segmentSlices[0] = allSegments.slice();
                segmentSlices[0].limit(segment0Size * Constants.BYTES_PER_WORD);
                segmentSlices[0].order(ByteOrder.LITTLE_ENDIAN);

                int offset = segment0Size;
                for (int ii = 1; ii < segmentCount; ++ii) {
                    allSegments.position(offset * Constants.BYTES_PER_WORD);
                    var segmentSize = moreSizes[ii-1];
                    segmentSlices[ii] = allSegments.slice();
                    segmentSlices[ii].limit(segmentSize * Constants.BYTES_PER_WORD);
                    segmentSlices[ii].order(ByteOrder.LITTLE_ENDIAN);
                    offset += segmentSize;
                }

                readCompleted.complete(new MessageReader(segmentSlices, options));
            });
        }

        private void read(int bytes, Consumer<ByteBuffer> consumer) {
            final var buffer = Serialize.makeByteBuffer(bytes);
            final var handler = new CompletionHandler<Integer, Object>() {
                @Override
                public void completed(Integer result, Object attachment) {
                    // System.out.println("read " + result + " bytes");
                    if (result <= 0) {
                        var text = result == 0
                                ? "Read zero bytes. Is the channel in non-blocking mode?"
                                : "Premature EOF";
                        readCompleted.completeExceptionally(new IOException(text));
                    } else if (buffer.hasRemaining()) {
                        // retry
                        channel.read(buffer, null, this);
                    } else {
                        consumer.accept(buffer);
                    }
                }

                @Override
                public void failed(Throwable exc, Object attachment) {
                    readCompleted.completeExceptionally(exc);
                }
            };

            this.channel.read(buffer, null, handler);
        }
    }

    public static CompletableFuture<MessageReader> readAsync(AsynchronousByteChannel channel) {
        return readAsync(channel, ReaderOptions.DEFAULT_READER_OPTIONS);
    }

    public static CompletableFuture<MessageReader> readAsync(AsynchronousByteChannel channel, ReaderOptions options) {
        return new AsyncMessageReader(channel, options).getMessage();
    }

    public static CompletableFuture<java.lang.Void> writeAsync(AsynchronousByteChannel outputChannel, MessageBuilder message) {
        final var writeCompleted = new CompletableFuture<java.lang.Void>();
        final var segments = message.getSegmentsForOutput();
        assert segments.length > 0: "Empty message";
        final int tableSize = (segments.length + 2) & (~1);
        final var table = ByteBuffer.allocate(4 * tableSize);

        table.order(ByteOrder.LITTLE_ENDIAN);
        table.putInt(0, segments.length - 1);

        for (int ii = 0; ii < segments.length; ++ii) {
            table.putInt(4 * (ii + 1), segments[ii].limit() / 8);
        }

        outputChannel.write(table, 0, new CompletionHandler<Integer, Integer>() {

            @Override
            public void completed(Integer result, Integer attachment) {
                //System.out.println("Wrote " + result + " bytes");
                if (writeCompleted.isCancelled()) {
                    // TODO do we really want to interrupt here?
                    return;
                }

                if (attachment == segments.length) {
                    writeCompleted.complete(null);
                    return;
                }

                outputChannel.write(segments[attachment], attachment + 1, this);
            }

            @Override
            public void failed(Throwable exc, Integer attachment) {
                writeCompleted.completeExceptionally(exc);
            }
        });
        return writeCompleted.copy();
    }
}
