package org.capnproto;

import java.util.concurrent.CompletableFuture;

public class Request<Params, Results> {

    Params params;
    PipelineFactory<Results> pipelineFactory;
    RequestHook hook;

    public Request(Params params,
                   PipelineFactory<Results> pipelineFactory,
                   RequestHook hook) {
        this.params = params;
        this.pipelineFactory = pipelineFactory;
        this.hook = hook;
    }

    public Params getParams() {
        return params;
    }

    public RequestHook getHook() {
        return this.hook;
    }

    public Results send() {
        var typelessPromise = this.hook.send();
        this.hook = null; // prevent reuse
        return pipelineFactory.newPipeline(typelessPromise);
    }

    static <P, R> Request<P, R> newBrokenRequest(Throwable exc) {
        final MessageBuilder message = new MessageBuilder();

        var hook = new RequestHook() {
            @Override
            public RemotePromise<AnyPointer.Reader> send() {
                return new RemotePromise<>(CompletableFuture.failedFuture(exc), null);
            }

            @Override
            public CompletableFuture<?> sendStreaming() {
                return CompletableFuture.failedFuture(exc);
            }

            @Override
            public Object getBrand() {
                return null;
            }
        };

        var root = message.getRoot(AnyPointer.factory);
        return new Request<P, R>(null, null, hook);
    }

    static <P, R> Request<P, R> fromTypeless(
            FromPointerBuilder<P> paramsFactory,
            PipelineFactory<R> pipelineFactory,
            Request<AnyPointer.Builder, AnyPointer.Pipeline> typeless) {
        return new Request<>(typeless.params.getAs(paramsFactory), pipelineFactory, typeless.hook);
    }
}
