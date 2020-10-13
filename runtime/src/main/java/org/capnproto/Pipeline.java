package org.capnproto;

public class Pipeline<Results>
        extends RemotePromise<Results> {

    protected final PipelineOp[] ops;
    protected PipelineHook hook;

    public Pipeline(RemotePromise<Results> remotePromise) {
        this(remotePromise, new PipelineOp[0]);
    }

    public Pipeline(RemotePromise<Results> remotePromise, PipelineOp[] ops) {
        super(remotePromise.response, remotePromise.hook);
        this.ops = ops;
        this.hook = remotePromise.hook;
    }

    public PipelineHook getHook() {
        return hook;
    }

    Pipeline<Results> noop() {
        return new Pipeline<>(this, this.ops.clone());
    }

    public ClientHook asCap() {
        return this.hook.getPipelinedCap(this.ops);
    }

    public Pipeline<Results> getPointerField(short pointerIndex) {
        var newOps = new PipelineOp[this.ops.length+1];
        for (int ii = 0; ii < this.ops.length; ++ii) {
            newOps[ii] = this.ops[ii];
        }
        newOps[this.ops.length] = PipelineOp.PointerField(pointerIndex);
        return new Pipeline<>(this, newOps);
    }
}


