package org.capnproto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class RpcSystem<VatId extends StructReader> {

    private final VatNetwork<VatId> network;
    private final BootstrapFactory<VatId> bootstrapFactory;
    private final Map<VatNetwork.Connection<VatId>, RpcState<VatId>> connections = new ConcurrentHashMap<>();

    public RpcSystem(VatNetwork<VatId> network) {
        this(network, (BootstrapFactory)null);
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
        this.startAcceptLoop();
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

    public VatNetwork<VatId> getNetwork() {
        return this.network;
    }

    RpcState<VatId> getConnectionState(VatNetwork.Connection<VatId> connection) {
        var state = this.connections.computeIfAbsent(connection, conn -> {
            var onDisconnect = new CompletableFuture<RpcState.DisconnectInfo>();
            onDisconnect.thenCompose(info -> {
                        this.connections.remove(connection);
                        return info.shutdownPromise.thenRun(() -> connection.close());
                    });
           return new RpcState<>(this.bootstrapFactory, conn, onDisconnect);
        });
        return state;
    }

    public void accept(VatNetwork.Connection<VatId> connection) {
        getConnectionState(connection);
    }

    private void startAcceptLoop() {
        this.network.accept()
                .thenAccept(this::accept)
                .thenRunAsync(this::startAcceptLoop);
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
