@0xb6577a1582e84742;

using Java = import "/capnp/java.capnp";
$Java.package("org.capnproto.demo");
$Java.outerClassname("Demo");

struct TestParams0 {
   param0 @0 :Int32;
}

struct TestResults0 {
   result0 @0 :Int32;
}

struct TestParams1 {
   param0 @0 :AnyPointer;
}

struct TestResults1 {
   result0 @0 :AnyPointer;
   result1 @1 :AnyPointer;
   result2 @2 :AnyPointer;
}

struct Struct0 {
    f0  @0  :Bool;
}

interface Iface0 {
}

struct Struct2 {
   f0 @0 :AnyPointer;
   f1i @1 :Iface0;
}

interface Iface1 {

  struct Struct1 {
    f0  @0  :Bool;
    f1 @1 :AnyPointer;
  }

  method0 @0 () -> (result0 :Struct0, result1 :Struct1);
  method1 @1 () -> (result0: Iface0);
}


