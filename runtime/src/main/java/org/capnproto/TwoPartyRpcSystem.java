package org.capnproto;

public class TwoPartyRpcSystem
        extends RpcSystem<TwoPartyVatNetwork> {

    public TwoPartyRpcSystem(TwoPartyVatNetwork network, Capability.Client bootstrapInterface) {
        super(network, bootstrapInterface);
    }

    public Capability.Client bootstrap(RpcTwoPartyProtocol.VatId.Reader vatId) {
        var connection = this.network.baseConnect(vatId);
        var state = getConnectionState(connection);
        var hook = state.restore();
        return new Capability.Client(hook);
    }
}
