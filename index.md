---
layout: page
title: Cap'n Proto for Java
---

## Introduction

This is a Java implementation of [Cap'n Proto](http://capnproto.org).
It has two main components:

1. A C++ program `capnpc-java` that
generates Java source code from Cap'n Proto [schemas](https://kentonv.github.io/capnproto/language.html)
by acting as a
[plugin](https://kentonv.github.io/capnproto/otherlang.html#how_to_write_compiler_plugins)
to the Cap'n Proto schema compiler.

2. A Java package `org.capnproto` that provides runtime support for `capnpc-java`'s generated code.

These components make it easy
to manipulate data in Java and to share it
with any other language that has a Cap'n Proto implementation.
Under the hood, all operations are
backed by `java.nio.ByteBuffer`.
If you for example want to communicate over a
network, you can just directly write the bytes to the wire.
There is no encode or decode step!

We hope eventually to provide support in Java for a
[distributed object-capability layer](https://kentonv.github.io/capnproto/rpc.html)
built on top of this serialization layer,
but we have not embarked on that project yet.


## Installation

You will need [sbt](http://scala-sbt.org).
We recommend [this version](https://github.com/paulp/sbt-extras).

You will also need to [install](http://capnproto.org/install.html) the Cap'n Proto schema compiler.

Running `sbt compile` at the `capnproto-java` top-level directory should build `capnpc-java`,
`org.capnproto`, and `org.capnproto.examples`.

Running `sbt test` should run the test suite.

## Examples

To read a message:

    $ echo '(people = [(id = 123, name = "Alice", email = "alice@example.com", employment = (school = "MIT"))])' \
     | capnp encode --packed examples/src/main/schema/addressbook.capnp AddressBook \
     | java -cp runtime/target/scala-2.11/classes:examples/target/scala-2.11/classes/ \
       org.capnproto.examples.AddressbookMain read


To write a message:

    $ java -cp runtime/target/scala-2.11/classes:examples/target/scala-2.11/classes/  \
      org.capnproto.examples.AddressbookMain write \
      | capnp decode --packed examples/src/main/schema/addressbook.capnp AddressBook
