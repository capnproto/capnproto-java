package org.capnproto;

import java.nio.channels.AsynchronousByteChannel;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TwoPartyVatNetwork implements VatNetwork, VatNetwork.Connection {

    private CompletableFuture<?> writeCompleted = CompletableFuture.completedFuture(null);
    private final AsynchronousByteChannel channel;

    public TwoPartyVatNetwork(AsynchronousByteChannel channel) {
        this.channel = channel;
    }

    @Override
    public OutgoingRpcMessage newOutgoingMessage(int firstSegmentWordSize) {
        return new OutgoingMessage(firstSegmentWordSize);
    }

    @Override
    public CompletableFuture<IncomingRpcMessage> receiveIncomingMessage() {
        return Serialize.readAsync(channel).thenApply(message -> {
            return new IncomingMessage(message);
        });
    }

    final class OutgoingMessage implements OutgoingRpcMessage {

        final MessageBuilder message;
        List<Integer> fds = List.of();

        OutgoingMessage(int firstSegmentWordSize) {
            this.message = new MessageBuilder(firstSegmentWordSize);
        }

        @Override
        public AnyPointer.Builder getBody() {
            return message.getRoot(AnyPointer.factory);
        }

        @Override
        public void setFds(List<Integer> fds) {
            this.fds = fds;
        }

        @Override
        public void send() {
            writeCompleted = writeCompleted.thenCompose(
                    x -> Serialize.writeAsync(channel, message)
            );
        }

        @Override
        public int sizeInWords() {
            int size = 0;
            for (var segment: message.getSegmentsForOutput()) {
                size += segment.position();
            }
            return size / 2;
        }
    }

    final class IncomingMessage implements IncomingRpcMessage {

        final MessageReader message;
        final List<Integer> fds;

        IncomingMessage(MessageReader message) {
            this(message, List.of());
        }

        IncomingMessage(MessageReader message, List<Integer> fds) {
            this.message = message;
            this.fds = fds;
        }

        @Override
        public AnyPointer.Reader getBody() {
            return message.getRoot(AnyPointer.factory);
        }

        @Override
        public List<Integer> getAttachedFds() {
            return fds;
        }
    }
}
