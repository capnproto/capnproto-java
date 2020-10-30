package org.capnproto;

public class PipelineImpl {
    protected final PipelineHook hook;
    protected final PipelineOp[] ops;

    public PipelineImpl(PipelineHook hook) {
        this(hook, new PipelineOp[0]);
    }

    public PipelineImpl(PipelineHook hook, PipelineOp[] ops) {
        this.hook = hook;
        this.ops = ops;
    }

    PipelineImpl noop() {
        return new PipelineImpl(this.hook, this.ops.clone());
    }

    public ClientHook asCap() {
        return this.hook.getPipelinedCap(ops);
    }

    public AnyPointer.Pipeline getPointerField(short pointerIndex) {
        var newOps = new PipelineOp[this.ops.length+1];
        for (int ii = 0; ii < this.ops.length; ++ii) {
            newOps[ii] = this.ops[ii];
        }
        newOps[this.ops.length] = PipelineOp.PointerField(pointerIndex);
        return new AnyPointer.Pipeline(this.hook, newOps);
    }
}
