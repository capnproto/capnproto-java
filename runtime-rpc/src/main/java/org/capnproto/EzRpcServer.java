package org.capnproto;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

public class EzRpcServer {

    private final AsynchronousChannelGroup channelgroup;
    private final AsynchronousServerSocketChannel serverAcceptSocket;
    private final TwoPartyServer twoPartyRpc;
    private final int port;

    public EzRpcServer(Capability.Server bootstrapInterface, InetSocketAddress address) throws IOException {
        this(new Capability.Client(bootstrapInterface), address);
    }

    public EzRpcServer(Capability.Client bootstrapInterface, InetSocketAddress address) throws IOException {
        this.channelgroup = AsynchronousChannelGroup.withThreadPool(Executors.newFixedThreadPool(1));
        this.serverAcceptSocket = AsynchronousServerSocketChannel.open(this.channelgroup);
        this.serverAcceptSocket.bind(address);
        var localAddress = (InetSocketAddress) this.serverAcceptSocket.getLocalAddress();
        this.port = localAddress.getPort();
        this.twoPartyRpc = new TwoPartyServer(bootstrapInterface);
    }

    public int getPort() {
        return this.port;
    }

    public CompletableFuture<java.lang.Void> start() {
        return this.twoPartyRpc.listen(new AsynchronousByteListenChannel() {
            @Override
            public <A> void accept(A attachment, CompletionHandler<AsynchronousByteChannel, ? super A> handler) {
                serverAcceptSocket.accept(attachment, new CompletionHandler<>() {
                    @Override
                    public void completed(AsynchronousSocketChannel result, A attachment) {
                        handler.completed(result, attachment);
                    }

                    @Override
                    public void failed(Throwable exc, A attachment) {
                        handler.failed(exc, attachment);
                    }
                });
            }
        });
    }
}
