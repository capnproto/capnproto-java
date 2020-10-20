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

    static void fromException(Throwable exc, RpcProtocol.Exception.Builder builder) {
        builder.setReason(exc.getMessage());
        builder.setType(RpcProtocol.Exception.Type.FAILED);
    }

    static RpcException toException(RpcProtocol.Exception.Reader reader) {
        var type = RpcException.Type.UNKNOWN;

        switch (reader.getType()) {
            case UNIMPLEMENTED:
                type = RpcException.Type.UNIMPLEMENTED;
                break;
            case FAILED:
                type = RpcException.Type.FAILED;
                break;
            case DISCONNECTED:
            case OVERLOADED:
            default:
                break;
        }
        return new RpcException(type, reader.getReason().toString());
    }
}
