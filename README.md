# capnproto-java: Cap'n Proto for Java

[![Build Status](https://github.com/vaci/capnproto-java-rpc/workflows/CI/badge.svg?branch=master&event=push)](https://github.com/vaci/capnproto-java-rpc/actions?query=workflow%3ACI)

[Cap'n Proto](http://capnproto.org) is an extremely efficient protocol for sharing data
and capabilities, and capnproto-java is a pure Java implementation.

[Read more here.](https://dwrensha.github.io/capnproto-java/index.html)

This repository clone adds an implementation of the RPC framework for Java.

Promise pipelining is provided via java.util.concurrent.CompletableFuture. Unlike the KJ asynchronous model, which completes promises
only when they are waited upon, a CompletableFuture can complete immediately. This may break E-ordering, as the C++ implementation
relies on kj::evalLater() to defer method calls, and there is no obvious (to me, anyway) way to replicate the behaviour of
kj::evalLater() with CompletableFutures.

Most of the C++ RPC test cases have been ported to this implementation, which gives me some comfort that the implementation logic is 
correct, but more extensive testing is required. 

This implementation does not support generic interfaces. Extending the schema compiler to output code for generic interfaces is an
exercise I leave to the reader.


