package org.capnproto;

class Response<Results> {

    final FromPointerReader<Results> factory;
    final ResponseHook hook;
    final AnyPointer.Reader results;

    public Response(FromPointerReader<Results> factory,
                    AnyPointer.Reader reader,
                    ResponseHook hook) {
        this.factory = factory;
        this.hook = hook;
        this.results = reader;
    }

    public final Results getResults() {
        return this.results.getAs(factory);
    }
}