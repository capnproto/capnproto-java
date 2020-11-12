package org.capnproto;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class RpcSystem<VatId extends StructReader> {

    private final VatNetwork<VatId> network;
    private final BootstrapFactory<VatId> bootstrapFactory;
    private final Map<VatNetwork.Connection<VatId>, RpcState<VatId>> connections = new HashMap<>();
    private final CompletableFuture<java.lang.Void> messageLoop;
    private final CompletableFuture<java.lang.Void> acceptLoop;

    public RpcSystem(VatNetwork<VatId> network) {
        this.network = network;
        this.bootstrapFactory = null;
        this.acceptLoop = new CompletableFuture<>();
        this.messageLoop = doMessageLoop();
    }

    public VatNetwork<VatId> getNetwork() {
        return this.network;
    }

    public RpcSystem(VatNetwork<VatId> network,
                     Capability.Server bootstrapInterface) {
        this(network, new Capability.Client(bootstrapInterface));
    }

    public RpcSystem(VatNetwork<VatId> network,
                     Capability.Client bootstrapInterface) {
        this(network, new BootstrapFactory<VatId>() {

            @Override
            public FromPointerReader<VatId> getVatIdFactory() {
                return this.getVatIdFactory();
            }

            @Override
            public Capability.Client createFor(VatId clientId) {
                return bootstrapInterface;
            }
        });
    }

    public RpcSystem(VatNetwork<VatId> network,
                     BootstrapFactory<VatId> bootstrapFactory) {
        this.network = network;
        this.bootstrapFactory = bootstrapFactory;
        this.acceptLoop = doAcceptLoop();
        this.messageLoop = doMessageLoop();
    }

    public Capability.Client bootstrap(VatId vatId) {
        var connection = this.getNetwork().connect(vatId);
        if (connection != null) {
            var state = getConnectionState(connection);
            var hook = state.restore();
            return new Capability.Client(hook);
        }
        else if (this.bootstrapFactory != null) {
            return this.bootstrapFactory.createFor(vatId);
        }
        else {
            return new Capability.Client(Capability.newBrokenCap("No bootstrap interface available"));
        }
    }

    RpcState<VatId> getConnectionState(VatNetwork.Connection<VatId> connection) {
        var state = this.connections.get(connection);
        if (state == null) {
            var onDisconnect = new CompletableFuture<RpcState.DisconnectInfo>()
                    .whenComplete((info, exc) -> {
                        this.connections.remove(connection);
                        try {
                            connection.close();
                        } catch (IOException ignored) {
                        }
                    });

            state = new RpcState<>(this.bootstrapFactory, connection, onDisconnect);
            this.connections.put(connection, state);
        }
        return state;
    }

    public void accept(VatNetwork.Connection<VatId> connection) {
        getConnectionState(connection);
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

    public CompletableFuture<java.lang.Void> getMessageLoop() {
        return this.messageLoop;
    }

    private CompletableFuture<java.lang.Void> getAcceptLoop() {
        return this.acceptLoop;
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
