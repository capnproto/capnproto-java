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
If you, for example, want to communicate over a
network, you can just directly write the bytes to the wire.
There is no encode or decode step!

We hope eventually to provide support in Java for a
[distributed object-capability layer](https://kentonv.github.io/capnproto/rpc.html)
built on top of this serialization layer,
but we have not embarked on that project yet.


## Installation

You will need to [install](http://capnproto.org/install.html) the Cap'n Proto schema compiler.

We use [sbt](http://scala-sbt.org) for building and testing.
Running `sbt compile` at the top-level directory should build `capnpc-java`,
`org.capnproto`, and `org.capnproto.examples`.
Running `sbt test` should run the test suite.

## Example

```
@0x9eb32e19f86ee174;
using Java = import "/capnp/java.capnp";
$Java.package("org.capnproto.examples");
$Java.outerClassname("Addressbook");

struct Person {
  id @0 :UInt32;
  name @1 :Text;
  email @2 :Text;
  phones @3 :List(PhoneNumber);

  struct PhoneNumber {
    number @0 :Text;
    type @1 :Type;

    enum Type {
      mobile @0;
      home @1;
      work @2;
    }
  }

  employment :union {
    unemployed @4 :Void;
    employer @5 :Text;
    school @6 :Text;
    selfEmployed @7 :Void;
    # We assume that a person is only one of these.
  }
}

struct AddressBook {
  people @0 :List(Person);
}
```

{% highlight java %}
package org.capnproto.examples;

import org.capnproto.StructList;
import org.capnproto.examples.Addressbook.AddressBook;
import org.capnproto.examples.Addressbook.Person;

public class AddressbookMain {

  public static void writeAddressBook() throws java.io.IOException {
    org.capnproto.MessageBuilder message =
      new org.capnproto.MessageBuilder();

    AddressBook.Builder addressbook =
      message.initRoot(AddressBook.factory);

    StructList.Builder<Person.Builder> people =
      addressbook.initPeople(2);

    Person.Builder alice = people.get(0);
    alice.setId(123);
    alice.setName("Alice");
    alice.setEmail("alice@example.com");

    StructList.Builder<Person.PhoneNumber.Builder> alicePhones =
      alice.initPhones(1);
    alicePhones.get(0).setNumber("555-1212");
    alicePhones.get(0).setType(Person.PhoneNumber.Type.MOBILE);
    alice.getEmployment().setSchool("MIT");

    Person.Builder bob = people.get(1);
    bob.setId(456);
    bob.setName("Bob");
    bob.setEmail("bob@example.com");
    StructList.Builder<Person.PhoneNumber.Builder> bobPhones =
      bob.initPhones(2);
    bobPhones.get(0).setNumber("555-4567");
    bobPhones.get(0).setType(Person.PhoneNumber.Type.HOME);
    bobPhones.get(1).setNumber("555-7654");
    bobPhones.get(1).setType(Person.PhoneNumber.Type.WORK);
    bob.getEmployment().setUnemployed(org.capnproto.Void.VOID);

    org.capnproto.SerializePacked.writeToUnbuffered(
      (new java.io.FileOutputStream(
        java.io.FileDescriptor.out)).getChannel(),
      message);
  }

  public static void printAddressBook() throws java.io.IOException {
    org.capnproto.MessageReader message =
      org.capnproto.SerializePacked.readFromUnbuffered(
        (new java.io.FileInputStream(
          java.io.FileDescriptor.in)).getChannel());

    AddressBook.Reader addressbook =
      message.getRoot(AddressBook.factory);

    for(Person.Reader person : addressbook.getPeople()) {
      System.out.println(person.getName() + ": " + person.getEmail());

      for (Person.PhoneNumber.Reader phone : person.getPhones()) {
        String typeName = "UNKNOWN";
        switch (phone.getType()) {
        case MOBILE:
          typeName = "mobile"; break;
        case HOME:
          typeName = "home"; break;
        case WORK:
          typeName = "work"; break;
        }
        System.out.println("  " + typeName + " phone: " +
                           phone.getNumber());
      }

      Person.Employment.Reader employment = person.getEmployment();
      switch (employment.which()) {
      case UNEMPLOYED:
        System.out.println("  unemployed");
        break;
      case EMPLOYER:
        System.out.println("  employer: " + employment.getEmployer());
        break;
      case SCHOOL:
        System.out.println("  student at: " + employment.getSchool());
        break;
      case SELF_EMPLOYED:
        System.out.println("  self-employed");
        break;
      default :
        break;
      }
    }
  }

  public static void usage() {
    System.out.println("usage: addressbook [write | read]");
  }

  public static void main(String[] args) {
    try {
      if (args.length < 1) {
        usage();
      } else if (args[0].equals("write")) {
        writeAddressBook();
      } else if (args[0].equals("read")) {
        printAddressBook();
      } else {
        usage();
      }
    } catch (java.io.IOException e) {
      System.out.println("io exception: "  + e);
    }
  }
}
{% endhighlight %}

To read a message:

    $ echo '(people = [(id = 123, name = "Alice", email = "alice@example.com", employment = (school = "MIT"))])' \
     | capnp encode --packed examples/src/main/schema/addressbook.capnp AddressBook \
     | java -cp runtime/target/scala-2.11/classes:examples/target/scala-2.11/classes/ \
       org.capnproto.examples.AddressbookMain read


To write a message:

    $ java -cp runtime/target/scala-2.11/classes:examples/target/scala-2.11/classes/  \
      org.capnproto.examples.AddressbookMain write \
      | capnp decode --packed examples/src/main/schema/addressbook.capnp AddressBook
