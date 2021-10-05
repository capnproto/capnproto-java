package org.capnproto;

import org.junit.Assert;
import org.junit.Test;

public class StructPointerTest {

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
