package org.capnproto;

public interface BootstrapFactory<VatId> {
    Capability.Client createFor(VatId clientId);
}