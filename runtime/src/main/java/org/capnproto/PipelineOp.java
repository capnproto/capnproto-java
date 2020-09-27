package org.capnproto;

final class PipelineOp {

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
}
