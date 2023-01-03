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

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

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

      byte[] outputBytes = new byte[exampleBytes.length];
      Serialize.write(new ArrayOutputStream(ByteBuffer.wrap(outputBytes)), messageReader);
      Assert.assertArrayEquals(exampleBytes, outputBytes);
    }

    // ------
    // read via ByteBuffer
    {
      MessageReader messageReader = Serialize.read(ByteBuffer.wrap(exampleBytes));
      checkSegmentContents(exampleSegmentCount, messageReader.arena);
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

  @Test
  public void testTryReadByteBuffer() throws IOException {
    // `tryRead` returns a non-null `MessageReader` when given correct input
    {
      byte[] input = new byte[]{
              0, 0, 0, 0, // 1 segment
              0, 0, 0, 0  // Segment 0 contains 0 bytes
              // No padding
              // Segment 0 (empty)
      };
      Optional<MessageReader> messageReader = Serialize.tryRead(new ArrayInputStream(ByteBuffer.wrap(input)));
      Assert.assertTrue(messageReader.isPresent());
    }

    // `tryRead` returns null when given no input
    {
      Optional<MessageReader> messageReader = Serialize.tryRead(new ArrayInputStream(ByteBuffer.wrap(new byte[]{})));
      Assert.assertFalse(messageReader.isPresent());
    }

    // `tryRead` throws when given too few bytes to form the first word
    {
      byte[] input = new byte[]{
              0, 0, 0, 0, // 1 segment
              0, 0, 0     // Premature end of stream after 7 bytes
      };
      Assert.assertThrows(IOException.class, () -> Serialize.tryRead(new ArrayInputStream(ByteBuffer.wrap(input))));
    }
  }

  @Test(expected=DecodeException.class)
  public void testSegment0SizeOverflow() throws java.io.IOException {
        byte[] input = {0, 0, 0, 0, -1, -1, -1, -113};
        java.nio.channels.ReadableByteChannel channel =
            java.nio.channels.Channels.newChannel(new java.io.ByteArrayInputStream(input));
        MessageReader message = Serialize.read(channel);
  }

  @Test(expected=DecodeException.class)
  public void testSegment1SizeOverflow() throws java.io.IOException {
      byte[] input = {
          1, 0, 0, 0, 1, 0, 0, 0,
          -1, -1, -1, -113, 0, 0, 0, 0};
        java.nio.channels.ReadableByteChannel channel =
            java.nio.channels.Channels.newChannel(new java.io.ByteArrayInputStream(input));
        MessageReader message = Serialize.read(channel);
  }

    @Test
    @Ignore("Ignored by default because the huge array used in the test results in a long execution")
    public void computeSerializedSizeInWordsShouldNotOverflowOnLargeSegmentCounts() {
        ByteBuffer dummySegmentBuffer = ByteBuffer.allocate(0);
        ByteBuffer[] segments = new ByteBuffer[Integer.MAX_VALUE / 2];
        Arrays.fill(segments, dummySegmentBuffer);
        assertThat(Serialize.computeSerializedSizeInWords(segments), is((segments.length * 4L + 4) / Constants.BYTES_PER_WORD));
    }
}
