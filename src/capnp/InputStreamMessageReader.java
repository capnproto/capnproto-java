package capnp;

import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class InputStreamMessageReader {
    static MessageReader create(InputStream is) throws IOException {
        byte[] firstWord = new byte[8];

        int bytesRead = 0;
        while (bytesRead < 8) {
            int n = is.read(firstWord, bytesRead, 8 - bytesRead);
            if (n < 0) {
                throw new IOException("premature EOF");
            }
            bytesRead += n;
        }

        ByteBuffer.wrap(firstWord);


        throw new Error();
    }

}
