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
    f1  @1  :UInt16;
    f2  @2  :Int16;
    f3  @3  :UInt32;
    f4  @4  :Int32;
    f5  @5  :UInt64;
    f6  @6  :Int64;
    f7  @7  :Float32;
    f8  @8  :Float64;
    f9  @9  :Text;
    f10 @10 :Data;
    f11 @11 :AnyPointer;
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
    f1  @1  :UInt16;
    f2  @2  :Int16;
    f3  @3  :UInt32;
    f4  @4  :Int32;
    f5  @5  :UInt64;
    f6  @6  :Int64;
    f7  @7  :Float32;
    f8  @8  :Float64;
    f9  @9  :Text;
    f10 @10 :Data;
    f11 @11 :AnyPointer;
  }

  method0 @0 () -> (result0 :Struct0, result1 :Struct1);
  method1 @1 () -> (result0: Iface0);

}


