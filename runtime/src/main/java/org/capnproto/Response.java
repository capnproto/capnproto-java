package org.capnproto;

public final class Response<Results> {

    private final Results results;
    private final ResponseHook hook;

    public Response(Results results,
                    ResponseHook hook) {
        this.results = results;
        this.hook = hook;
    }

    public Results getResults() {
        return this.results;
    }

    public ResponseHook getHook() {
        return this.hook;
    }

    static <R> Response<R> fromTypeless(FromPointerReader<R> resultsFactory,
                                        Response<AnyPointer.Reader> typeless) {
        return new Response<>(typeless.getResults().getAs(resultsFactory), typeless.hook);

    }
}
