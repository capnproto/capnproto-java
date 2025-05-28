// Copyright (c) 2018 Sandstorm Development Group, Inc. and contributors
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
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Map;

public class TestUtils {
    @SuppressWarnings("unchecked")
    public static Map<Integer, FileChannel> getChannelMap(MemoryMappedAllocator allocator) {
        try {
            Field channelMapField = MemoryMappedAllocator.class.getDeclaredField("channelMap");
            channelMapField.setAccessible(true);
            return (Map<Integer, FileChannel>) channelMapField.get(allocator);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void assertAllBytes(FileChannel channel, char allocator) throws IOException {
        long size = channel.size();
        long position = 0;
        int bufferSize = 8192;
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);

        while (position < size) {
            buffer.clear();
            int bytesToRead = (int) Math.min(bufferSize, size - position);
            buffer.limit(bytesToRead);

            int read = channel.read(buffer, position);
            if (read == -1) break; // EOF

            buffer.flip();
            for (int i = 0; i < read; i++) {
                byte b = buffer.get();
                if (b != allocator) {
                    throw new AssertionError("File content is not all 'A'. Found byte: " + b + " at position " + (position + i));
                }
            }
            position += read;
        }
    }

}
