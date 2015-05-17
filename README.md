# capnproto-java: Cap'n Proto for Java

[![Build Status](https://travis-ci.org/dwrensha/capnproto-java.svg?branch=master)](https://travis-ci.org/dwrensha/capnproto-java)

[Cap'n Proto](http://capnproto.org) is an extremely efficient protocol for sharing data
and capabilities,
and capnproto-java is a work-in-progress pure Java implementation.

[Read more here.](https://dwrensha.github.io/capnproto-java/index.html)

## How to use this

There are two components to this project:

1. A compiler for Cap'n Proto which outputs Java source code given a Cap'n Proto definition, and the compile-time schema responsible for this.
1. The runtime support which must be referenced by projects wishing to use the generated classes.

### Dependencies

- Install the main `capnp` binary (either built from source or via a package manager)
- A C++ compiler supporting C++11 (the Makefiles for Windows target mingw rather than Visual Studio)
- Maven

### Building and testing

- `make` - builds the compiler, runtime, generates the code for tests and benchmarks, and runs the tests.
- `make benchmark` - runs various benchmarks cases.

Travis CI is set up to build some sample schema, and run tests via Maven. 
