package org.capnproto.benchmark;

import java.io.IOException;

public final class Packed implements Compression {
    public void writeBuffered(org.capnproto.BufferedOutputStream writer,
                              org.capnproto.MessageBuilder message) throws IOException {
        org.capnproto.SerializePacked.write(writer, message);
        writer.flush();
    }

    public org.capnproto.MessageReader newBufferedReader(
        org.capnproto.BufferedInputStream inputStream) throws IOException {
        return org.capnproto.SerializePacked.read(inputStream);
    }
}
