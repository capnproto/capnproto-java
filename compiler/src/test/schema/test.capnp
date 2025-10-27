@0xd6eccba9557a7f51;

using Java = import "/capnp/java.capnp";
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

  voidList      @17 : List(Void);
  boolList      @18 : List(Bool);
  int8List      @19 : List(Int8);
  int16List     @20 : List(Int16);
  int32List     @21 : List(Int32);
  int64List     @22 : List(Int64);
  uInt8List     @23 : List(UInt8);
  uInt16List    @24 : List(UInt16);
  uInt32List    @25 : List(UInt32);
  uInt64List    @26 : List(UInt64);
  float32List   @27 : List(Float32);
  float64List   @28 : List(Float64);
  textList      @29 : List(Text);
  dataList      @30 : List(Data);
  structList    @31 : List(TestAllTypes);
  enumList      @32 : List(TestEnum);
#  interfaceList @33 : List(Void);
}



struct TestDefaults {
  voidField      @0  : Void    = void;
  boolField      @1  : Bool    = true;
  int8Field      @2  : Int8    = -123;
  int16Field     @3  : Int16   = -12345;
  int32Field     @4  : Int32   = -12345678;
  int64Field     @5  : Int64   = -123456789012345;
  uInt8Field     @6  : UInt8   = 234;
  uInt16Field    @7  : UInt16  = 45678;
  uInt32Field    @8  : UInt32  = 3456789012;
  uInt64Field    @9  : UInt64  = 12345678901234567890;
  float32Field   @10 : Float32 = 1234.5;
  float64Field   @11 : Float64 = -123e45;
  textField      @12 : Text    = "foo";
  dataField      @13 : Data    = "bar"; # 0x"62 61 72";

  structField    @14 : TestAllTypes = (
      voidField      = void,
      boolField      = true,
      int8Field      = -12,
      int16Field     = 3456,
      int32Field     = -78901234,
      int64Field     = 56789012345678,
      uInt8Field     = 90,
      uInt16Field    = 1234,
      uInt32Field    = 56789012,
      uInt64Field    = 345678901234567890,
      float32Field   = -1.25e-10,
      float64Field   = 345,
      textField      = "baz",
      dataField      = "qux",
      structField    = (
          textField = "nested",
          structField = (textField = "really nested")),
      enumField      = baz,
      # interfaceField can't have a default

      voidList      = [void, void, void],
      boolList      = [false, true, false, true, true],
      int8List      = [12, -34, -0x80, 0x7f],
      int16List     = [1234, -5678, -0x8000, 0x7fff],
      int32List     = [12345678, -90123456, -0x80000000, 0x7fffffff],
      int64List     = [123456789012345, -678901234567890, -0x8000000000000000, 0x7fffffffffffffff],
      uInt8List     = [12, 34, 0, 0xff],
      uInt16List    = [1234, 5678, 0, 0xffff],
      uInt32List    = [12345678, 90123456, 0, 0xffffffff],
      uInt64List    = [123456789012345, 678901234567890, 0, 0xffffffffffffffff],
      float32List   = [0, 1234567, 1e37, -1e37, 1e-37, -1e-37],
      float64List   = [0, 123456789012345, 1e306, -1e306, 1e-306, -1e-306],
      textList      = ["quux", "corge", "grault"],
      dataList      = ["garply", "waldo", "fred"],
      structList    = [
          (textField = "x structlist 1"),
          (textField = "x structlist 2"),
          (textField = "x structlist 3")],
      enumList      = [qux, bar, grault]
      # interfaceList can't have a default
      );

  enumField      @15 : TestEnum = corge;
  interfaceField @16 : Void;  # TODO

  voidList      @17 : List(Void)    = [void, void, void, void, void, void];
  boolList      @18 : List(Bool)    = [true, false, false, true];
  int8List      @19 : List(Int8)    = [111, -111];
  int16List     @20 : List(Int16)   = [11111, -11111];
  int32List     @21 : List(Int32)   = [111111111, -111111111];
  int64List     @22 : List(Int64)   = [1111111111111111111, -1111111111111111111];
  uInt8List     @23 : List(UInt8)   = [111, 222] ;
  uInt16List    @24 : List(UInt16)  = [33333, 44444];
  uInt32List    @25 : List(UInt32)  = [3333333333];
  uInt64List    @26 : List(UInt64)  = [11111111111111111111];
  float32List   @27 : List(Float32) = [5555.5, inf, -inf, nan];
  float64List   @28 : List(Float64) = [7777.75, inf, -inf, nan];
  textList      @29 : List(Text)    = ["plugh", "xyzzy", "thud"];
  dataList      @30 : List(Data)    = ["oops", "exhausted", "rfc3092"];
  structList    @31 : List(TestAllTypes) = [
      (textField = "structlist 1"),
      (textField = "structlist 2"),
      (textField = "structlist 3")];
  enumList      @32 : List(TestEnum) = [foo, garply];
  interfaceList @33 : List(Void);  # TODO


}

