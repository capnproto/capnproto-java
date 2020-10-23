package org.capnproto;

public class Response<Results> {

    private Results results;
    private ResponseHook hook;

    public Response(Results results,
                    ResponseHook hook) {
        this.results = results;
        this.hook = hook;
    }

    public Results getResults() {
        return this.results;
    }

    static <R> Response<R> fromTypeless(FromPointerReader<R> resultsFactory,
                                        Response<AnyPointer.Reader> typeless) {
        return new Response<>(typeless.getResults().getAs(resultsFactory), typeless.hook);

    }
}
