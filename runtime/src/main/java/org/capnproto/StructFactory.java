package org.capnproto;

public interface StructFactory<Builder, Reader> extends StructBuilder.Factory<Builder>, StructReader.Factory<Reader>{
    public Reader asReader(Builder builder);
}
