package org.capnproto.benchmark;

import java.io.IOException;

public final class Uncompressed implements Compression {
    public final void writeBuffered(org.capnproto.BufferedOutputStream writer,
                              org.capnproto.MessageBuilder message) throws IOException {
        org.capnproto.Serialize.write(writer, message);
        writer.flush();
    }

    public final org.capnproto.MessageReader newBufferedReader(
        org.capnproto.BufferedInputStream inputStream) throws IOException {
        return org.capnproto.Serialize.read(inputStream);
    }
}
