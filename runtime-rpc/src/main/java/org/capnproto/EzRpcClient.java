package org.capnproto;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class EzRpcClient {

    private final AsynchronousSocketChannel socket;
    private final TwoPartyClient twoPartyRpc;
    private final Capability.Client client;

    public EzRpcClient(SocketAddress address) throws Exception {
        this.socket = AsynchronousSocketChannel.open();

        var connected = new CompletableFuture<Void>();

        this.socket.connect(address, null, new CompletionHandler<>() {

            @Override
            public void completed(java.lang.Void result, Object attachment) {
                connected.complete(null);
            }

            @Override
            public void failed(Throwable exc, Object attachment) {
                connected.completeExceptionally(exc);
            }
        });

        this.twoPartyRpc = new TwoPartyClient(socket);
        this.client = new Capability.Client(connected.thenApply(void_ -> this.twoPartyRpc.bootstrap()));
    }

    public Capability.Client getMain() {
        return this.client;
    }

    public <T> CompletableFuture<T> runUntil(CompletableFuture<T> done) {
        return this.twoPartyRpc.runUntil(done);
    }
}
