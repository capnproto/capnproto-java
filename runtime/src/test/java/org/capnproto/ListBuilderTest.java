package org.capnproto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ListBuilderTest {

    @Test
    public void _setBooleanElementShouldNotOverflowDuringPositionOffsetCalculation() {
        ByteBuffer buffer = ByteBuffer.allocate(10);
        BuilderArena builderArena = new BuilderArena(new DefaultAllocator());
        SegmentBuilder segmentBuilder = new SegmentBuilder(buffer, builderArena);
        ListBuilder listBuilder = new ListBuilder(segmentBuilder, 0, 0, 2, 0, (short) 0);

        assertThrows(IndexOutOfBoundsException.class, () -> listBuilder._setBooleanElement(Integer.MAX_VALUE, true));
    }
}