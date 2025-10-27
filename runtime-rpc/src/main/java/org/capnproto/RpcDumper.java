package org.capnproto;

import java.util.HashMap;
import java.util.Map;

public class RpcDumper {

    //private final Map<Long, Schema.Node.Reader> schemas = new HashMap<>();
    private final Map<Integer, Long> clientReturnTypes = new HashMap<>();
    private final Map<Integer, Long> serverReturnTypes = new HashMap<>();

    /*void addSchema(long schemaId, Schema.Node.Reader node) {
        this.schemas.put(schemaId, node);
    }*/

    private void setReturnType(RpcTwoPartyProtocol.Side side, int schemaId, long schema) {
        switch (side) {
            case CLIENT:
                clientReturnTypes.put(schemaId, schema);
                break;
            case SERVER:
                serverReturnTypes.put(schemaId, schema);
            default:
                break;
        }
    }

    private Long getReturnType(RpcTwoPartyProtocol.Side side, int schemaId) {
        switch (side) {
            case CLIENT:
                return clientReturnTypes.get(schemaId);
            case SERVER:
                return serverReturnTypes.get(schemaId);
            default:
                break;
        }
        return -1L;
    }

    private String dumpCap(RpcProtocol.CapDescriptor.Reader cap) {
        return cap.which().toString();
    }
    private String dumpCaps(StructList.Reader<RpcProtocol.CapDescriptor.Reader> capTable) {
        switch (capTable.size()) {
            case 0:
                return "";
            case 1:
                return dumpCap(capTable.get(0));
            default:
            {
                var text = dumpCap(capTable.get(0));
                for (int ii = 1; ii< capTable.size(); ++ii) {
                    text += ", " + dumpCap(capTable.get(ii));
                }
                return text;
            }
        }
    }

    String dump(RpcProtocol.Message.Reader message, RpcTwoPartyProtocol.Side sender) {
        switch (message.which()) {
            case CALL: {
                var call = message.getCall();
                var iface = call.getInterfaceId();

                var interfaceName = String.format("0x%x", iface);
                var methodName = String.format("method#%d", call.getMethodId());
                var payload = call.getParams();
                var params = payload.getContent();
                var sendResultsTo = call.getSendResultsTo();
/*
                var schema = this.schemas.get(iface);
                if (schema != null) {
                    interfaceName = schema.getDisplayName().toString();
                    if (schema.isInterface()) {

                        interfaceName = schema.getDisplayName().toString();
                        var interfaceSchema = schema.getInterface();

                        var methods = interfaceSchema.getMethods();
                        if (call.getMethodId() < methods.size()) {
                            var method = methods.get(call.getMethodId());
                            methodName = method.getName().toString();
                            var paramType = method.getParamStructType();
                            var resultType = method.getResultStructType();

                            if (call.getSendResultsTo().isCaller()) {
                                var questionId = call.getQuestionId();
                                setReturnType(sender, call.getQuestionId(), resultType);
                            }

                        }
                    }
                }*/

                return sender.name() + "(" + call.getQuestionId() + "): call " +
                        call.getTarget() + " <- " + interfaceName + "." +
                        methodName + " " + params.getClass().getName() + " caps:[" +
                        dumpCaps(payload.getCapTable()) + "]" +
                        (sendResultsTo.isCaller() ? "" : (" sendResultsTo:" + sendResultsTo));
            }

            case RETURN: {
                var ret = message.getReturn();
                var text = sender.name() + "(" + ret.getAnswerId() + "): ";
                var returnType = getReturnType(
                        sender == RpcTwoPartyProtocol.Side.CLIENT
                                ? RpcTwoPartyProtocol.Side.SERVER
                                : RpcTwoPartyProtocol.Side.CLIENT,
                        ret.getAnswerId());
                switch (ret.which()) {
                    case RESULTS: {
                        var payload = ret.getResults();
                        return text + "return " + payload +
                                " caps:[" + dumpCaps(payload.getCapTable()) + "]";
                    }
                    case EXCEPTION: {
                        var exc = ret.getException();
                        return text + "exception "
                                + exc.getType().toString() +
                                " " + exc.getReason();
                    }
                    default: {
                        return text + ret.which().name();
                    }
                }
            }

            case BOOTSTRAP: {
                var restore = message.getBootstrap();
                setReturnType(sender, restore.getQuestionId(), 0);
                return sender.name() + "(" + restore.getQuestionId() + "): bootstrap " +
                        restore.getDeprecatedObjectId();
            }

            case ABORT: {
                var abort = message.getAbort();
                return sender.name() + ": abort "
                        + abort.getType().toString()
                        + " \"" + abort.getReason().toString() + "\"";
            }

            case RESOLVE: {
                var resolve = message.getResolve();
                var id = resolve.getPromiseId();
                String text;
                switch (resolve.which()) {
                    case CAP: {
                        var cap = resolve.getCap();
                        text =  cap.which().toString();
                        break;
                    }
                    case EXCEPTION: {
                        var exc = resolve.getException();
                        text = exc.getType().toString() + ": " + exc.getReason().toString();
                        break;
                    }
                    default: text = resolve.which().toString(); break;
                };
                return sender.name() + "(" + id + "): resolve " + text;
            }

            default:
                return sender.name() + ": " + message.which().name();
        }
    }
}
