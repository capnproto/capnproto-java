@0xd6eccba9557a7f51;

using Java = import "/java_support/java.capnp";
$Java.package("org.capnproto.test");

enum TestEnum {
  foo @0;
  bar @1;
  baz @2;
  qux @3;
  quux @4;
  corge @5;
  grault @6;
  garply @7;
}

struct TestOutOfOrder {
  foo @3 :Text;
  bar @2 :Text;
  baz @8 :Text;
  qux @0 :Text;
  quux @6 :Text;
  corge @4 :Text;
  grault @1 :Text;
  garply @7 :Text;
  waldo @5 :Text;
}