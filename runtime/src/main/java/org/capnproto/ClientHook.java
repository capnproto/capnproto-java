package org.capnproto;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public interface ClientHook {

    Object NULL_CAPABILITY_BRAND = new Object();
    Object BROKEN_CAPABILITY_BRAND = new Object();

    Request<AnyPointer.Builder, AnyPointer.Pipeline> newCall(long interfaceId, short methodId);

    VoidPromiseAndPipeline call(long interfaceId, short methodId, CallContextHook context);

    /**
     If this ClientHook is a promise that has already resolved, returns the inner, resolved version
     of the capability.  The caller may permanently replace this client with the resolved one if
     desired.  Returns null if the client isn't a promise or hasn't resolved yet -- use
     `whenMoreResolved()` to distinguish between them.

     @return the resolved capability
    */
    default ClientHook getResolved() {
        return null;
    }

    /**
     If this client is a settled reference (not a promise), return nullptr.  Otherwise, return a
     promise that eventually resolves to a new client that is closer to being the final, settled
     client (i.e. the value eventually returned by `getResolved()`).  Calling this repeatedly
     should eventually produce a settled client.
    */
    default CompletableFuture<ClientHook> whenMoreResolved() {
        return null;
    }

    /**
     Returns an opaque object that identifies who made this client. This can be used by an RPC adapter to
     discover when a capability it needs to marshal is one that it created in the first place, and
     therefore it can transfer the capability without proxying.
    */
    default Object getBrand() {
        return NULL_CAPABILITY_BRAND;
    }

    /**
     *  Repeatedly calls whenMoreResolved() until it returns nullptr.
     */
    default CompletionStage<java.lang.Void> whenResolved() {
        var promise = whenMoreResolved();
        return promise != null
                ? promise.thenCompose(ClientHook::whenResolved)
                : CompletableFuture.completedFuture(null);
    }

    /**
     * Returns true if the capability was created as a result of assigning a Client to null or by
     * reading a null pointer out of a Cap'n Proto message.
     */
    default boolean isNull() {
        return getBrand() == NULL_CAPABILITY_BRAND;
    }

    /**
     * Returns true if the capability was created by newBrokenCap().
     */
    default boolean isError() {
        return getBrand() == BROKEN_CAPABILITY_BRAND;
    }

    /**
     *  Implements {@link Capability.Client.getFd}. If this returns null but whenMoreResolved() returns
     *  non-null, then Capability::Client::getFd() waits for resolution and tries again.
     */
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
