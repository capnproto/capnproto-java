package org.capnproto;

import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Vector;


public class InputStreamMessageReader {

    static byte[] readExact(InputStream is, int length) throws IOException {
        byte[] bytes = new byte[length];

        int bytesRead = 0;
        while (bytesRead < length) {
            int r = is.read(bytes, bytesRead, length - bytesRead);
            if (r < 0) {
                throw new IOException("premature EOF");
            }
            bytesRead += r;
        }

        return bytes;
    }

    static ByteBuffer makeByteBuffer(byte[] bytes) {
        ByteBuffer result = ByteBuffer.wrap(bytes);
        result.order(ByteOrder.LITTLE_ENDIAN);
        result.mark();
        return result;
    }

    public static MessageReader create(InputStream is) throws IOException {
        ByteBuffer firstWord = makeByteBuffer(readExact(is, 8));

        int segmentCount = 1 + firstWord.getInt(0);

        int segment0Size = 0;
        if (segmentCount > 0) {
            segment0Size = firstWord.getInt(4);
        }

        int totalWords = segment0Size;

        if (segmentCount > 512) {
            throw new IOException("too many segments");
        }

        // in words
        Vector<Integer> moreSizes = new Vector<Integer>();

        if (segmentCount > 1) {
            ByteBuffer moreSizesRaw = makeByteBuffer(readExact(is, 4 * (segmentCount & ~1)));
            for(int ii = 0; ii < segmentCount - 1; ++ii) {
                int size = moreSizesRaw.getInt(ii * 4);
                moreSizes.add(size);
                totalWords += size;
            }
        }

        // TODO check that totalWords is reasonable

        byte[] allSegments = readExact(is, totalWords * 8);

        ByteBuffer[] segmentSlices = new ByteBuffer[segmentCount];

        segmentSlices[0] = ByteBuffer.wrap(allSegments, 0, segment0Size * 8);
        segmentSlices[0].order(ByteOrder.LITTLE_ENDIAN);
        segmentSlices[0].mark();

        int offset = segment0Size;

        for (int ii = 1; ii < segmentCount; ++ii) {
            segmentSlices[ii] = ByteBuffer.wrap(allSegments, offset * 8, moreSizes.get(ii - 1) * 8);
            segmentSlices[ii].order(ByteOrder.LITTLE_ENDIAN);
            segmentSlices[ii].mark();
            offset += moreSizes.get(ii - 1);
        }

        return new MessageReader(segmentSlices);
    }

}
