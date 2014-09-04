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
#  enumList      @32 : List(TestEnum);
#  interfaceList @33 : List(Void);
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
# ...
}

const globalInt :UInt32 = 12345;