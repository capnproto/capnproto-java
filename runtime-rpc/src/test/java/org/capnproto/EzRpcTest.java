package org.capnproto;

import org.capnproto.rpctest.Test;
import org.junit.Assert;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;

public class EzRpcTest {

    @org.junit.Test
    public void testBasic() throws Exception {
        var callCount = new Counter();
        var address = new InetSocketAddress("localhost", 0);
        var server = new EzRpcServer(new RpcTestUtil.TestInterfaceImpl(callCount), address);
        server.start();

        var clientSocket = AsynchronousSocketChannel.open();
        clientSocket.connect(new InetSocketAddress("localhost", server.getPort())).get();
        var client = new EzRpcClient(clientSocket);
        var cap = new Test.TestInterface.Client(client.getMain());
        var request = cap.fooRequest();
        request.getParams().setI(123);
        request.getParams().setJ(true);

        var response = client.runUntil(request.send()).join();
        Assert.assertEquals("foo", response.getX().toString());
        Assert.assertEquals(1, callCount.value());
    }
}