struct TestAnyPointer {
  anyPointerField @0 :AnyPointer;

  # Do not add any other fields here!  Some tests rely on anyPointerField being the last pointer
  # in the struct.
}

struct TestAnyOthers {
  anyStructField @0 :AnyStruct;
  #anyListField @1 :AnyPointer; # not currently implemented
  #capabilityField @2 :Capability;
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


struct TestUnion {
  union0 @0! :union {
    # Pack union 0 under ideal conditions: there is no unused padding space prior to it.
    u0f0s0  @4: Void;
    u0f0s1  @5: Bool;
    u0f0s8  @6: Int8;
    u0f0s16 @7: Int16;
    u0f0s32 @8: Int32;
    u0f0s64 @9: Int64;
    u0f0sp  @10: Text;

    # Pack more stuff into union0 -- should go in same space.
    u0f1s0  @11: Void;
    u0f1s1  @12: Bool;
    u0f1s8  @13: Int8;
    u0f1s16 @14: Int16;
    u0f1s32 @15: Int32;
    u0f1s64 @16: Int64;
    u0f1sp  @17: Text;

    primitiveList @56: List(UInt32);
  }

  # Pack one bit in order to make pathological situation for union1.
  bit0 @18: Bool;

  union1 @1! :union {
    # Pack pathologically bad case.  Each field takes up new space.
    u1f0s0  @19: Void;
    u1f0s1  @20: Bool;
    u1f1s1  @21: Bool;
    u1f0s8  @22: Int8;
    u1f1s8  @23: Int8;
    u1f0s16 @24: Int16;
    u1f1s16 @25: Int16;
    u1f0s32 @26: Int32;
    u1f1s32 @27: Int32;
    u1f0s64 @28: Int64;
    u1f1s64 @29: Int64;
    u1f0sp  @30: Text;
    u1f1sp  @31: Text;

    # Pack more stuff into union1 -- each should go into the same space as corresponding u1f0s*.
    u1f2s0  @32: Void;
    u1f2s1  @33: Bool;
    u1f2s8  @34: Int8;
    u1f2s16 @35: Int16;
    u1f2s32 @36: Int32;
    u1f2s64 @37: Int64;
    u1f2sp  @38: Text;
  }

  # Fill in the rest of that bitfield from earlier.
  bit2 @39: Bool;
  bit3 @40: Bool;
  bit4 @41: Bool;
  bit5 @42: Bool;
  bit6 @43: Bool;
  bit7 @44: Bool;

  # Interleave two unions to be really annoying.
  # Also declare in reverse order to make sure union discriminant values are sorted by field number
  # and not by declaration order.
  union2 @2! :union {
    u2f0s64 @54: Int64;
    u2f0s32 @52: Int32;
    u2f0s16 @50: Int16;
    u2f0s8 @47: Int8;
    u2f0s1 @45: Bool;
  }

  union3 @3! :union {
    u3f0s64 @55: Int64;
    u3f0s32 @53: Int32;
    u3f0s16 @51: Int16;
    u3f0s8 @48: Int8;
    u3f0s1 @46: Bool;
  }

  byte0 @49: UInt8;
}


struct TestGroups {
  groups :union {
    foo :group {
      corge @0 :Int32;
      grault @2 :Int64;
      garply @8 :Text;
      quux @9 : Void;
    }
    bar :group {
      corge @3 :Int32;
      grault @4 :Text;
      garply @5 :Int64;
    }
    baz :group {
      corge @1 :Int32;
      grault @6 :Text;
      garply @7 :Text;
    }
  }
}

struct TestLists {
  # Small structs, when encoded as list, will be encoded as primitive lists rather than struct
  # lists, to save space.
  struct Struct0  { f @0 :Void; }
  struct Struct1  { f @0 :Bool; }
  struct Struct8  { f @0 :UInt8; }
  struct Struct16 { f @0 :UInt16; }
  struct Struct32 { f @0 :UInt32; }
  struct Struct64 { f @0 :UInt64; }
  struct StructP  { f @0 :Text; }

