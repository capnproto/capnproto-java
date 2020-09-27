package org.capnproto;

import java.util.List;

class ReaderCapabilityTable implements CapTableReader {

    final List<ClientHook> table;

    ReaderCapabilityTable(List<ClientHook> table) {
        this.table = table;
    }

    @Override
    public ClientHook extractCap(int index) {
        return index < table.size() ? table.get(index) : null;
    }
}
