package org.capnproto;

import org.capnproto.WireHelpers.FollowFarsResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SegmentReaderTest {

    @Test
    public void in_boundsCalculationShouldNotOverflow() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(64);
        SegmentReader segmentReader = new SegmentReader(byteBuffer, null);
        assertEquals(false, segmentReader.isInBounds(0, Integer.MAX_VALUE));
    }

    @Test
    public void oneWordAtLastWordShouldBeInBounds() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(64);
        SegmentReader segmentReader = new SegmentReader(byteBuffer, null);
        assertEquals(true, segmentReader.isInBounds(7, 1));
    }

    @Test
    public void twoWordsAtLastWordShouldNotBeInBounds() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(64);
        SegmentReader segmentReader = new SegmentReader(byteBuffer, null);
        assertEquals(false, segmentReader.isInBounds(7, 2));
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
            // Struct pointer, offset of 5 words. 1 data section word, 1 pointer section word.
            (byte)0x14, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x01, (byte)0x00,

            (byte)0xC9, (byte)0xAA, (byte)0xD2, (byte)0xB6, (byte)0x70, (byte)0x1A, (byte)0x49, (byte)0x85,
            (byte)0x9D, (byte)0x8B, (byte)0x50, (byte)0x45, (byte)0x4C, (byte)0x91, (byte)0x86, (byte)0xE5,
            (byte)0xC9, (byte)0xAA, (byte)0xD2, (byte)0xB6, (byte)0x70, (byte)0x1A, (byte)0x49, (byte)0x85,
            (byte)0x9D, (byte)0x8B, (byte)0x50, (byte)0x45, (byte)0x4C, (byte)0x91, (byte)0x86, (byte)0xE1,

            // List pointer, offset -5, Byte elements, length 32.
            (byte)0xED, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x02, (byte)0x01, (byte)0x00, (byte)0x00,

            // Root struct data section.
            (byte)0x02, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,

            // Root struct pointer section.
            // Struct pointer, offset -3 words. 0 data section words, 1 pointer section word.
            (byte)0xF4, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x00
        }).order(ByteOrder.LITTLE_ENDIAN);
        SegmentReader segment = new SegmentReader(byteBuffer, null);

        /* Read root Struct: GenericReturn. */
        refOffset = 0; /* At the root STRUCT POINTER */
        ref = segment.get(refOffset);
        refTarget = WirePointer.target(refOffset, ref);
        dataSizeWords = StructPointer.dataSize(ref);
        wordSize = dataSizeWords + StructPointer.ptrCount(ref);
        assertEquals(true, segment.isInBounds(refTarget, wordSize));

        /* Read inner Struct: ComObject. */
        refOffset = refTarget + dataSizeWords; /* At the inner STRUCT POINTER */
        ref = segment.get(refOffset);
        refTarget = WirePointer.target(refOffset, ref);
        dataSizeWords = StructPointer.dataSize(ref);
        wordSize = dataSizeWords + StructPointer.ptrCount(ref);
        assertEquals(true, segment.isInBounds(refTarget, wordSize));
    }

}
