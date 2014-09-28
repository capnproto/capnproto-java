package org.capnproto;

public final class SerializePacked {

    public static MessageReader newReader(BufferedInputStream input) throws java.io.IOException {
        PackedInputStream packedInput = new PackedInputStream(input);
        return ByteChannelMessageReader.create(packedInput);
    }

    public static MessageReader newReaderUnbuffered(java.nio.channels.ReadableByteChannel input) throws java.io.IOException {
        PackedInputStream packedInput = new PackedInputStream(new BufferedInputStreamWrapper(input));
        return ByteChannelMessageReader.create(packedInput);
    }

    public static void writeMessage(BufferedOutputStream output,
                                    MessageBuilder message) throws java.io.IOException {
        PackedOutputStream packedOutputStream = new PackedOutputStream(output);
        Serialize.writeMessage(packedOutputStream, message);
    }

    public static void writeMessageUnbuffered(java.nio.channels.WritableByteChannel output,
                                              MessageBuilder message) throws java.io.IOException {
        BufferedOutputStreamWrapper buffered = new BufferedOutputStreamWrapper(output);
        writeMessage(buffered, message);
        buffered.flush();
    }
}
