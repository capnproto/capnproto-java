package org.capnproto;

import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

public interface BufferedWritableByteChannel extends WritableByteChannel {
    public ByteBuffer getWriteBuffer();
}
