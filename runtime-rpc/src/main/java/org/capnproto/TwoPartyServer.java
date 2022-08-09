package org.capnproto;

import java.nio.channels.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TwoPartyServer {

    private class AcceptedConnection {
        private final AsynchronousByteChannel connection;
        private final TwoPartyVatNetwork network;
        private final RpcSystem<RpcTwoPartyProtocol.VatId.Reader> rpcSystem;

        AcceptedConnection(Capability.Client bootstrapInterface, AsynchronousByteChannel connection) {
            this.connection = connection;
            this.network = new TwoPartyVatNetwork(this.connection, RpcTwoPartyProtocol.Side.SERVER);
            this.rpcSystem = new RpcSystem<>(network, bootstrapInterface);
            this.rpcSystem.start();
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

    public void accept(AsynchronousByteChannel channel) {
        var connection = new AcceptedConnection(this.bootstrapInterface, channel);
        this.connections.add(connection);
        connection.network.onDisconnect().whenComplete((x, exc) -> {
            this.connections.remove(connection);
        });
    }

    public CompletableFuture<java.lang.Void> listen(AsynchronousServerSocketChannel listener) {
        var result = new CompletableFuture<AsynchronousSocketChannel>();
        listener.accept(null, new CompletionHandler<>() {
            @Override
            public void completed(AsynchronousSocketChannel channel, Object attachment) {
                accept(channel);
                result.complete(null);
            }

            @Override
            public void failed(Throwable exc, Object attachment) {
                result.completeExceptionally(exc);
            }
        });
        return result.thenCompose(void_ -> this.listen(listener));
    }

    CompletableFuture<java.lang.Void> drain() {
        CompletableFuture<java.lang.Void> loop = CompletableFuture.completedFuture(null);
        for (var conn: this.connections) {
            loop = CompletableFuture.allOf(loop, conn.network.onDisconnect());
        }
        return loop;
    }
}
