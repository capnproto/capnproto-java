package org.capnproto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LayoutTest {

    private static final int MAX_NESTING_LIMIT = 0x7fffffff;

    private class BareStructReader implements StructReader.Factory<StructReader> {
        @Override
        public StructReader constructReader(SegmentReader segment, int data, int pointers, int dataSize, short pointerCount, int nestingLimit) {
            return new StructReader(segment, data, pointers, dataSize, pointerCount, nestingLimit);
        }
    }

    @Test
    public void testSimpleRawDataStruct() {
        byte[] data = new byte[] {
            0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x23, 0x45, (byte)0x467, (byte)0x89, (byte)0xab, (byte)0xcd, (byte)0xef
        };

        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        ReaderArena arena = new ReaderArena(new ByteBuffer[]{ buffer }, 0x7fffffffffffffffL);

        StructReader reader = WireHelpers.readStructPointer(new BareStructReader(), arena.tryGetSegment(0), 0, null, 0, MAX_NESTING_LIMIT);

        assertEquals(0xefcdab8967452301L, reader._getLongField(0));
        assertEquals(0L, reader._getLongField(1));

        assertEquals(0x67452301, reader._getIntField(0));
        assertEquals(0xefcdab89, reader._getIntField(1));
        assertEquals(0, reader._getIntField(2));

        assertEquals((short)0x2301, reader._getShortField(0));
        assertEquals((short)0x6745, reader._getShortField(1));
        assertEquals((short)0xab89, reader._getShortField(2));
        assertEquals((short)0xefcd, reader._getShortField(3));
        assertEquals((short)0, reader._getShortField(4));

        assertEquals(true, reader._getBooleanField(0));
        assertEquals(false, reader._getBooleanField(1));
        assertEquals(false, reader._getBooleanField(2));

        assertEquals(false, reader._getBooleanField(3));
        assertEquals(false, reader._getBooleanField(4));
        assertEquals(false, reader._getBooleanField(5));
        assertEquals(false, reader._getBooleanField(6));
        assertEquals(false, reader._getBooleanField(7));

        assertEquals(true, reader._getBooleanField(8));
        assertEquals(true, reader._getBooleanField(9));
        assertEquals(false, reader._getBooleanField(10));
        assertEquals(false, reader._getBooleanField(11));
        assertEquals(false, reader._getBooleanField(12));
        assertEquals(true, reader._getBooleanField(13));
        assertEquals(false, reader._getBooleanField(14));
        assertEquals(false, reader._getBooleanField(15));

        assertEquals(true, reader._getBooleanField(63));
        assertEquals(false, reader._getBooleanField(64));
    }

    /**
     * @see <a href="https://github.com/capnproto/capnproto-java/issues/122">#122</a>
     */
    @Test
    public void readStructPointerShouldThrowDecodeExceptionOnOutOfBoundsStructPointer() {
        byte[] brokenMSG = new byte[]{
                0x00, 0x00, 0x00, 0x00, 0x07, 0x00, 0x00, 0x00, //declare word size of 7, with payload of only 6 words
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
        };

        ByteBuffer buffer = ByteBuffer.wrap(brokenMSG);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        ReaderArena arena = new ReaderArena(new ByteBuffer[]{ buffer }, 0x7fffffffffffffffL);

        assertThrows(DecodeException.class, () -> WireHelpers.readStructPointer(new BareStructReader(), arena.tryGetSegment(0), 0, null, 0, MAX_NESTING_LIMIT));
    }


    private static class BareListReader implements ListReader.Factory<ListReader> {
        BareListReader() {
        }

        @Override
        public ListReader constructReader(SegmentReader segment, int ptr, int elementCount, int step, int structDataSize, short structPointerCount, int nestingLimit) {
            return new ListReader(segment, ptr, elementCount, step, structDataSize, structPointerCount, nestingLimit);
        }
    }

    @Test
    public void readListPointerShouldThrowDecodeExceptionOnOutOfBoundsCompositeListPointer() {
        byte[] brokenMSG = {
                // set list pointer bits to 1, elementSize to 7 to indicate composite list and number of words in the list (minus tag) to 0x1FFFFFFF (max value possible in 29b limit)
                0x01, 0x00, 0x00, 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00,//tag with element wordSize of 1
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
        };

        ByteBuffer buffer = ByteBuffer.wrap(brokenMSG);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        ReaderArena arena = new ReaderArena(new ByteBuffer[]{buffer}, 0x7fffffffffffffffL);

        assertThrows(DecodeException.class, () -> WireHelpers.readListPointer(new BareListReader(), arena.tryGetSegment(0), 0, null, 0, (byte) 0, MAX_NESTING_LIMIT));
    }

    private class BareStructBuilder implements StructBuilder.Factory<StructBuilder> {
        private StructSize structSize;

        public BareStructBuilder(StructSize structSize) {
            this.structSize = structSize;
        }

        @Override
        public StructBuilder constructBuilder(SegmentBuilder segment, int data, int pointers, int dataSize, short pointerCount) {
            return new StructBuilder(segment, data, pointers, dataSize, pointerCount);
        }

        @Override
        public StructSize structSize() {
            return this.structSize;
        }
    }

    @Test
    public void testStructRoundTripOneSegment() {
        ByteBuffer buffer = ByteBuffer.allocate(1024*8);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        SegmentBuilder segment = new SegmentBuilder(buffer, new BuilderArena(BuilderArena.SUGGESTED_FIRST_SEGMENT_WORDS,
                BuilderArena.SUGGESTED_ALLOCATION_STRATEGY));
        BareStructBuilder factory = new BareStructBuilder(new StructSize((short) 2, (short) 4));
        StructBuilder builder  = WireHelpers.initStructPointer(factory, 0, segment, factory.structSize());

        setUpStruct(builder);
        checkStruct(builder);
    }

    private void setUpStruct(StructBuilder builder) {
        builder._setLongField(0, 0x1011121314151617L);
        builder._setIntField(2, 0x20212223);
        builder._setShortField(6, (short)0x3031);
        builder._setByteField(14, (byte)0x40);
        builder._setBooleanField(120, false);
        builder._setBooleanField(121, false);
        builder._setBooleanField(122, true);
        builder._setBooleanField(123, false);
        builder._setBooleanField(124, true);
        builder._setBooleanField(125, true);
        builder._setBooleanField(126, true);
        builder._setBooleanField(127, false);
    }

    private void checkStruct(StructBuilder builder) {
        assertEquals(0x1011121314151617L, builder._getLongField(0));
        assertEquals(0x20212223, builder._getIntField(2));
        assertEquals(0x3031, builder._getShortField(6));
        assertEquals(0x40, builder._getByteField(14));
        assertEquals(false, builder._getBooleanField(120));
        assertEquals(false, builder._getBooleanField(121));
        assertEquals(true, builder._getBooleanField(122));
        assertEquals(false, builder._getBooleanField(123));
        assertEquals(true, builder._getBooleanField(124));
        assertEquals(true, builder._getBooleanField(125));
        assertEquals(true, builder._getBooleanField(126));
        assertEquals(false, builder._getBooleanField(127));
    }
}
