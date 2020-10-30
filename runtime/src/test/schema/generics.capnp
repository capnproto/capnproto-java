@0xbf250a886b8a4258;

using Java = import "/capnp/java.capnp";
$Java.package("org.capnproto.test");
$Java.outerClassname("TestGenerics");

interface Aaa {
  struct S {
    bar @0 :UInt32;
  }
}

struct Sss(X) {
}

interface Bbb(X) {

  foo @0 (value: Aaa);
}

interface Ccc(X) {
}

#interface Ddd(X, Y) {
#   foo @0 (value: X);
#   bar @1 () -> (value: X);
#}