package org.capnproto;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.CompletableFuture;

public class TwoPartyClient {

    private final TwoPartyVatNetwork network;
    private final TwoPartyRpcSystem rpcSystem;

    public TwoPartyClient(AsynchronousSocketChannel channel) {
        this(channel, null);
    }

    public TwoPartyClient(AsynchronousSocketChannel channel, Capability.Client bootstrapInterface) {
        this(channel, bootstrapInterface, RpcTwoPartyProtocol.Side.CLIENT);
    }

    public TwoPartyClient(AsynchronousSocketChannel channel,
                          Capability.Client bootstrapInterface,
                          RpcTwoPartyProtocol.Side side) {
        this.network = new TwoPartyVatNetwork(channel, side);
        this.rpcSystem = new TwoPartyRpcSystem(network, bootstrapInterface);
    }

    public Capability.Client bootstrap() {
        var message = new MessageBuilder();
        var vatId = message.getRoot(RpcTwoPartyProtocol.VatId.factory);
        vatId.setSide(network.getSide() == RpcTwoPartyProtocol.Side.CLIENT
                ? RpcTwoPartyProtocol.Side.SERVER
                : RpcTwoPartyProtocol.Side.CLIENT);
        return rpcSystem.bootstrap(vatId.asReader());
    }

    public CompletableFuture<java.lang.Void> onDisconnect() {
        return this.network.onDisconnect();
    }

    /*
    public CompletableFuture<?> runOnce() {
        return this.rpcSystem.runOnce();
    }
     */
}
