package org.capnproto;

import java.util.ArrayList;
import java.util.List;

class BuilderCapabilityTable implements CapTableBuilder {

    private final List<ClientHook> table = new ArrayList<>();

    BuilderCapabilityTable() {
    }

    @Override
    public ClientHook extractCap(int index) {
        return table.get(index);
    }

    @Override
    public int injectCap(ClientHook cap) {
        int index = table.size();
        table.add(cap);
        return index;
    }

    @Override
    public void dropCap(int index) {
        table.set(index, null);
    }

    @Override
    public ClientHook[] getTable() {
        return table.toArray(new ClientHook[0]);
    }
}
