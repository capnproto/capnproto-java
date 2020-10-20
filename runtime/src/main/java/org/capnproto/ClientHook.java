package org.capnproto;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public interface ClientHook {

    Object NULL_CAPABILITY_BRAND = new Object();
    Object BROKEN_CAPABILITY_BRAND = new Object();

    Request<AnyPointer.Builder, AnyPointer.Pipeline> newCall(long interfaceId, short methodId);

    VoidPromiseAndPipeline call(long interfaceId, short methodId, CallContextHook context);

    default ClientHook getResolved() {
        return null;
    }

    default CompletionStage<ClientHook> whenMoreResolved() {
        return null;
    }

    default Object getBrand() {
        return NULL_CAPABILITY_BRAND;
    }

    default CompletionStage<?> whenResolved() {
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
        public final CompletionStage<java.lang.Void> promise;
        public final PipelineHook pipeline;

        VoidPromiseAndPipeline(CompletionStage<java.lang.Void> promise, PipelineHook pipeline) {
            this.promise = promise;
            this.pipeline = pipeline;
        }
    }

}
