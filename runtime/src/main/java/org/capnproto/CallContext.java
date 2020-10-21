package org.capnproto;

import java.util.concurrent.CompletableFuture;

public class CallContext<Params, Results> {

    private final FromPointerReader<Params> params;
    private final FromPointerBuilder<Results> results;
    private final CallContextHook hook;

    public CallContext(FromPointerReader<Params> params,
                FromPointerBuilder<Results> results,
                CallContextHook hook) {
        this.params = params;
        this.results = results;
        this.hook = hook;
    }

    public final Params getParams() {
        return this.hook.getParams().getAs(params);
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

    public final <SubParams, Results> CompletableFuture<?> tailCall(Request<SubParams, Results> tailRequest) {
        return this.hook.tailCall(tailRequest.getHook());
    }

    public final void allowCancellation() {
        this.hook.allowCancellation();
    }

    public final CallContextHook getHook() {
        return this.hook;
    }
}
