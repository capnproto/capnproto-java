package org.capnproto;

import java.nio.channels.AsynchronousByteChannel;
import java.util.concurrent.CompletableFuture;

public class TwoPartyClient {

    private final TwoPartyVatNetwork network;
    private final TwoPartyRpcSystem rpcSystem;

    public TwoPartyClient(AsynchronousByteChannel channel) {
        this(channel, null);
    }

    public TwoPartyClient(AsynchronousByteChannel channel, Capability.Client bootstrapInterface) {
        this(channel, bootstrapInterface, RpcTwoPartyProtocol.Side.CLIENT);
    }

    public TwoPartyClient(AsynchronousByteChannel channel,
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

    public synchronized CompletableFuture<?> runOnce() {
        return this.rpcSystem.runOnce();
    }
}
