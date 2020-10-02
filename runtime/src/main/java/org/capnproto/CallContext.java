package org.capnproto;

import java.util.concurrent.CompletableFuture;

public class CallContext<Params, Results> {

    final CallContextHook hook;
    private final FromPointerReader<Params> params;
    private final FromPointerBuilder<Results> results;

    public CallContext(FromPointerReader<Params> params,
                FromPointerBuilder<Results> results,
                CallContextHook hook) {
        this.hook = hook;
        this.params = params;
        this.results = results;
    }

    public final Params getParams() {
        return hook.getParams().getAs(params);
    }

    public final void releaseParams() {
        this.hook.releaseParams();
    }

    public final Results getResults() {
        return this.hook.getResults().getAs(results);
    }

    public final Results initResults() {
        return this.hook.getResults().initAs(results);
    }

    public final <SubParams> CompletableFuture<java.lang.Void> tailCall(Request<SubParams, Results> tailRequest) {
        return hook.tailCall(tailRequest.hook);
    }

    public final void allowCancellation() {
        this.hook.allowCancellation();
    }

    public final CallContextHook getHook() {
        return this.hook;
    }
}
