package org.capnproto;

import java.util.HashMap;
import java.util.Map;

public class RpcDumper {

    private final Map<Long, Schema.Node.Reader> schemas = new HashMap<>();
    private final Map<Integer, Long> clientReturnTypes = new HashMap<>();
    private final Map<Integer, Long> serverReturnTypes = new HashMap<>();

    void addSchema(long schemaId, Schema.Node.Reader node) {
        this.schemas.put(schemaId, node);
    }

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

    String dump(RpcProtocol.Message.Reader message, RpcTwoPartyProtocol.Side sender) {
        switch (message.which()) {
            case CALL: {
                var call = message.getCall();
                var iface = call.getInterfaceId();
                var schema = this.schemas.get(iface);
                if (schema == null || !schema.isInterface()) {
                    break;
                }

                var interfaceSchema = schema.getInterface();

                var methods = interfaceSchema.getMethods();
                if (call.getMethodId() >= methods.size()) {
                    break;
                }

                var method = methods.get(call.getMethodId());
                var interfaceName = schema.getDisplayName().toString();
                var paramType = method.getParamStructType();
                var resultType = method.getResultStructType();

                if (call.getSendResultsTo().isCaller()) {
                    var questionId = call.getQuestionId();
                    setReturnType(sender, call.getQuestionId(), resultType);
                }

                var payload = call.getParams();
                var params = payload.getContent();
                var sendResultsTo = call.getSendResultsTo();

                return sender.name() + "(" + call.getQuestionId() + "): call " +
                        call.getTarget() + " <- " + interfaceName + "." +
                        method.getName().toString() + " " + params + " caps:[" +
                        payload.getCapTable() + "]" + (sendResultsTo.isCaller() ? "" : (" sendResultsTo:" + sendResultsTo));
            }

            case RETURN: {
                var ret = message.getReturn();
                var returnType = getReturnType(
                        sender == RpcTwoPartyProtocol.Side.CLIENT
                                ? RpcTwoPartyProtocol.Side.SERVER
                                : RpcTwoPartyProtocol.Side.CLIENT,
                        ret.getAnswerId());
                if (ret.which() != RpcProtocol.Return.Which.RESULTS) {
                    break;
                }
                var payload = ret.getResults();
                return sender.name() + "(" + ret.getAnswerId() + "): return " + payload +
                        " caps:[" + payload.getCapTable() + "]";
            }

            case BOOTSTRAP: {
                var restore = message.getBootstrap();
                setReturnType(sender, restore.getQuestionId(), 0);
                return sender.name() + "(" + restore.getQuestionId() + "): bootstrap " +
                        restore.getDeprecatedObjectId();
            }
            default:
                break;
        }
        return "";
    }
}
