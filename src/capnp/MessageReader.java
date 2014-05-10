package capnp;

import java.nio.ByteBuffer;

public class MessageReader {
    ByteBuffer[] segmentSlices;

    public MessageReader(ByteBuffer[] segmentSlices) {
        this.segmentSlices = segmentSlices;
    }

    public <T> T getRoot(FromStructReader<T> factory) {
        SegmentReader segment = new SegmentReader(this.segmentSlices[0]);
        PointerReader pointerReader = PointerReader.getRoot(segment,
                                                            new WordPointer(this.segmentSlices[0], 0),
                                                            0x7fffffff /* XXX */);
        AnyPointer.Reader any = new AnyPointer.Reader(pointerReader);
        return any.getAsStruct(factory);
    }
}
