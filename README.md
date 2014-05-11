# capnproto-java: Cap'n Proto for Java

This is an experimental pure Java implementation of Cap'n Proto.

It doesn't do much yet.

To get started, try this:

```
$ make
$ cd examples
$ echo '(people = [(id = 123, name = "Alice", email = "alice@example.com", employment = (school = "MIT"))])' \
  | capnp encode addressbook.capnp AddressBook | java -cp .:.. AddressbookMain read
```