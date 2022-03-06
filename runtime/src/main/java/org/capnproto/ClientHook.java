package org.capnproto;

import java.io.FileDescriptor;
import java.util.concurrent.CompletableFuture;

public interface ClientHook {

    static final Object NULL_CAPABILITY_BRAND = new Object();
    static final Object BROKEN_CAPABILITY_BRAND = new Object();
    /**
     * Start a new call, allowing the client to allocate request/response objects as it sees fit.
     * This version is used when calls are made from application code in the local process.
     */
    Request<AnyPointer.Builder> newCall(long interfaceId, short methodId);

    /**
     * Call the object, but the caller controls allocation of the request/response objects.  If the
     * callee insists on allocating these objects itself, it must make a copy.  This version is used
     * when calls come in over the network via an RPC system.
     *
     * Since the caller of this method chooses the CallContext implementation, it is the caller's
     * responsibility to ensure that the returned promise is not canceled unless allowed via
     * the context's `allowCancellation()`.
     *
     * The call must not begin synchronously; the callee must arrange for the call to begin in a
     * later turn of the event loop. Otherwise, application code may call back and affect the
     * callee's state in an unexpected way.
     */
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
    Object getBrand();

    /**
     *  Repeatedly calls whenMoreResolved() until it returns nullptr.
     */
    default CompletableFuture<java.lang.Void> whenResolved() {
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
    default FileDescriptor getFd() {
        return null;
    }

    final class VoidPromiseAndPipeline {

        public final CompletableFuture<java.lang.Void> promise;
        public final PipelineHook pipeline;

        VoidPromiseAndPipeline(CompletableFuture<java.lang.Void> promise,
                               PipelineHook pipeline) {
            this.promise = promise;
            this.pipeline = pipeline;
        }
    }
}
