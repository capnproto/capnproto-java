package org.capnproto;

import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

public interface BufferedOutputStream extends WritableByteChannel {
    public ByteBuffer getWriteBuffer();
}
