package org.capnproto;

import org.capnproto.test.MessageJava;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class MessageJavaToStringTest {

    private MessageJava.Message.Builder message;

    @Test
    public void testDataMessage() {
        withDataMessage();
        verifyToString("Message { Time={1030} Data={Data{buffer=java.nio.HeapByteBuffer[pos=0 lim=8192 cap=8192]}}}");
        verifyReaderToString("Message { Time={1030} Data={Data{buffer=java.nio.HeapByteBuffer[pos=0 lim=8192 cap=8192]}}}");
    }

    @Test
    public void testEnumMessage() {
        withEnumMessage();
        verifyToString("Message { Time={-1030} Flag={FILE_NOT_FOUND}}");
    }

    @Test
    public void testMixedMessage() {
        withMessages();
        verifyToString("Message { Time={-1030} Messages={Message { Time={0} MetaData={MetaData { Size={2}}}},Message { Time={0} Flag={START}}}}");
    }

    private void verifyToString(final String result) {
        assertThat(message.toString(), is(result));
    }

    private void withDataMessage() {
        this.message = new MessageBuilder().initRoot(MessageJava.Message.factory);
        message.initData(100);
        message.setTime(1030L);
    }

    private void withEnumMessage() {
        this.message = new MessageBuilder().initRoot(MessageJava.Message.factory);
        message.setTime(-1030L);
        message.setFlag(MessageJava.Flag.FILE_NOT_FOUND);
    }

    private void withMessages() {
        this.message = new MessageBuilder().initRoot(MessageJava.Message.factory);
        message.setTime(-1030L);
        final StructList.Builder<MessageJava.Message.Builder> msgs = message.initMessages(2);
        msgs.get(0).initMetaData().setName("Andrew");
        msgs.get(0).initMetaData().setSize(2);
        msgs.get(1).setFlag(MessageJava.Flag.START);
    }

    private void verifyReaderToString(String result) {
        assertThat(message.asReader().toString(), is(result));
    }
}
