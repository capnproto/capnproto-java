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
import java.net.SocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.Test;

public class SerializeTest {

  /**
   * @param arena: segment `i` contains `i` words each set to `i`
   */
  private void checkSegmentContents(int exampleSegmentCount, ReaderArena arena) {
    Assert.assertEquals(arena.segments.size(), exampleSegmentCount);
    for (int i = 0; i < exampleSegmentCount; ++i) {
      SegmentReader segment = arena.segments.get(i);
      java.nio.LongBuffer segmentWords = segment.buffer.asLongBuffer();

      Assert.assertEquals(segmentWords.capacity(), i);
      segmentWords.rewind();
      while (segmentWords.hasRemaining()) {
        Assert.assertEquals(segmentWords.get(), i);
      }
    }
  }

  /**
   * @param exampleSegmentCount number of segments
   * @param exampleBytes byte array containing `segmentCount` segments; segment `i` contains `i` words each set to `i`
   */
  private void expectSerializesTo(int exampleSegmentCount, byte[] exampleBytes) throws java.io.IOException {
    // ----
    // read via ReadableByteChannel
    {
      MessageReader messageReader = Serialize.read(new ArrayInputStream(ByteBuffer.wrap(exampleBytes)));
      checkSegmentContents(exampleSegmentCount, messageReader.arena);
    }

    // ------
    // read via ByteBuffer
    {
      MessageReader messageReader = Serialize.read(ByteBuffer.wrap(exampleBytes));
      checkSegmentContents(exampleSegmentCount, messageReader.arena);
    }

    // read via AsyncChannel
    expectSerializesToAsync(exampleSegmentCount, exampleBytes);
  }

  private void expectSerializesToAsync(int exampleSegmentCount, byte[] exampleBytes) throws IOException {
    var done =  new CompletableFuture<java.lang.Void>();
    var server = AsynchronousServerSocketChannel.open();
    server.bind(null);
    server.accept(null, new CompletionHandler<AsynchronousSocketChannel, Object>() {
      @Override
      public void completed(AsynchronousSocketChannel socket, Object attachment) {
        socket.write(ByteBuffer.wrap(exampleBytes), null, new CompletionHandler<Integer, Object>() {
          @Override
          public void completed(Integer result, Object attachment) {
            done.complete(null);
          }

          @Override
          public void failed(Throwable exc, Object attachment) {
            done.completeExceptionally(exc);
          }
        });
      }

      @Override
      public void failed(Throwable exc, Object attachment) {
        done.completeExceptionally(exc);
      }
    });

    var socket = AsynchronousSocketChannel.open();
    try {
      socket.connect(server.getLocalAddress()).get();
      var messageReader = Serialize.readAsync(socket).get();
      checkSegmentContents(exampleSegmentCount, messageReader.arena);
      done.get();
    }
    catch (InterruptedException exc) {
      Assert.fail(exc.getMessage());
    }
    catch (ExecutionException exc) {
      Assert.fail(exc.getMessage());
    }
  }

  @Test
  public void testSegmentReading() throws java.io.IOException {
    // When transmitting over a stream, the following should be sent. All integers are unsigned and little-endian.
    // - (4 bytes) The number of segments, minus one (since there is always at least one segment).
    // - (N * 4 bytes) The size of each segment, in words.
    // - (0 or 4 bytes) Padding up to the next word boundary.
    // - The content of each segment, in order.

    expectSerializesTo(1, new byte[]{
        0, 0, 0, 0, // 1 segment
        0, 0, 0, 0  // Segment 0 contains 0 bytes
        // No padding
        // Segment 0 (empty)
      });

    expectSerializesTo(2, new byte[]{
        1, 0, 0, 0, // 2 segments
        0, 0, 0, 0, // Segment 0 contains 0 words
        1, 0, 0, 0, // Segment 1 contains 1 words
        // Padding
        0, 0, 0, 0,
        // Segment 0 (empty)
        // Segment 1
        1, 0, 0, 0, 0, 0, 0, 0
      });

    expectSerializesTo(3, new byte[] {
        2, 0, 0, 0, // 3 segments
        0, 0, 0, 0, // Segment 0 contains 0 words
        1, 0, 0, 0, // Segment 1 contains 1 words
        2, 0, 0, 0, // Segment 2 contains 2 words
        // No padding
        // Segment 0 (empty)
        // Segment 1
        1, 0, 0, 0, 0, 0, 0, 0,
        // Segment 2
        2, 0, 0, 0, 0, 0, 0, 0,
        2, 0, 0, 0, 0, 0, 0, 0
      });

    expectSerializesTo(4, new byte[]{
        3, 0, 0, 0, // 4 segments
        0, 0, 0, 0, // Segment 0 contains 0 words
        1, 0, 0, 0, // Segment 1 contains 1 words
        2, 0, 0, 0, // Segment 2 contains 2 words
        3, 0, 0, 0, // Segment 3 contains 3 words
        // Padding
        0, 0, 0, 0,
        // Segment 0 (empty)
        // Segment 1
        1, 0, 0, 0, 0, 0, 0, 0,
        // Segment 2
        2, 0, 0, 0, 0, 0, 0, 0,
        2, 0, 0, 0, 0, 0, 0, 0,
        // Segment 3
        3, 0, 0, 0, 0, 0, 0, 0,
        3, 0, 0, 0, 0, 0, 0, 0,
        3, 0, 0, 0, 0, 0, 0, 0
      });
  }
}
