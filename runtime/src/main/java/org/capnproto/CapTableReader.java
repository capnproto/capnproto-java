package org.capnproto;

public interface CapTableReader {

    class ReaderContext {
        public CapTableReader capTable;
    }

    ClientHook extractCap(int index);
}
