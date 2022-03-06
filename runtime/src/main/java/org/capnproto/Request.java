package org.capnproto;

public interface Request<Params>
    extends RequestBase<Params> {

    RequestBase<Params> getBaseRequest();

    default FromPointerBuilder<Params> getParamsFactory() {
        return getBaseRequest().getParamsFactory();
    }

    default RequestBase<AnyPointer.Builder> getTypelessRequest() {
        return getBaseRequest().getTypelessRequest();
    }
}
