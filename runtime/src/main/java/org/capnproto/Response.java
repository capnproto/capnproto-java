package org.capnproto;

class Response {

    final ResponseHook hook;
    final AnyPointer.Reader results;

    public Response(AnyPointer.Reader reader, ResponseHook hook) {
        this.hook = hook;
        this.results = reader;
    }

    public final <T> T getAs(FromPointerReader<T> factory) {
        return this.results.getAs(factory);
    }
}