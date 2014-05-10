package capnp;

import java.nio.ByteBuffer;

public class SegmentReader {
    ByteBuffer ptr;

    public SegmentReader(ByteBuffer ptr) {
        this.ptr = ptr;
    }
}
