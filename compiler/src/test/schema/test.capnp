@0xd6eccba9557a7f51;

using Java = import "/java_support/java.capnp";
$Java.package("org.capnproto.test");
$Java.outerClassname("Test");

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

struct TestAllTypes {
  voidField      @0  : Void;
  boolField      @1  : Bool;
  int8Field      @2  : Int8;
  int16Field     @3  : Int16;
  int32Field     @4  : Int32;
  int64Field     @5  : Int64;
  uInt8Field     @6  : UInt8;
  uInt16Field    @7  : UInt16;
  uInt32Field    @8  : UInt32;
  uInt64Field    @9  : UInt64;
  float32Field   @10 : Float32;
  float64Field   @11 : Float64;
  textField      @12 : Text;
  dataField      @13 : Data;
  structField    @14 : TestAllTypes;
  enumField      @15 : TestEnum;
  interfaceField @16 : Void;  # TODO

#  voidList      @17 : List(Void);
#  boolList      @18 : List(Bool);
# ...
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