  # Versions of the above which cannot be encoded as primitive lists.
  struct Struct0c  { f @0 :Void; pad @1 :Text; }
  struct Struct1c  { f @0 :Bool; pad @1 :Text; }
  struct Struct8c  { f @0 :UInt8; pad @1 :Text; }
  struct Struct16c { f @0 :UInt16; pad @1 :Text; }
  struct Struct32c { f @0 :UInt32; pad @1 :Text; }
  struct Struct64c { f @0 :UInt64; pad @1 :Text; }
  struct StructPc  { f @0 :Text; pad @1 :UInt64; }

  list0  @0 :List(Struct0);
  list1  @1 :List(Struct1);
  list8  @2 :List(Struct8);
  list16 @3 :List(Struct16);
  list32 @4 :List(Struct32);
  list64 @5 :List(Struct64);
  listP  @6 :List(StructP);

  int32ListList @7 :List(List(Int32));
  textListList @8 :List(List(Text));
  structListList @9 :List(List(TestAllTypes));
  boolListListList @10 :List(List(List(Bool)));
  enumListList @11 :List(List(TestEnum));
}

struct TestFieldZeroIsBit {
  bit @0 :Bool;
  secondBit @1 :Bool = true;
  thirdField @2 :UInt8 = 123;
}



struct TestOldVersion {
  # A subset of TestNewVersion.
  old1 @0 :Int64;
  old2 @1 :Text;
  old3 @2 :TestOldVersion;
  old4 @3 :List(Text);
}

struct TestNewVersion {
  # A superset of TestOldVersion.
  old1 @0 :Int64;
  old2 @1 :Text;
  old3 @2 :TestNewVersion;

  struct UpgradedFromText {
     textField @0 :Text;
     int32Field @1 :Int32;
     dataField @2 :Data;
  }
  old4 @3 :List(UpgradedFromText);
  new1 @4 :Int64 = 987;
  new2 @5 :Text = "baz";
  new3 @6 :TestDefaults;
}

struct TestGenerics(Foo, Bar) {
  foo @0 :Foo;
  bar @1 :Bar;
  rev @2 :TestGenerics(Bar, Foo);
  dub @3 :TestGenerics(Text, List(UInt8));

  struct Inner {
    foo @0 :Foo;
    bar @1 :Bar;
  }

  struct Inner2(Baz) {
    bar @0 :Bar;
    baz @1 :Baz;
    innerBound @2 :Inner;
    innerUnbound @3 :TestGenerics.Inner;

    struct DeepNest(Qux) {
      foo @0 :Foo;
      bar @1 :Bar;
      baz @2 :Baz;
      qux @3 :Qux;
    }
  }
}

struct TestGenericsWrapper(Foo, Bar) {
  value @0 :TestGenerics(Foo, Bar);
}

struct TestGenericsWrapper2 {
  value @0 :TestGenericsWrapper(Text, TestAllTypes);
}

struct TestUseGenerics  {
  basic @0 :TestGenerics(TestAllTypes, TestAnyPointer);
  inner @1 :TestGenerics(TestAllTypes, TestAnyPointer).Inner;
  inner2 @2 :TestGenerics(TestAllTypes, TestAnyPointer).Inner2(Text);
  unspecified @3 :TestGenerics;
  unspecifiedInner @4 :TestGenerics.Inner2(Text);
  wrapper @8 :TestGenericsWrapper(TestAllTypes, TestAnyPointer);

  default @5 :TestGenerics(TestAllTypes, Text) =
      (foo = (int16Field = 123), rev = (foo = "text", rev = (foo = (int16Field = 321))));
  defaultInner @6 :TestGenerics(TestAllTypes, Text).Inner =
      (foo = (int16Field = 123), bar = "text");
  defaultUser @7 :TestUseGenerics = (basic = (foo = (int16Field = 123)));
  defaultWrapper @9 :TestGenericsWrapper(Text, TestAllTypes) =
      (value = (foo = "text", rev = (foo = (int16Field = 321))));
  defaultWrapper2 @10 :TestGenericsWrapper2 =
      (value = (value = (foo = "text", rev = (foo = (int16Field = 321)))));

  listOfGenerics @11 :List(TestGenerics(TestAllTypes, Text));
}

struct GenericMap(K, V)
{
  entries @0 :List(Entry);

