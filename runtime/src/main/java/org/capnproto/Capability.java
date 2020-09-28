package org.capnproto;

public class Capability {

    public static class Client {

        final ClientHook hook;

        public Client(ClientHook hook) {
            this.hook = hook;
        }

    }

}
