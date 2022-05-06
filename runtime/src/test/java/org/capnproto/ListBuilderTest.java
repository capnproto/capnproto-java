package org.capnproto;

import org.junit.Test;

import java.nio.ByteBuffer;

public class ListBuilderTest {

    @Test(expected = IndexOutOfBoundsException.class)
    public void _setBooleanElementShouldNotOverflowDuringPositionOffsetCalculation() {
        ByteBuffer buffer = ByteBuffer.allocate(10);
        BuilderArena builderArena = new BuilderArena(new DefaultAllocator());
        SegmentBuilder segmentBuilder = new SegmentBuilder(buffer, builderArena);
        ListBuilder listBuilder = new ListBuilder(segmentBuilder, 0, 0, 2, 0, (short) 0);

        listBuilder._setBooleanElement(Integer.MAX_VALUE, true);
    }
}