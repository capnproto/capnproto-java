package org.capnproto;

import java.nio.channels.AsynchronousByteChannel;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TwoPartyVatNetwork
        implements VatNetwork<RpcTwoPartyProtocol.VatId.Reader>, VatNetwork.Connection {

    private CompletableFuture<?> writeCompleted = CompletableFuture.completedFuture(null);
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final AsynchronousByteChannel channel;
    private final RpcTwoPartyProtocol.Side side;
    private final MessageBuilder peerVatId = new MessageBuilder(4);
    private boolean accepted;

    public TwoPartyVatNetwork(AsynchronousByteChannel channel, RpcTwoPartyProtocol.Side side) {
        this.channel = channel;
        this.side = side;
        this.peerVatId.initRoot(RpcTwoPartyProtocol.VatId.factory).setSide(
                side == RpcTwoPartyProtocol.Side.CLIENT
                        ? RpcTwoPartyProtocol.Side.SERVER
                        : RpcTwoPartyProtocol.Side.CLIENT);
    }

    public RpcTwoPartyProtocol.Side getSide() {
        return side;
    }

    public RpcTwoPartyProtocol.VatId.Reader getPeerVatId() {
        return peerVatId.getRoot(RpcTwoPartyProtocol.VatId.factory).asReader();
    }

    private Connection connect(RpcTwoPartyProtocol.VatId.Reader vatId) {
        if (vatId.getSide() != side) {
            return this;
        }
        return null;
    }

    private CompletableFuture<Connection> accept() {
        if (side == RpcTwoPartyProtocol.Side.SERVER & !accepted) {
            accepted = true;
            return CompletableFuture.completedFuture(this);
        }
        else {
            // never completes
            return new CompletableFuture<>();
        }
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

    @Override
    public Connection baseConnect(RpcTwoPartyProtocol.VatId.Reader hostId) {
        return this.connect(hostId);
    }

    @Override
    public CompletableFuture<Connection> baseAccept() {
        return this.accept().thenApply(conn -> conn);
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
