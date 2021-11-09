package org.capnproto;

import org.junit.Assert;
import org.junit.Test;

public class StructPointerTest {
    @Test
    public void testDataSize() {
        Assert.assertEquals(
            2,
            StructPointer.dataSize(0x0001000200000000L));
    }

    @Test
    public void testDataSizeUnderflow() {
        Assert.assertEquals(
            0xffff,
            StructPointer.dataSize(0x0001ffff00000000L));
    }

    @Test
    public void testPtrCount() {
        Assert.assertEquals(
            1,
            StructPointer.ptrCount(0x0001000200000000L));
    }

    @Test
    public void testPtrCountUnderflow() {
        Assert.assertEquals(
            0xffff,
            StructPointer.ptrCount(0xffff000200000000L));
    }

    @Test
    public void testWordSize() {
        Assert.assertEquals(
            3,
            StructPointer.wordSize(0x0001000200000000L));
    }

    @Test
    public void testWordSizeUnderflow() {
        Assert.assertEquals(
            0x1fffe,
            StructPointer.wordSize(0xffffffff00000000L));
    }
}
