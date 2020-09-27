package org.capnproto;

interface CapTableBuilder extends CapTableReader {
    int injectCap(ClientHook cap);

    void dropCap(int index);

    default ClientHook[] getTable() {
        return new ClientHook[0];
    }
}
