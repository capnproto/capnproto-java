package org.capnproto;

import java.util.concurrent.CompletableFuture;

public interface Request<Params>
    extends RequestBase<Params> {

    RequestBase<Params> getBaseRequest();

    default FromPointerBuilder<Params> getParamsFactory() {
        return getBaseRequest().getParamsFactory();
    }

    default RequestBase<AnyPointer.Builder> getTypelessRequest() {
        return getBaseRequest().getTypelessRequest();
    }

    static <Params> Request<Params> newBrokenRequest(FromPointerBuilder<Params> paramsFactory,
                                                     Throwable exc) {

        var message = new MessageBuilder();

        var hook = new RequestHook() {
            @Override
            public RemotePromise<AnyPointer.Reader> send() {
                return new RemotePromise<>(CompletableFuture.failedFuture(exc),
                        new AnyPointer.Pipeline(PipelineHook.newBrokenPipeline(exc)));
            }

            @Override
            public CompletableFuture<java.lang.Void> sendStreaming() {
                return CompletableFuture.failedFuture(exc);
            }
        };

        return new Request<>() {
            @Override
            public FromPointerBuilder<Params> getParamsFactory() {
                return paramsFactory;
            }

            @Override
            public RequestBase<AnyPointer.Builder> getTypelessRequest() {
                return new AnyPointer.Request(message.getRoot(AnyPointer.factory), hook);
            }

            @Override
            public Request<Params> getBaseRequest() {
                return this;
            }
        };
    }

    static <Params> Request<Params> fromTypeless(
            FromPointerBuilder<Params> paramsFactory,
            Request<AnyPointer.Builder> typeless) {
        return new Request<>() {
            @Override
            public FromPointerBuilder<Params> getParamsFactory() {
                return paramsFactory;
            }

            @Override
            public Request<AnyPointer.Builder> getTypelessRequest() {
                return typeless;
            }

            @Override
            public Request<Params> getBaseRequest() {
                return this;
            }
        };
    }
}
