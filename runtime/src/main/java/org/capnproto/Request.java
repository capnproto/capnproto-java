package org.capnproto;

import java.util.concurrent.CompletableFuture;

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
