package org.capnproto;

/**
 * Collection of unsafe functions for handling Capnp data.
 */
public class UnsafeOperations {

    /**
     * Converts a {@link MessageReader} to a {@link MessageBuilder} without copying the underlying data.
     * This does not check if the data in the Reader is valid and should only be executed on trusted data.
     * Changes made to the builder are immediately visible to the Reader.
     * It is discouraged to use the Reader and Builder concurrently.
     *
     * @param reader The MessageReader
     *
     * @return the MessageBuilder, sharing it's data with the Reader.
     */
    public MessageBuilder convertTo(MessageReader reader) {
        return new MessageBuilder(new BuilderArena(reader.arena));
    }
}