  struct Entry
  {
    key @0 :K;
    value @1 :V;
  }

 # Some other things that might cause problems.
 a @1 :List(TestAllTypes);
 b @2 :List(List(Entry));
 c @3 :List(List(List(Entry)));
}

# TODO. this is currently broken
#struct TestGenericUnion(T) {
#  union0: union {
#    nothing @0 :Void;
#    something @1 :T;
#  }
#}

struct TestEmptyStruct {}

struct TestConstants {
  const voidConst      :Void    = void;
  const boolConst      :Bool    = true;
  const int8Const      :Int8    = -123;
  const int16Const     :Int16   = -12345;
  const int32Const     :Int32   = -12345678;
  const int64Const     :Int64   = -123456789012345;
  const uint8Const     :UInt8   = 234;
  const uint16Const    :UInt16  = 45678;
  const uint32Const    :UInt32  = 3456789012;
  const uint64Const    :UInt64  = 12345678901234567890;
  const float32Const   :Float32 = 1234.5;
  const float64Const   :Float64 = -123e45;
  const textConst      :Text    = "foo";
  const dataConst      :Data    = "bar";
  const structConst    :TestAllTypes = (
      voidField      = void,
      boolField      = true,
      int8Field      = -12,
      int16Field     = 3456,
      int32Field     = -78901234,
      int64Field     = 56789012345678,
      uInt8Field     = 90,
      uInt16Field    = 1234,
      uInt32Field    = 56789012,
      uInt64Field    = 345678901234567890,
      float32Field   = -1.25e-10,
      float64Field   = 345,
      textField      = "baz",
      dataField      = "qux",
      structField    = (
          textField = "nested",
          structField = (textField = "really nested")),
      enumField      = baz,
      # interfaceField can't have a default

      voidList      = [void, void, void],
      boolList      = [false, true, false, true, true],
      int8List      = [12, -34, -0x80, 0x7f],
      int16List     = [1234, -5678, -0x8000, 0x7fff],
      int32List     = [12345678, -90123456, -0x80000000, 0x7fffffff],
      int64List     = [123456789012345, -678901234567890, -0x8000000000000000, 0x7fffffffffffffff],
      uInt8List     = [12, 34, 0, 0xff],
      uInt16List    = [1234, 5678, 0, 0xffff],
      uInt32List    = [12345678, 90123456, 0, 0xffffffff],
      uInt64List    = [123456789012345, 678901234567890, 0, 0xffffffffffffffff],
      float32List   = [0, 1234567, 1e37, -1e37, 1e-37, -1e-37],
      float64List   = [0, 123456789012345, 1e306, -1e306, 1e-306, -1e-306],
      textList      = ["quux", "corge", "grault"],
      dataList      = ["garply", "waldo", "fred"],
      structList    = [
          (textField = "x structlist 1"),
          (textField = "x structlist 2"),
          (textField = "x structlist 3")],
      enumList      = [qux, bar, grault]
      # interfaceList can't have a default

      );
  const enumConst      :TestEnum = corge;

  const voidListConst      :List(Void)    = [void, void, void, void, void, void];
  const boolListConst      :List(Bool)    = [true, false, false, true];
  const int8ListConst      :List(Int8)    = [111, -111];
  const int16ListConst     :List(Int16)   = [11111, -11111];
  const int32ListConst     :List(Int32)   = [111111111, -111111111];
  const int64ListConst     :List(Int64)   = [1111111111111111111, -1111111111111111111];
  const uint8ListConst     :List(UInt8)   = [111, 222] ;
  const uint16ListConst    :List(UInt16)  = [33333, 44444];
  const uint32ListConst    :List(UInt32)  = [3333333333];
  const uint64ListConst    :List(UInt64)  = [11111111111111111111];
  const float32ListConst   :List(Float32) = [5555.5, inf, -inf, nan];
  const float64ListConst   :List(Float64) = [7777.75, inf, -inf, nan];
  const textListConst      :List(Text)    = ["plugh", "xyzzy", "thud"];
  const dataListConst      :List(Data)    = ["oops", "exhausted", "rfc3092"];
  const structListConst    :List(TestAllTypes) = [
      (textField = "structlist 1"),
      (textField = "structlist 2"),
      (textField = "structlist 3")];
  const enumListConst      :List(TestEnum) = [foo, garply];

}

const globalInt :UInt32 = 12345;
