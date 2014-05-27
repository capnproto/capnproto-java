package org.capnproto;

import org.capnproto.test.Test._;

import org.scalatest.FunSuite


class EncodingSuite extends FunSuite {

  test("AllTypes") {
    val message = new MessageBuilder();
    val allTypes = message.initRoot(TestAllTypes.Builder.factory);
    TestUtil.initTestMessage(allTypes);
    TestUtil.checkTestMessage(allTypes);
  }

}
