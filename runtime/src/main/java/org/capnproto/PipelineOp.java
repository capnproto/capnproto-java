package org.capnproto;

final class PipelineOp {

    // TODO just use array of Short?

    enum Type {
        NOOP,
        GET_POINTER_FIELD
    }

    final PipelineOp.Type type;
    final short pointerIndex;

    private PipelineOp(PipelineOp.Type type, short pointerIndex) {
        this.type = type;
        this.pointerIndex = pointerIndex;
    }

    static PipelineOp Noop() {
        return new PipelineOp(Type.NOOP, (short) 0);
    }

    static PipelineOp PointerField(short pointerIndex) {
        return new PipelineOp(Type.GET_POINTER_FIELD, pointerIndex);
    }

    static void FromPipelineOps(PipelineOp[] ops, RpcProtocol.PromisedAnswer.Builder builder) {
        var transforms = builder.initTransform(ops.length);
        for (int ii = 0; ii < ops.length; ++ii) {
            switch (ops[ii].type) {
                case NOOP:
                    transforms.get(ii).setNoop(null);
                    break;
                case GET_POINTER_FIELD:
                    transforms.get(ii).setGetPointerField(ops[ii].pointerIndex);
                    break;
            }
        }
    }

    static PipelineOp[] ToPipelineOps(RpcProtocol.PromisedAnswer.Reader reader) {
        var transforms = reader.getTransform();
        var ops = new PipelineOp[transforms.size()];
        for (int ii = 0; ii < ops.length; ++ii) {
            var transform = transforms.get(ii);
            switch (transform.which()) {
                case NOOP:
                    ops[ii] = Noop(); // TODO null?
                    break;
                case GET_POINTER_FIELD:
                    ops[ii] = PointerField(transform.getGetPointerField());
                    break;
                default:
                    // TODO improve error handling here
                    // Unsupported pipeline ops
                    return null;
            }
        }
        return ops;
    }
}
