package org.capnproto;

public final class RpcException extends java.lang.Exception {

    public enum Type {
        UNKNOWN,
        UNIMPLEMENTED,
        FAILED,
        DISCONNECTED
    }

    private Type type;

    public RpcException(Type type, String message) {
        super(message);
        this.type = type;
    }

    public final Type getType() {
        return type;
    }

    public static RpcException unimplemented(String message) {
        return new RpcException(Type.UNIMPLEMENTED, message);
    }

    public static RpcException failed(String message) {
        return new RpcException(Type.FAILED, message);
    }

    public static RpcException disconnected(String message) {
        return new RpcException(Type.DISCONNECTED, message);
    }
}
