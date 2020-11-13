package org.capnproto;

import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TwoPartyServer {

    private class AcceptedConnection {
        final AsynchronousSocketChannel connection;
        final TwoPartyVatNetwork network;
        final RpcSystem<RpcTwoPartyProtocol.VatId.Reader> rpcSystem;

        AcceptedConnection(Capability.Client bootstrapInterface, AsynchronousSocketChannel connection) {
            this.connection = connection;
            this.network = new TwoPartyVatNetwork(this.connection, RpcTwoPartyProtocol.Side.SERVER);
            this.rpcSystem = new RpcSystem<>(network, bootstrapInterface);
        }
     }

    class ConnectionReceiver {
        final AsynchronousServerSocketChannel listener;

        ConnectionReceiver(AsynchronousServerSocketChannel listener) {
            this.listener = listener;
        }

        CompletableFuture<AsynchronousSocketChannel> accept() {
            CompletableFuture<AsynchronousSocketChannel> result = new CompletableFuture<>();
            this.listener.accept(null, new CompletionHandler<>() {
                @Override
                public void completed(AsynchronousSocketChannel channel, Object attachment) {
                    result.complete(channel);
                }

                @Override
                public void failed(Throwable exc, Object attachment) {
                    result.completeExceptionally(exc);
                }
            });
            return result.copy();
        }
    }

    private final Capability.Client bootstrapInterface;
    private final List<AcceptedConnection> connections = new ArrayList<>();

    public TwoPartyServer(Capability.Client bootstrapInterface) {
        this.bootstrapInterface = bootstrapInterface;
    }

    public TwoPartyServer(Capability.Server bootstrapServer) {
        this(new Capability.Client(bootstrapServer));
    }

    public void accept(AsynchronousSocketChannel channel) {
        var connection = new AcceptedConnection(this.bootstrapInterface, channel);
        this.connections.add(connection);
        connection.network.onDisconnect().whenComplete((x, exc) -> {
            this.connections.remove(connection);
        });
    }

    public CompletableFuture<java.lang.Void> listen(AsynchronousServerSocketChannel listener) {
        return this.listen(wrapListenSocket(listener));
    }

    CompletableFuture<java.lang.Void> listen(ConnectionReceiver listener) {
        return listener.accept().thenCompose(channel -> {
            this.accept(channel);
            return this.listen(listener);
        });
    }

    CompletableFuture<java.lang.Void> drain() {
        CompletableFuture<java.lang.Void> loop = CompletableFuture.completedFuture(null);
        for (var conn: this.connections) {
            loop = CompletableFuture.allOf(loop, conn.network.onDisconnect());
        }
        return loop;
    }

    ConnectionReceiver wrapListenSocket(AsynchronousServerSocketChannel channel) {
        return new ConnectionReceiver(channel);
    }
}
