package org.capnproto;

import java.util.concurrent.CompletableFuture;

public final class CallContext<Params, Results> {

    private final FromPointerReader<Params> paramsFactory;
    private final FromPointerBuilder<Results> resultsFactory;
    private final CallContextHook hook;

    public CallContext(FromPointerReader<Params> paramsFactory,
                FromPointerBuilder<Results> resultsFactory,
                CallContextHook hook) {
        this.paramsFactory = paramsFactory;
        this.resultsFactory = resultsFactory;
        this.hook = hook;
    }

    public final Params getParams() {
        return this.hook.getParams().getAs(paramsFactory);
    }

    public final void releaseParams() {
        this.hook.releaseParams();
    }

    public final Results getResults() {
        return this.hook.getResults().getAs(resultsFactory);
    }

    public final Results initResults() {
        return this.hook.getResults().initAs(resultsFactory);
    }

    public final <SubParams> CompletableFuture<java.lang.Void> tailCall(Request<SubParams> tailRequest) {
        return this.hook.tailCall(tailRequest.getHook());
    }

    public final void allowCancellation() {
        this.hook.allowCancellation();
    }

    public final CallContextHook getHook() {
        return this.hook;
    }
}
