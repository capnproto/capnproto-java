package org.capnproto;

import java.io.FileDescriptor;
import java.nio.channels.AsynchronousByteChannel;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TwoPartyVatNetwork
        implements VatNetwork<RpcTwoPartyProtocol.VatId.Reader>,
                   VatNetwork.Connection<RpcTwoPartyProtocol.VatId.Reader> {

    private CompletableFuture<java.lang.Void> previousWrite = CompletableFuture.completedFuture(null);
    private final CompletableFuture<java.lang.Void> disconnectPromise = new CompletableFuture<>();
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

    @Override
    public void close() {
        try {
            this.channel.close();
            this.disconnectPromise.complete(null);
        }
        catch (Exception exc) {
            this.disconnectPromise.completeExceptionally(exc);
        }
    }

    @Override
    public String toString() {
        return this.side.toString();
    }

    @Override
    public Connection<RpcTwoPartyProtocol.VatId.Reader> connect(RpcTwoPartyProtocol.VatId.Reader vatId) {
        return vatId.getSide() != side
                ? this.asConnection()
                : null;
    }

    @Override
    public RpcTwoPartyProtocol.VatId.Reader getPeerVatId() {
        return this.peerVatId.getRoot(RpcTwoPartyProtocol.VatId.factory).asReader();
    }

    @Override
    public OutgoingRpcMessage newOutgoingMessage(int firstSegmentWordSize) {
        return new OutgoingMessage(firstSegmentWordSize);
    }

    @Override
    public CompletableFuture<IncomingRpcMessage> receiveIncomingMessage() {
        return Serialize.readAsync(channel)
                .thenApply(reader -> (IncomingRpcMessage) new IncomingMessage(reader))
                .exceptionally(exc -> null);
    }

    @Override
    public CompletableFuture<java.lang.Void> shutdown() {
        assert this.previousWrite != null: "Already shut down";

        var result = this.previousWrite.whenComplete((void_, exc) -> {
            try {
                this.channel.shutdownOutput();
            }
            catch (Exception ignored) {
            }
        });

        this.previousWrite = null;
        return result;
    }

    public RpcTwoPartyProtocol.Side getSide() {
        return side;
    }

    public Connection<RpcTwoPartyProtocol.VatId.Reader> asConnection() {
        return this;
    }

    public CompletableFuture<java.lang.Void> onDisconnect() {
        return this.disconnectPromise.copy();
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

    private synchronized void write(MessageBuilder message) {
        this.previousWrite = this.previousWrite.thenCompose(void_ -> Serialize.writeAsync(channel, message));
    }

    final class OutgoingMessage implements OutgoingRpcMessage {

        private final MessageBuilder message;
        private List<FileDescriptor> fds = List.of();

        OutgoingMessage(int firstSegmentWordSize) {
            this.message = new MessageBuilder(firstSegmentWordSize);
        }

        @Override
        public AnyPointer.Builder getBody() {
            return message.getRoot(AnyPointer.factory);
        }

        @Override
        public void setFds(List<FileDescriptor> fds) {
            this.fds = fds;
        }

        @Override
        public void send() {
            write(message);
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

    static final class IncomingMessage implements IncomingRpcMessage {

        private final MessageReader message;
        private final List<FileDescriptor> fds;

        IncomingMessage(MessageReader message) {
            this(message, List.of());
        }

        IncomingMessage(MessageReader message, List<FileDescriptor> fds) {
            this.message = message;
            this.fds = fds;
        }

        @Override
        public AnyPointer.Reader getBody() {
            return this.message.getRoot(AnyPointer.factory);
        }

        @Override
        public List<FileDescriptor> getAttachedFds() {
            return this.fds;
        }
    }
}
