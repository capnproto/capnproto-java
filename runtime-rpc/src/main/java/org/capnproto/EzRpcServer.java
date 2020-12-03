package org.capnproto;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

public class EzRpcServer {

    private final AsynchronousChannelGroup channelgroup;
    private final AsynchronousServerSocketChannel serverAcceptSocket;
    private final TwoPartyServer twoPartyRpc;
    private final int port;

    public EzRpcServer(Capability.Server bootstrapInterface, SocketAddress address) throws IOException {
        this(new Capability.Client(bootstrapInterface), address);
    }

    public EzRpcServer(Capability.Client bootstrapInterface, SocketAddress address) throws IOException {
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
        return this.twoPartyRpc.listen(this.serverAcceptSocket);
    }
}
