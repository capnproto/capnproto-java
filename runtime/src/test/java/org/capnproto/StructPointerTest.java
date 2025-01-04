package org.capnproto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StructPointerTest {
    @Test
    public void testDataSize() {
        Assertions.assertEquals(
            2,
            StructPointer.dataSize(0x0001000200000000L));
    }

    @Test
    public void testDataSizeUnderflow() {
        Assertions.assertEquals(
            0xffff,
            StructPointer.dataSize(0x0001ffff00000000L));
    }

    @Test
    public void testPtrCount() {
        Assertions.assertEquals(
            1,
            StructPointer.ptrCount(0x0001000200000000L));
    }

    @Test
    public void testPtrCountUnderflow() {
        Assertions.assertEquals(
            0xffff,
            StructPointer.ptrCount(0xffff000200000000L));
    }

    @Test
    public void testWordSize() {
        Assertions.assertEquals(
            3,
            StructPointer.wordSize(0x0001000200000000L));
    }

    @Test
    public void testWordSizeUnderflow() {
        Assertions.assertEquals(
            0x1fffe,
            StructPointer.wordSize(0xffffffff00000000L));
    }
}
