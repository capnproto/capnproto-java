package org.capnproto;

import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TwoPartyServer {

    private class AcceptedConnection {
        final AsynchronousSocketChannel channel;
        final TwoPartyVatNetwork network;
        final RpcSystem<RpcTwoPartyProtocol.VatId.Reader> rpcSystem;
        private final CompletableFuture<?> messageLoop;

        AcceptedConnection(Capability.Client bootstrapInterface, AsynchronousSocketChannel channel) {
            this.channel = channel;
            this.network = new TwoPartyVatNetwork(channel, RpcTwoPartyProtocol.Side.SERVER);
            this.rpcSystem = new RpcSystem<>(network, bootstrapInterface);
            this.messageLoop = this.rpcSystem.getMessageLoop().exceptionally(exc -> {
                connections.remove(this);
                return null;
            });
        }

        public CompletableFuture<?> getMessageLoop() {
            return this.messageLoop;
        }
     }

    class ConnectionReceiver {
        AsynchronousServerSocketChannel listener;
        final CompletableFuture<?> messageLoop;
        public ConnectionReceiver(AsynchronousServerSocketChannel listener) {
            this.listener = listener;
            this.messageLoop = doMessageLoop();
        }

        public CompletableFuture<?> getMessageLoop() {
            return this.messageLoop;
        }

        private CompletableFuture<?> doMessageLoop() {
            final var accepted = new CompletableFuture<AsynchronousSocketChannel>();
            listener.accept(null, new CompletionHandler<>() {

                @Override
                public void completed(AsynchronousSocketChannel channel, Object attachment) {
                    accepted.complete(channel);
                }

                @Override
                public void failed(Throwable exc, Object attachment) {
                    accepted.completeExceptionally(exc);
                }
            });
            return accepted.thenCompose(channel -> CompletableFuture.allOf(
                    accept(channel),
                    doMessageLoop()));
        }
    }

    private final Capability.Client bootstrapInterface;
    private final List<AcceptedConnection> connections = new ArrayList<>();
    private final List<ConnectionReceiver> listeners = new ArrayList<>();
    private final CompletableFuture<?> messageLoop;

    public TwoPartyServer(Capability.Client bootstrapInterface) {
        this.bootstrapInterface = bootstrapInterface;
        this.messageLoop = doMessageLoop();
    }

    public TwoPartyServer(Capability.Server bootstrapServer) {
        this(new Capability.Client(bootstrapServer));
    }

    private CompletableFuture<?> getMessageLoop() {
        return this.messageLoop;
    }

    public CompletableFuture<?> drain() {
        CompletableFuture<java.lang.Void> done = new CompletableFuture<>();
        for (var conn: this.connections) {
            done = CompletableFuture.allOf(done, conn.getMessageLoop());
        }
        return done;
    }

    private CompletableFuture<java.lang.Void> accept(AsynchronousSocketChannel channel) {
        var connection = new AcceptedConnection(this.bootstrapInterface, channel);
        this.connections.add(connection);
        return connection.network.onDisconnect().whenComplete((x, exc) -> {
            this.connections.remove(connection);
        });
    }
/*
    private final CompletableFuture<?> acceptLoop(AsynchronousServerSocketChannel listener) {
        final var accepted = new CompletableFuture<AsynchronousSocketChannel>();
        listener.accept(null, new CompletionHandler<>() {

            @Override
            public void completed(AsynchronousSocketChannel channel, Object attachment) {
                accepted.complete(channel);
            }

            @Override
            public void failed(Throwable exc, Object attachment) {
                accepted.completeExceptionally(exc);
            }
        });
        return accepted.thenCompose(channel -> CompletableFuture.anyOf(
                accept(channel),
                acceptLoop(listener)));
    }
*/
    public CompletableFuture<?> listen(AsynchronousServerSocketChannel listener) {
        var receiver = new ConnectionReceiver(listener);
        this.listeners.add(receiver);
        return receiver.getMessageLoop();
    }

    private CompletableFuture<?> doMessageLoop() {
        var done = new CompletableFuture<>();
        for (var conn: this.connections) {
            done = CompletableFuture.anyOf(done, conn.getMessageLoop());
        }
        for (var listener: this.listeners) {
            done = CompletableFuture.anyOf(done, listener.getMessageLoop());
        }
        return done.thenCompose(x -> doMessageLoop());
    }

    /*
    public CompletableFuture<?> runOnce() {
        var done = new CompletableFuture<>();
        for (var conn: connections) {
            done = CompletableFuture.anyOf(done, conn.runOnce());
        }
        return done;
    }
     */
}
