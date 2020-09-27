package org.capnproto;

import java.util.concurrent.CompletableFuture;

public interface ClientHook {

    Object NULL_CAPABILITY_BRAND = new Object();
    Object BROKEN_CAPABILITY_BRAND = new Object();

    Request<AnyPointer.Builder, AnyPointer.Reader> newCall(long interfaceId, short methodId);

    VoidPromiseAndPipeline call(long interfaceId, short methodId, CallContextHook context);

    default ClientHook getResolved() {
        return null;
    }

    default CompletableFuture<ClientHook> whenMoreResolved() {
        return null;
    }

    default Object getBrand() {
        return NULL_CAPABILITY_BRAND;
    }

    default CompletableFuture<java.lang.Void> whenResolved() {
        var promise = whenMoreResolved();
        return promise != null
                ? promise.thenCompose(ClientHook::whenResolved)
                : CompletableFuture.completedFuture(null);
    }

    default boolean isNull() {
        return getBrand() == NULL_CAPABILITY_BRAND;
    }

    default boolean isError() {
        return getBrand() == BROKEN_CAPABILITY_BRAND;
    }

    default Integer getFd() {
        return null;
    }

    final class VoidPromiseAndPipeline {
        public final CompletableFuture<java.lang.Void> promise;
        public final PipelineHook pipeline;

        VoidPromiseAndPipeline(CompletableFuture<java.lang.Void> promise, PipelineHook pipeline) {
            this.promise = promise;
            this.pipeline = pipeline;
        }
    }

    static ClientHook newBrokenCap(String reason) {
        return newBrokenClient(reason, false, BROKEN_CAPABILITY_BRAND);
    }

    static ClientHook newBrokenCap(Throwable exc) {
        return newBrokenClient(exc, false, BROKEN_CAPABILITY_BRAND);
    }

    static ClientHook newNullCap() {
        return newBrokenClient(new RuntimeException("Called null capability"), true, NULL_CAPABILITY_BRAND);
    }

    static private ClientHook newBrokenClient(String reason, boolean resolved, Object brand) {
        return newBrokenClient(new RuntimeException(reason), resolved, brand);
    }

    static private ClientHook newBrokenClient(Throwable exc, boolean resolved, Object brand) {
        return new ClientHook() {
            @Override
            public Request<AnyPointer.Builder, AnyPointer.Reader> newCall(long interfaceId, short methodId) {
                return Request.newBrokenRequest(exc);
            }

            @Override
            public VoidPromiseAndPipeline call(long interfaceId, short methodId, CallContextHook context) {
                return new VoidPromiseAndPipeline(CompletableFuture.failedFuture(exc), null);
            }

            @Override
            public CompletableFuture<ClientHook> whenMoreResolved() {
                if (resolved) {
                    return null;
                } else {
                    return CompletableFuture.failedFuture(exc);
                }
            }

            @Override
            public Object getBrand() {
                return brand;
            }
        };
    }
}
