package org.capnproto;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TwoPartyVatNetwork
        implements VatNetwork<RpcTwoPartyProtocol.VatId.Reader>,
                   VatNetwork.Connection {

    public interface MessageTap {
        void incoming(IncomingRpcMessage message, RpcTwoPartyProtocol.Side side);
    }

    private static BuilderArena.AllocationStrategy allocationStrategy = BuilderArena.SUGGESTED_ALLOCATION_STRATEGY;
    private CompletableFuture<java.lang.Void> previousWrite = CompletableFuture.completedFuture(null);
    private final CompletableFuture<java.lang.Void> peerDisconnected = new CompletableFuture<>();
    private final AsynchronousSocketChannel channel;
    private final RpcTwoPartyProtocol.Side side;
    private final MessageBuilder peerVatId = new MessageBuilder(4);
    private boolean accepted;
    private MessageTap tap;

    public TwoPartyVatNetwork(AsynchronousSocketChannel channel, RpcTwoPartyProtocol.Side side) {
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

    public void setTap(MessageTap tap) {
        this.tap = tap;
    }

    public VatNetwork.Connection asConnection() {
        return this;
    }

    private Connection connect(RpcTwoPartyProtocol.VatId.Reader vatId) {
        return vatId.getSide() != side ? this : null;
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
        return Serialize.readAsync(channel)
                .thenApply(reader -> (IncomingRpcMessage) new IncomingMessage(reader))
                .whenComplete((msg, exc) -> {
                    if (exc != null) {
                        this.peerDisconnected.complete(null);
                    }
                })
                .whenComplete((msg, exc) -> {
                    if (this.tap != null && msg != null) {
                        this.tap.incoming(
                                msg,
                                this.getSide() == RpcTwoPartyProtocol.Side.CLIENT
                                        ? RpcTwoPartyProtocol.Side.SERVER
                                        : RpcTwoPartyProtocol.Side.CLIENT);
                    }
                });
    }

    @Override
    public CompletableFuture<java.lang.Void> onDisconnect() {
        return this.peerDisconnected.copy();
    }

    @Override
    public CompletableFuture<java.lang.Void> shutdown() {
        return this.previousWrite.whenComplete((x, exc) -> {
            try {
                this.channel.shutdownOutput();
            }
            catch (Exception ioExc) {
            }
        });
    }

    @Override
    public Connection baseConnect(RpcTwoPartyProtocol.VatId.Reader hostId) {
        return this.connect(hostId);
    }

    @Override
    public CompletableFuture<Connection> baseAccept() {
        return this.accept();
    }

    final class OutgoingMessage implements OutgoingRpcMessage {

        final MessageBuilder message;
        List<Integer> fds = List.of();

        OutgoingMessage(int firstSegmentWordSize) {
            this.message = new MessageBuilder(firstSegmentWordSize, allocationStrategy);
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
            previousWrite = previousWrite.thenCompose(x -> Serialize.writeAsync(channel, message));
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
