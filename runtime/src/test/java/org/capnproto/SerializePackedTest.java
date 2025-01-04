package org.capnproto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

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

        assertPacksTo(new byte[8 * 200], new byte[]{0, (byte)199});

        byte[] ones = new byte[8 * 200];
        Arrays.fill(ones, (byte)1);
        byte[] packedOnes = new byte[10 + 8 * 199];
        Arrays.fill(packedOnes, (byte)1);
        packedOnes[0] = (byte)255;
        packedOnes[9] = (byte)199;
        assertPacksTo(ones,packedOnes);
    }

    private void assertPacksTo(byte[] unpacked, byte[] packed) {
        {
            byte[] bytes = new byte[packed.length];
            ArrayOutputStream writer = new ArrayOutputStream(ByteBuffer.wrap(bytes));
            PackedOutputStream packedOutputStream = new PackedOutputStream(writer);
            try {
                packedOutputStream.write(ByteBuffer.wrap(unpacked));
            } catch (IOException e) {
                Assertions.fail("Failed writing to PackedOutputStream");
            }

            Assertions.assertTrue(Arrays.equals(bytes, packed));
        }

        {
            ArrayInputStream reader = new ArrayInputStream(ByteBuffer.wrap(packed));
            PackedInputStream stream = new PackedInputStream(reader);
            byte[] bytes = new byte[unpacked.length];
            int n = 0;
            try {
                n = stream.read(ByteBuffer.wrap(bytes));
            } catch (IOException e) {
                Assertions.fail("Failed reading from PackedInputStream");
            }

            Assertions.assertEquals(n, unpacked.length);
            Assertions.assertTrue(Arrays.equals(bytes, unpacked));
        }
    }

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void read_shouldThrowDecodingExceptionOnEmptyArrayInputStream() throws IOException {
        byte[] emptyByteArray = {};
        Assertions.assertThrows(DecodeException.class, () -> SerializePacked.read(new ArrayInputStream(ByteBuffer.wrap(emptyByteArray)), ReaderOptions.DEFAULT_READER_OPTIONS));
    }

    @Test
    @Timeout(value = 1000, unit = TimeUnit.MILLISECONDS)
    public void read_shouldThrowDecodingExceptionWhenTryingToReadMoreThanAvailableFromArrayInputStream() throws IOException {
        byte[] bytes = {17, 0, 127, 0, 0, 0, 0}; //segment0 size of 127 words, which is way larger than the tiny 7 byte input
        Assertions.assertThrows(DecodeException.class, () -> SerializePacked.read(new ArrayInputStream(ByteBuffer.wrap(bytes)), ReaderOptions.DEFAULT_READER_OPTIONS));
    }
}
