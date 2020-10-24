package org.capnproto;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public abstract class RpcSystem<VatId> {

    final VatNetwork<VatId> network;
    final Capability.Client bootstrapInterface;
    final Map<VatNetwork.Connection, RpcState> connections = new HashMap<>();
    final CompletableFuture<java.lang.Void> messageLoop;
    final CompletableFuture<java.lang.Void> acceptLoop;

    public RpcSystem(VatNetwork<VatId> network, Capability.Client bootstrapInterface) {
        this.network = network;
        this.bootstrapInterface = bootstrapInterface;
        this.acceptLoop = doAcceptLoop();
        this.messageLoop = doMessageLoop();
    }

    public CompletableFuture<java.lang.Void> getMessageLoop() {
        return this.messageLoop;
    }

    private CompletableFuture<java.lang.Void> getAcceptLoop() {
        return this.acceptLoop;
    }

    public void accept(VatNetwork.Connection connection) {
        getConnectionState(connection);
    }

    RpcState getConnectionState(VatNetwork.Connection connection) {

        var onDisconnect = new CompletableFuture<VatNetwork.Connection>()
                .thenAccept(lostConnection -> {
                    this.connections.remove(lostConnection);
                });

        return connections.computeIfAbsent(connection, key ->
                new RpcState(bootstrapInterface, connection, onDisconnect));
    }

    private CompletableFuture<java.lang.Void> doAcceptLoop() {
        return this.network.baseAccept().thenCompose(connection -> {
            this.accept(connection);
            return this.doAcceptLoop();
        });
    }

    private CompletableFuture<java.lang.Void> doMessageLoop() {
        var accept = this.getAcceptLoop();
        for (var conn: this.connections.values()) {
            accept = accept.acceptEither(conn.getMessageLoop(), x -> {});
        }
        return accept.thenCompose(x -> this.doMessageLoop());
    }
}
