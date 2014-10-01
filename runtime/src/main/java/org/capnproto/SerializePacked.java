package org.capnproto;

public final class SerializePacked {

    public static MessageReader read(BufferedInputStream input) throws java.io.IOException {
        PackedInputStream packedInput = new PackedInputStream(input);
        return Serialize.read(packedInput);
    }

    public static MessageReader readUnbuffered(java.nio.channels.ReadableByteChannel input) throws java.io.IOException {
        PackedInputStream packedInput = new PackedInputStream(new BufferedInputStreamWrapper(input));
        return Serialize.read(packedInput);
    }

    public static void write(BufferedOutputStream output,
                             MessageBuilder message) throws java.io.IOException {
        PackedOutputStream packedOutputStream = new PackedOutputStream(output);
        Serialize.write(packedOutputStream, message);
    }

    public static void writeUnbuffered(java.nio.channels.WritableByteChannel output,
                                       MessageBuilder message) throws java.io.IOException {
        BufferedOutputStreamWrapper buffered = new BufferedOutputStreamWrapper(output);
        write(buffered, message);
        buffered.flush();
    }
}
