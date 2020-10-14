package org.capnproto;

import java.nio.channels.AsynchronousByteChannel;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TwoPartyServer {

    private class AcceptedConnection {
        final AsynchronousByteChannel channel;
        final TwoPartyVatNetwork network;
        final TwoPartyRpcSystem rpcSystem;

        AcceptedConnection(Capability.Client bootstrapInterface, AsynchronousByteChannel channel) {
            this.channel = channel;
            this.network = new TwoPartyVatNetwork(channel, RpcTwoPartyProtocol.Side.SERVER);
            this.rpcSystem = new TwoPartyRpcSystem(network, bootstrapInterface);
        }

        public CompletableFuture<?> runOnce() {
            return this.rpcSystem.runOnce();
        }
    }

    private final Capability.Client bootstrapInterface;
    private final List<AcceptedConnection> connections = new ArrayList<>();
    private final List<AsynchronousServerSocketChannel> listeners = new ArrayList<>();

    public TwoPartyServer(Capability.Client bootstrapInterface) {
        this.bootstrapInterface = bootstrapInterface;
    }

    private synchronized void accept(AsynchronousByteChannel channel) {
        var connection = new AcceptedConnection(this.bootstrapInterface, channel);
        this.connections.add(connection);
    }

    public void listen(AsynchronousServerSocketChannel listener) {
        listener.accept(null, new CompletionHandler<AsynchronousSocketChannel, Object>() {
            @Override
            public void completed(AsynchronousSocketChannel channel, Object attachment) {
                accept(channel);
                listen(listener);
            }

            @Override
            public void failed(Throwable exc, Object attachment) {
                listeners.remove(listener);
            }
        });
    }

    public synchronized CompletableFuture<?> runOnce() {
        var done = new CompletableFuture<>();
        for (var conn: connections) {
            done = CompletableFuture.anyOf(done, conn.runOnce());
        }
        return done;
    }
}
