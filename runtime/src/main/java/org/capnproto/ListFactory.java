package org.capnproto;

public interface ListFactory<Builder, Reader> extends ListBuilder.Factory<Builder>, FromPointerBuilder<Builder>,
                                              ListReader.Factory<Reader>, FromPointerReader<Reader> {

}
