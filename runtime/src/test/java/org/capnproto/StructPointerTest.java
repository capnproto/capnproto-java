package org.capnproto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StructPointerTest {
    @Test
    public void testDataSize() {
        assertEquals(
            2,
            StructPointer.dataSize(0x0001000200000000L));
    }

    @Test
    public void testDataSizeUnderflow() {
        assertEquals(
            0xffff,
            StructPointer.dataSize(0x0001ffff00000000L));
    }

    @Test
    public void testPtrCount() {
        assertEquals(
            1,
            StructPointer.ptrCount(0x0001000200000000L));
    }

    @Test
    public void testPtrCountUnderflow() {
        assertEquals(
            0xffff,
            StructPointer.ptrCount(0xffff000200000000L));
    }

    @Test
    public void testWordSize() {
        assertEquals(
            3,
            StructPointer.wordSize(0x0001000200000000L));
    }

    @Test
    public void testWordSizeUnderflow() {
        assertEquals(
            0x1fffe,
            StructPointer.wordSize(0xffffffff00000000L));
    }
}
