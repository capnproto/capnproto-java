package org.capnproto;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class RpcSystem<VatId extends StructReader> {

    private final VatNetwork<VatId> network;
    private final BootstrapFactory<VatId> bootstrapFactory;
    private final Map<VatNetwork.Connection<VatId>, RpcState<VatId>> connections = new HashMap<>();

    public RpcSystem(VatNetwork<VatId> network) {
        this(network, clientId -> new Capability.Client(
                Capability.newBrokenCap("No bootstrap interface available")));
    }

    public RpcSystem(VatNetwork<VatId> network,
                     Capability.Server bootstrapInterface) {
        this(network, new Capability.Client(bootstrapInterface));
    }

    public RpcSystem(VatNetwork<VatId> network,
                     Capability.Client bootstrapInterface) {
        this(network, clientId -> bootstrapInterface);
    }

    public RpcSystem(VatNetwork<VatId> network,
                     BootstrapFactory<VatId> bootstrapFactory) {
        this.network = network;
        this.bootstrapFactory = bootstrapFactory;
    }

    public Capability.Client bootstrap(VatId vatId) {
        var connection = this.network.connect(vatId);
        if (connection != null) {
            var state = getConnectionState(connection);
            var hook = state.restore();
            return new Capability.Client(hook);
        }
        else {
            return this.bootstrapFactory.createFor(vatId);
        }
    }

    public void accept(VatNetwork.Connection<VatId> connection) {
        var state = getConnectionState(connection);
        state.runMessageLoop();
    }

    private RpcState<VatId> getConnectionState(VatNetwork.Connection<VatId> connection) {
        return this.connections.computeIfAbsent(connection, conn -> {
            var onDisconnect = new CompletableFuture<RpcState.DisconnectInfo>();
            onDisconnect.thenCompose(info -> {
                        this.connections.remove(connection);
                        return info.shutdownPromise.thenRun(connection::close);
                    });
           return new RpcState<>(this.bootstrapFactory, conn, onDisconnect);
        });
    }

    public void runOnce() {
        for (var state: this.connections.values()) {
            state.pollOnce().join();
            return;
        }
    }

    public void start() {
        this.network.accept()
                .thenAccept(this::accept)
                .thenRunAsync(this::start);
    }

    public static <VatId extends StructReader>
    RpcSystem<VatId> makeRpcClient(VatNetwork<VatId> network) {
        return new RpcSystem<>(network);
    }

    public static <VatId extends StructReader>
    RpcSystem<VatId> makeRpcServer(VatNetwork<VatId> network,
                                      BootstrapFactory<VatId> bootstrapFactory) {
        return new RpcSystem<>(network, bootstrapFactory);
    }

    public static <VatId extends StructReader>
    RpcSystem<VatId> makeRpcServer(VatNetwork<VatId> network,
                                      Capability.Client bootstrapInterface) {
        return new RpcSystem<>(network, bootstrapInterface);
    }
}
