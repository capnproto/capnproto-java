package org.capnproto;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.List;
import java.util.concurrent.CompletableFuture;


public class TwoPartyVatNetwork
        implements VatNetwork<RpcTwoPartyProtocol.VatId.Reader>,
                   VatNetwork.Connection<RpcTwoPartyProtocol.VatId.Reader> {

    @Override
    public CompletableFuture<Connection<RpcTwoPartyProtocol.VatId.Reader>> baseAccept() {
        return this.accept();
    }

    public interface MessageTap {
        void incoming(IncomingRpcMessage message, RpcTwoPartyProtocol.Side side);
    }

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

    public void setTap(MessageTap tap) {
        this.tap = tap;
    }

    public Connection asConnection() {
        return this;
    }

    @Override
    public Connection connect(RpcTwoPartyProtocol.VatId.Reader vatId) {
        return vatId.getSide() != side
                ? this.asConnection()
                : null;
    }

    public CompletableFuture<Connection<RpcTwoPartyProtocol.VatId.Reader>> accept() {
        if (side == RpcTwoPartyProtocol.Side.SERVER & !accepted) {
            accepted = true;
            return CompletableFuture.completedFuture(this.asConnection());
        }
        else {
            // never completes
            return new CompletableFuture<>();
        }
    }

    public RpcTwoPartyProtocol.VatId.Reader getPeerVatId() {
        return this.peerVatId.getRoot(RpcTwoPartyProtocol.VatId.factory).asReader();
    }

    @Override
    public OutgoingRpcMessage newOutgoingMessage(int firstSegmentWordSize) {
        return new OutgoingMessage(firstSegmentWordSize);
    }

    @Override
    public CompletableFuture<IncomingRpcMessage> receiveIncomingMessage() {
        var message = Serialize.readAsync(channel)
                .thenApply(reader -> (IncomingRpcMessage) new IncomingMessage(reader))
                .whenComplete((msg, exc) -> {
                    if (exc != null) {
                        this.peerDisconnected.complete(null);
                    }
                });

        // send to message tap
        if (this.tap != null) {
            message = message.whenComplete((msg, exc) -> {
                if (this.tap == null || msg == null) {
                    return;
                }

                var side = this.getSide() == RpcTwoPartyProtocol.Side.CLIENT
                        ? RpcTwoPartyProtocol.Side.SERVER
                        : RpcTwoPartyProtocol.Side.CLIENT;

                this.tap.incoming(msg, side);
            });
        }

        return message;
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

    final class OutgoingMessage implements OutgoingRpcMessage {

        private final MessageBuilder message;
        private List<Integer> fds = List.of();

        OutgoingMessage(int firstSegmentWordSize) {
            this.message = new MessageBuilder(firstSegmentWordSize == 0
                    ? BuilderArena.SUGGESTED_FIRST_SEGMENT_WORDS
                    : firstSegmentWordSize);
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

        private final MessageReader message;
        private final List<Integer> fds;

        IncomingMessage(MessageReader message) {
            this(message, List.of());
        }

        IncomingMessage(MessageReader message, List<Integer> fds) {
            this.message = message;
            this.fds = fds;
        }

        @Override
        public AnyPointer.Reader getBody() {
            return this.message.getRoot(AnyPointer.factory);
        }

        @Override
        public List<Integer> getAttachedFds() {
            return this.fds;
        }
    }
}
