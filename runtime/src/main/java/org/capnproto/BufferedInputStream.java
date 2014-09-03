package org.capnproto;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public interface BufferedInputStream extends ReadableByteChannel {
    public ByteBuffer getReadBuffer();
}
