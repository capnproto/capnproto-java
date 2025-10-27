package org.capnproto;

public interface RequestBase<Params> {

    FromPointerBuilder<Params> getParamsFactory();

    RequestBase<AnyPointer.Builder> getTypelessRequest();

    default Params getParams() {
        return this.getTypelessRequest().getParams().getAs(this.getParamsFactory());
    }

    default RequestHook getHook() {
        return this.getTypelessRequest().getHook();
    }

    default RemotePromise<AnyPointer.Reader> sendInternal() {
        return this.getTypelessRequest().sendInternal();
    }
}
