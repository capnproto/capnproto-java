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

import java.util.Optional;

/**
 * Serialization using the packed encoding: https://capnproto.org/encoding.html#packing
 */
public final class SerializePacked {

    /**
     * Attempts to read a message from the provided BufferedInputStream with default options. Returns an empty optional
     * if the input stream reached end-of-stream on first read.
     */
    public static Optional<MessageReader> tryRead(BufferedInputStream input) throws java.io.IOException {
        return tryRead(input, ReaderOptions.DEFAULT_READER_OPTIONS);
    }

    /**
     * Attempts to read a message from the provided BufferedInputStream with the provided options. Returns an empty
     * optional if the input stream reached end-of-stream on first read.
     */
    public static Optional<MessageReader> tryRead(BufferedInputStream input, ReaderOptions options) throws java.io.IOException {
        PackedInputStream packedInput = new PackedInputStream(input);
        return Serialize.tryRead(packedInput, options);
    }

    /**
     * Reads a message from the provided BufferedInputStream with default options.
     */
    public static MessageReader read(BufferedInputStream input) throws java.io.IOException {
        return read(input, ReaderOptions.DEFAULT_READER_OPTIONS);
    }

    /**
     * Reads a message from the provided BufferedInputStream with the provided options.
     */
    public static MessageReader read(BufferedInputStream input, ReaderOptions options) throws java.io.IOException {
        PackedInputStream packedInput = new PackedInputStream(input);
        return Serialize.read(packedInput, options);
    }

    /**
     * Wraps the provided ReadableByteChannel in a BufferedInputStream and attempts to read a message from it with
     * default options. Returns an empty optional if the channel reached end-of-stream on first read.
     */
    public static Optional<MessageReader> tryReadFromUnbuffered(java.nio.channels.ReadableByteChannel input) throws java.io.IOException {
        return tryReadFromUnbuffered(input, ReaderOptions.DEFAULT_READER_OPTIONS);
    }

    /**
     * Wraps the provided ReadableByteChannel in a BufferedInputStream and attempts to read a message from it with
     * the provided options. Returns an empty optional if the channel reached end-of-stream on first read.
     */
    public static Optional<MessageReader> tryReadFromUnbuffered(java.nio.channels.ReadableByteChannel input,
                                                                ReaderOptions options) throws java.io.IOException {
        PackedInputStream packedInput = new PackedInputStream(new BufferedInputStreamWrapper(input));
        return Serialize.tryRead(packedInput, options);
    }

    /**
     * Wraps the provided ReadableByteChannel in a BufferedInputStream and reads a message from it with default options.
     */
    public static MessageReader readFromUnbuffered(java.nio.channels.ReadableByteChannel input) throws java.io.IOException {
        return readFromUnbuffered(input, ReaderOptions.DEFAULT_READER_OPTIONS);
    }

    /**
     * Wraps the provided ReadableByteChannel in a BufferedInputStream and reads a message from it with the provided
     * options.
     */
    public static MessageReader readFromUnbuffered(java.nio.channels.ReadableByteChannel input,
                                                   ReaderOptions options) throws java.io.IOException {
        PackedInputStream packedInput = new PackedInputStream(new BufferedInputStreamWrapper(input));
        return Serialize.read(packedInput, options);
    }

    /**
     * Serializes a MessageBuilder to a BufferedOutputStream.
     */
    public static void write(BufferedOutputStream output,
                             MessageBuilder message) throws java.io.IOException {
        PackedOutputStream packedOutputStream = new PackedOutputStream(output);
        Serialize.write(packedOutputStream, message);
    }

    /**
     * Serializes a MessageReader to a BufferedOutputStream.
     */
    public static void write(BufferedOutputStream output,
                             MessageReader message) throws java.io.IOException {
        PackedOutputStream packedOutputStream = new PackedOutputStream(output);
        Serialize.write(packedOutputStream, message);
    }

    /**
     * Serializes a MessageBuilder to an unbuffered output stream.
     */
    public static void writeToUnbuffered(java.nio.channels.WritableByteChannel output,
                                         MessageBuilder message) throws java.io.IOException {
        BufferedOutputStreamWrapper buffered = new BufferedOutputStreamWrapper(output);
        write(buffered, message);
        buffered.flush();
    }

    /**
     * Serializes a MessageReader to an unbuffered output stream.
     */
    public static void writeToUnbuffered(java.nio.channels.WritableByteChannel output,
                                         MessageReader message) throws java.io.IOException {
        BufferedOutputStreamWrapper buffered = new BufferedOutputStreamWrapper(output);
        write(buffered, message);
        buffered.flush();
    }

}
