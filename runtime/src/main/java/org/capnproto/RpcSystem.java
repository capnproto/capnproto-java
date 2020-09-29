package org.capnproto;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public abstract class RpcSystem<Network extends VatNetwork> {

    final Network network;
    final Capability.Client bootstrapInterface;
    final Map<VatNetwork.Connection, RpcState> connections = new HashMap<>();
    CompletableFuture<?> acceptCompleted = CompletableFuture.completedFuture(null);

    public RpcSystem(Network network, Capability.Client bootstrapInterface) {
        this.network = network;
        this.bootstrapInterface = bootstrapInterface;
    }

    public void accept(VatNetwork.Connection connection) {
        getConnectionState(connection);
    }

    synchronized RpcState getConnectionState(VatNetwork.Connection connection) {
        return connections.computeIfAbsent(connection, key ->
                new RpcState(key, bootstrapInterface));
    }

    public final CompletableFuture<?> runOnce() {
        var done = acceptLoop();
        for (var conn : connections.values()) {
            done = CompletableFuture.anyOf(done, conn.runOnce());
        }
        return done;
    }


    CompletableFuture<?> acceptLoop() {
        if (this.acceptCompleted.isDone()) {
            CompletableFuture<VatNetwork.Connection> accepted = this.network.baseAccept();
            this.acceptCompleted = accepted.thenAccept(this::accept);
        }
        return this.acceptCompleted;
    }
}
