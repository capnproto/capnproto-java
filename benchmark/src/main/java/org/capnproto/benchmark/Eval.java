package org.capnproto.benchmark;

import org.capnproto.MessageBuilder;
import org.capnproto.StructList;
import org.capnproto.Text;
import org.capnproto.benchmark.EvalSchema.*;

public class Eval
    extends TestCase<Expression.Factory, Expression.Builder, Expression.Reader,
    EvaluationResult.Factory, EvaluationResult.Builder, EvaluationResult.Reader, Integer> {

    public static int makeExpression(Common.FastRand rng, Expression.Builder exp, int depth) {
        exp.setOp(Operation.values()[rng.nextLessThan(Operation.MODULUS.ordinal() + 1)]);

        int left = 0;
        if (rng.nextLessThan(8) < depth) {
            int tmp = rng.nextLessThan(128) + 1;
            exp.getLeft().setValue(tmp);
            left = tmp;
        } else {
            left = makeExpression(rng, exp.getLeft().initExpression(), depth + 1);
        }

        int right = 0;
        if (rng.nextLessThan(8) < depth) {
            int tmp = rng.nextLessThan(128) + 1;
            exp.getRight().setValue(tmp);
            right = tmp;
        } else {
            right = makeExpression(rng, exp.getRight().initExpression(), depth + 1);
        }

        switch (exp.getOp()) {
        case ADD: return left + right;
        case SUBTRACT: return left - right;
        case MULTIPLY: return left * right;
        case DIVIDE: return Common.div(left, right);
        case MODULUS: return Common.modulus(left, right);
        default:
            throw new Error("impossible");
        }
    }

    public static int evaluateExpression(Expression.Reader exp) {
        int left = 0, right = 0;

        switch (exp.getLeft().which()) {
        case VALUE:
            left = exp.getLeft().getValue();
            break;
        case EXPRESSION:
            left = evaluateExpression(exp.getLeft().getExpression());
        }

        switch (exp.getRight().which()) {
        case VALUE:
            right = exp.getRight().getValue();
            break;
        case EXPRESSION:
            right = evaluateExpression(exp.getRight().getExpression());
        }

        switch (exp.getOp()) {
        case ADD: return left + right;
        case SUBTRACT: return left - right;
        case MULTIPLY: return left * right;
        case DIVIDE: return Common.div(left, right);
        case MODULUS: return Common.modulus(left, right);
        default:
            throw new Error("impossible");
        }
    }

    public final Integer setupRequest(Common.FastRand rng, Expression.Builder request) {
        return makeExpression(rng, request, 0);
    }

    public final void handleRequest(Expression.Reader request, EvaluationResult.Builder response) {
        response.setValue(evaluateExpression(request));
    }

    public final boolean checkResponse(EvaluationResult.Reader response, Integer expected) {
        return response.getValue() == expected;
    }

    public static void main(String[] args) {
        Eval testCase = new Eval();
        testCase.execute(args, Expression.factory, EvaluationResult.factory);
    }
}
