package org.capnproto;

public class TwoPartyRpcSystem
        extends RpcSystem<RpcTwoPartyProtocol.VatId.Reader> {

    private TwoPartyVatNetwork network;

    public TwoPartyRpcSystem(TwoPartyVatNetwork network, Capability.Client bootstrapInterface) {
        super(network, bootstrapInterface);
        this.network = network;
    }

    public TwoPartyRpcSystem(TwoPartyVatNetwork network, Capability.Server bootstrapInterface) {
        super(network, new Capability.Client(bootstrapInterface));
        this.network = network;
    }

    @Override
    public VatNetwork<RpcTwoPartyProtocol.VatId.Reader> getNetwork() {
        return this.network;
    }
}
