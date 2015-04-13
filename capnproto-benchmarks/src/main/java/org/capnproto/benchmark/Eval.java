// Copyright (c) 2013-2014 Sandstorm Development Group, Inc. and contributors
// Licensed under the MIT License:
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package org.capnproto.benchmark;

import org.capnproto.MessageBuilder;
import org.capnproto.StructList;
import org.capnproto.Text;
import org.capnproto.benchmark.EvalSchema.*;

public class Eval
    extends TestCase<Expression.Factory, Expression.Builder, Expression.Reader,
    EvaluationResult.Factory, EvaluationResult.Builder, EvaluationResult.Reader, Integer> {

    static final Operation operations[] = Operation.values();

    public static int makeExpression(Common.FastRand rng, Expression.Builder exp, int depth) {
        exp.setOp(operations[rng.nextLessThan(Operation.MODULUS.ordinal() + 1)]);

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
