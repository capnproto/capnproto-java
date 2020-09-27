package org.capnproto;

interface RequestHook {
    RemotePromise<AnyPointer.Reader> send();
    Object getBrand();
}
