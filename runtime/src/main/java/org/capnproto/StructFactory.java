package org.capnproto;

public interface StructFactory<Builder, Reader> extends FromStructBuilder<Builder>, FromStructReader<Reader>{

    public Reader asReader(Builder builder);

}
