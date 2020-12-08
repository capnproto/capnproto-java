package org.capnproto;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.CompletableFuture;

public class EzRpcClient {

    private final TwoPartyClient twoPartyRpc;
    private final Capability.Client client;

    public EzRpcClient(AsynchronousSocketChannel socket) {
        this.twoPartyRpc = new TwoPartyClient(socket);
        this.client = this.twoPartyRpc.bootstrap();
    }

    public Capability.Client getMain() {
        return this.client;
    }

    public <T> CompletableFuture<T> runUntil(CompletableFuture<T> done) {
        return this.twoPartyRpc.runUntil(done);
    }
}
