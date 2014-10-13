---
layout: post
title: capnproto-java alpha release with benchmarks
author: dwrensha
---

I'm happy to announce that
[capnproto-java](https://github.com/dwrensha/capnproto-java) is now a
mostly complete implementation of
the [Cap'n Proto](http://capnproto.org) serialization protocol.
It supports all of the data features of the Cap'n Proto schema language,
including constants, default values, and arbitrarily nested list types.
Moreover, the runtime and the generated code consist entirely
of platform-independent, safe Java code.

In the spirit of previous announcements about the
[Rust](http://dwrensha.github.io/capnproto-rust/2014/01/15/benchmark-update.html)
and
[OCaml](http://pelzlpj.github.io/capnp-ocaml/2014/09/02/capnp-ocaml-2.0/)
implementations,
I've made some informal measurements
with the [usual](https://github.com/kentonv/capnproto/tree/master/c%2B%2B/src/capnp/benchmark)
toy benchmark suite.
The results show
the Java implementation performing
at worst
3x slower than the C++ and Rust implementations,
and at best about 2x slower.
I find these results promising,
considering how difficult it can be to avoid
heap allocations and redundant bounds-checking
on the JVM.

I ran each of the three benchmark cases
in five modes. In the "object" mode,
the data is passed in-process without any copying.
In the "bytes" mode, the data
is written to a flat array and then re-read in-process.
In the "pipe" mode, the data is passed
as a byte stream between separate processes.
In the "packed" sub-modes,
a [compression scheme](https://kentonv.github.io/capnproto/encoding.html#packing)
is applied to the bytes before they are passed.

To give the JVM a chance to amortize
the cost of just-in-time compilation,
I ran the benchmarks for
10x more iterations than
in the previous Rust vs. C++ benchmarks.

Here are the results.

<img src="{{site.baseurl}}/assets/carsales.png"
     width="500"/>

The CarSales case does a lot of
iteration through lists of structs.
One way to improve Java's performance here
might be to implement list iterators
that update in-place, so that we don't
need to allocate a new `StructReader` for each
member of a list.

<img src="{{site.baseurl}}/assets/catrank.png"
     width="500"/>

The CatRank case emphasizes string processing.
One big difficulty faced by Java here is that
it uses UTF-16 encoding, while
Cap'n Proto uses UTF-8;
translating between the two requires
significant copying of memory.

<img src="{{site.baseurl}}/assets/eval.png"
     width="500"/>

The Eval case seems to confirm that packing
costs proportionally more for Java
than for the other implementations.
There are almost certainly still
some optimizations we could apply to that
part of the Java implementation,
but we may be fundamentally limited
by the fact that Java bounds-checks
every array access.