package org.capnproto;

public class StreamingCallContext<Params> {

    private final FromPointerReader<Params> paramsFactory;
    private final CallContextHook hook;

    public StreamingCallContext(FromPointerReader<Params> paramsFactory,
                                CallContextHook hook) {
        this.paramsFactory = paramsFactory;
        this.hook = hook;
    }

    public final Params getParams() {
        return this.hook.getParams().getAs(paramsFactory);
    }

    public final void allowCancellation() {
        this.hook.allowCancellation();
    }
}
