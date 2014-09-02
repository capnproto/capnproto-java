package org.capnproto;

import org.capnproto.test.Test._;

import org.scalatest.FunSuite


class EncodingSuite extends FunSuite {

  test("AllTypes") {
    val message = new MessageBuilder();
    val allTypes = message.initRoot(TestAllTypes.factory);
    TestUtil.initTestMessage(allTypes);
    TestUtil.checkTestMessage(allTypes);
    TestUtil.checkTestMessage(allTypes.asReader());
  }

  test("AllTypesMultiSegment") {
    val message = new MessageBuilder(5, BuilderArena.AllocationStrategy.FIXED_SIZE);
    val allTypes = message.initRoot(TestAllTypes.factory);
    TestUtil.initTestMessage(allTypes);

    TestUtil.checkTestMessage(allTypes);
    TestUtil.checkTestMessage(allTypes.asReader());
  }

  test("Groups") {
    val builder = new MessageBuilder();
    val root = builder.initRoot(TestGroups.factory);

    //val foo = root.getGroups().initFoo();

  }

  test("Constants") {
    assert(true == TestConstants.BOOL_CONST);
    assert(-123 == TestConstants.INT8_CONST);
    assert(-12345 == TestConstants.INT16_CONST);
    assert(-12345678 == TestConstants.INT32_CONST);
    assert(-123456789012345L == TestConstants.INT64_CONST);

    assert(-22 == TestConstants.UINT8_CONST);
    assert(-19858 == TestConstants.UINT16_CONST);
    assert(-838178284 == TestConstants.UINT32_CONST);
    assert(-6101065172474983726L == TestConstants.UINT64_CONST);

    assert(1234.5f == TestConstants.FLOAT32_CONST);
    assert(-123e45 == TestConstants.FLOAT64_CONST);
  }

  test("GlobalConstants") {
    assert(12345 == GLOBAL_INT);
  }

  // to debug, do this:
  //Serialize.writeMessage((new java.io.FileOutputStream("/Users/dwrensha/Desktop/test.dat")).getChannel(),
  //                       message);


}
