# capnproto-java: Cap'n Proto for Java

This is an experimental pure Java implementation of [Cap'n Proto](http://capnproto.org).

It doesn't do much yet.

## getting started

You will need [SBT](http://scala-sbt.org). We recommend [this version](https://github.com/paulp/sbt-extras).

```
$ sbt compile
$ echo '(people = [(id = 123, name = "Alice", email = "alice@example.com", employment = (school = "MIT"))])'   | capnp encode examples/src/main/schema/addressbook.capnp AddressBook | java -cp generator/target/scala-2.11/classes:examples/target/scala-2.11/classes/ org.capnproto.examples.AddressbookMain read
```

To generate IntelliJ project files, do `sbt update gen-idea`.

