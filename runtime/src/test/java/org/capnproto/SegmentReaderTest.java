package org.capnproto;

import org.capnproto.WireHelpers.FollowFarsResult;
import org.hamcrest.MatcherAssert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.hamcrest.CoreMatchers.is;

public class SegmentReaderTest {

    @Test
    public void in_boundsCalculationShouldNotOverflow() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(64);
        SegmentReader segmentReader = new SegmentReader(byteBuffer, null);
        MatcherAssert.assertThat(segmentReader.isInBounds(0, Integer.MAX_VALUE), is(false));
    }

    @Test
    public void oneWordAtLastWordShouldBeInBounds() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(64);
        SegmentReader segmentReader = new SegmentReader(byteBuffer, null);
        MatcherAssert.assertThat(segmentReader.isInBounds(7, 1), is(true));
    }

    @Test
    public void twoWordsAtLastWordShouldNotBeInBounds() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(64);
        SegmentReader segmentReader = new SegmentReader(byteBuffer, null);
        MatcherAssert.assertThat(segmentReader.isInBounds(7, 2), is(false));
    }

    @Test
    public void validSegmentWithNegativeOffsetShouldBeInBounds() {
        int refOffset;
        long ref;
        int refTarget;
        int dataSizeWords;
        int wordSize;
        
        /*
        Binary data:
            echo -n
              'FAAAAAEAAQDJqtK2cBpJhZ2LUEVMkYblyarStnAaSYWdi1BFTJGG4e3///8CAQAAAgAAAAAAAAD0////AAABAA=='
            | base64 -d

        Verify it is valid with:
            capnp decode --flat dksdk_std_schema.capnp GenericReturn
        where the .capnp comes from
        https://gitlab.com/diskuv/dksdk-schema/-/blob/afbf9564a60f2670f6b9dfb3c423fc55dd4c3013/src/dksdk_std_schema.capnp
        */
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[] {
            (byte)0x14, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x01, (byte)0x00,
            (byte)0xC9, (byte)0xAA, (byte)0xD2, (byte)0xB6, (byte)0x70, (byte)0x1A, (byte)0x49, (byte)0x85,
            (byte)0x9D, (byte)0x8B, (byte)0x50, (byte)0x45, (byte)0x4C, (byte)0x91, (byte)0x86, (byte)0xE5,
            (byte)0xC9, (byte)0xAA, (byte)0xD2, (byte)0xB6, (byte)0x70, (byte)0x1A, (byte)0x49, (byte)0x85,
            (byte)0x9D, (byte)0x8B, (byte)0x50, (byte)0x45, (byte)0x4C, (byte)0x91, (byte)0x86, (byte)0xE1,
            (byte)0xED, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x02, (byte)0x01, (byte)0x00, (byte)0x00,
            (byte)0x02, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0xF4, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x00            
        }).order(ByteOrder.LITTLE_ENDIAN);
        SegmentReader segment = new SegmentReader(byteBuffer, null);

        /* Read root Struct: GenericReturn. */
        refOffset = 0; /* At the root STRUCT POINTER */
        ref = segment.get(refOffset);
        refTarget = WirePointer.target(refOffset, ref);
        dataSizeWords = StructPointer.dataSize(ref);
        wordSize = dataSizeWords + StructPointer.ptrCount(ref);
        MatcherAssert.assertThat(segment.isInBounds(refTarget, wordSize), is(true));

        /* Read inner Struct: ComObject. */
        refOffset = refTarget + dataSizeWords; /* At the inner STRUCT POINTER */
        ref = segment.get(refOffset);
        refTarget = WirePointer.target(refOffset, ref);
        dataSizeWords = StructPointer.dataSize(ref);
        wordSize = dataSizeWords + StructPointer.ptrCount(ref);
        MatcherAssert.assertThat(segment.isInBounds(refTarget, wordSize), is(true));    
    }

}