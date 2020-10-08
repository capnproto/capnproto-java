package org.capnproto;

public class StreamingCallContext<Params> {

    private final FromPointerReader<Params> params;
    final CallContextHook hook;

    public StreamingCallContext(FromPointerReader<Params> params,
                                CallContextHook hook) {
        this.params = params;
        this.hook = hook;
    }
}
