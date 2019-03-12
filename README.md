# capnproto-java: Cap'n Proto for Java

[![Build Status](https://travis-ci.org/paxel/capnproto-java.svg?branch=master)](https://travis-ci.org/capnproto/capnproto-java)

[Cap'n Proto](http://capnproto.org) is an extremely efficient protocol for sharing data
and capabilities, and capnproto-java is a pure Java implementation.

[Read more in the source of this fork.](https://dwrensha.github.io/capnproto-java/index.html)

Improvements that have been introduced here:

* dropped support for JDK 7 (question -> reply ("because of this.")).apply("how is that an improvement?")
* toString() - The generated source now has toString() so you can log what you receive or send
* doX(Consumer<T> c) - safely call doX methods without which(), isX() and/or hasX() before. The consumer will only receive X in case X exists.
* Collection<> - all ListReader implement Collection<> now. Yes including stream(). Yes you can instantiate list entries via stream().
* interface annotation - you can now annotate structs with an interface to let reader and builder of similar objects implement similar interfaces to simplify usage of generated code. you're welcome.


