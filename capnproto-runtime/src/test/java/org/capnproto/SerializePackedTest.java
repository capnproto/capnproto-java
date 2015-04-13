package org.capnproto;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class SerializePackedTest {

    @Test
    public void testSimplePacking() {
        assertPacksTo(new byte[0], new byte[0]);

        assertPacksTo(new byte[]{0,0,0,0,0,0,0,0}, new byte[]{0,0});

        assertPacksTo(new byte[]{0,0,12,0,0,34,0,0}, new byte[]{0x24,12,34});

        assertPacksTo(new byte[]{1,3,2,4,5,7,6,8}, new byte[]{(byte)0xff,1,3,2,4,5,7,6,8,0});

        assertPacksTo(new byte[]{0,0,0,0,0,0,0,0, 1,3,2,4,5,7,6,8},
                new byte[]{0,0,(byte)0xff,1,3,2,4,5,7,6,8,0});

        assertPacksTo(new byte[]{0,0,12,0,0,34,0,0, 1,3,2,4,5,7,6,8},
                new byte[]{0x24, 12, 34, (byte)0xff,1,3,2,4,5,7,6,8,0});

        assertPacksTo(new byte[]{1,3,2,4,5,7,6,8, 8,6,7,4,5,2,3,1},
                new byte[]{(byte)0xff,1,3,2,4,5,7,6,8,1,8,6,7,4,5,2,3,1});

        assertPacksTo(new byte[]{1,2,3,4,5,6,7,8, 1,2,3,4,5,6,7,8, 1,2,3,4,5,6,7,8, 1,2,3,4,5,6,7,8, 0,2,4,0,9,0,5,1},
                new byte[]{(byte)0xff,1,2,3,4,5,6,7,8, 3, 1,2,3,4,5,6,7,8, 1,2,3,4,5,6,7,8, 1,2,3,4,5,6,7,8,
                        (byte)0xd6,2,4,9,5,1});

        assertPacksTo(new byte[]{1,2,3,4,5,6,7,8, 1,2,3,4,5,6,7,8, 6,2,4,3,9,0,5,1, 1,2,3,4,5,6,7,8, 0,2,4,0,9,0,5,1},
                new byte[]{(byte)0xff,1,2,3,4,5,6,7,8, 3, 1,2,3,4,5,6,7,8, 6,2,4,3,9,0,5,1, 1,2,3,4,5,6,7,8,
                        (byte)0xd6,2,4,9,5,1});

        assertPacksTo(new byte[]{8,0,100,6,0,1,1,2, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0, 0,0,1,0,2,0,3,1},
                new byte[]{(byte)0xed,8,100,6,1,1,2, 0,2, (byte)0xd4,1,2,3,1});

        assertPacksTo(new byte[]{0,0,0,0,2,0,0,0, 0,0,0,0,0,0,1,0, 0,0,0,0,0,0,0,0},
                new byte[]{0x10,2, 0x40,1, 0,0});
    }

    private void assertPacksTo(byte[] unpacked, byte[] packed) {
        {
            byte[] bytes = new byte[packed.length];
            ArrayOutputStream writer = new ArrayOutputStream(ByteBuffer.wrap(bytes));
            PackedOutputStream packedOutputStream = new PackedOutputStream(writer);
            try {
                packedOutputStream.write(ByteBuffer.wrap(unpacked));
            } catch (IOException e) {
                Assert.fail("Failed writing to PackedOutputStream");
            }

            Assert.assertTrue(Arrays.equals(bytes, packed));
        }

        {
            ArrayInputStream reader = new ArrayInputStream(ByteBuffer.wrap(packed));
            PackedInputStream stream = new PackedInputStream(reader);
            byte[] bytes = new byte[unpacked.length];
            int n = 0;
            try {
                n = stream.read(ByteBuffer.wrap(bytes));
            } catch (IOException e) {
                Assert.fail("Failed reading from PackedInputStream");
            }

            Assert.assertEquals(n, unpacked.length);
            Assert.assertTrue(Arrays.equals(bytes, unpacked));
        }
    }
}
