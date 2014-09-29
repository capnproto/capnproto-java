package org.capnproto.benchmark;

import java.io.IOException;

public interface Compression {
    public void writeBuffered(org.capnproto.BufferedOutputStream writer,
                              org.capnproto.MessageBuilder message) throws IOException;

    public org.capnproto.MessageReader newBufferedReader(
        org.capnproto.BufferedInputStream inputStream) throws IOException;

    public final Compression PACKED = new Packed();
    public final Compression UNCOMPRESSED = new Uncompressed();
}

