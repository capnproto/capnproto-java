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
        public final CompletableFuture<?> promise;
        public final PipelineHook pipeline;

        VoidPromiseAndPipeline(CompletableFuture<?> promise, PipelineHook pipeline) {
            this.promise = promise;
            this.pipeline = pipeline;
        }
    }

}
