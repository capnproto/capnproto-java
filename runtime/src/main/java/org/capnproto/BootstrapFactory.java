package org.capnproto;

public interface BootstrapFactory<VatId> {

    FromPointerReader<VatId> getVatIdFactory();

    Capability.Client createFor(VatId clientId);
}