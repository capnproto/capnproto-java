package org.capnproto;

import org.hamcrest.MatcherAssert;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.hamcrest.CoreMatchers.is;

public class SegmentReaderTest {

    @Test
    public void in_boundsCalculationShouldNotOverflow() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(64);
        SegmentReader segmentReader = new SegmentReader(byteBuffer, null);
        MatcherAssert.assertThat(segmentReader.isInBounds(0, Integer.MAX_VALUE), is(false));
    }
}