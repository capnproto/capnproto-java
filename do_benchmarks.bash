#! /bin/bash

set -x
shopt -s expand_aliases

rm -f fifo;
mkfifo fifo

alias run_java="java -cp runtime/target/scala-2.11/classes:benchmark/target/scala-2.11/classes"

ITERS=100000

time run_java org.capnproto.benchmark.CarSales object b none $ITERS
time run_java org.capnproto.benchmark.CarSales bytes b none $ITERS
time run_java org.capnproto.benchmark.CarSales bytes b packed $ITERS
time run_java org.capnproto.benchmark.CarSales client b none $ITERS < fifo | run_java org.capnproto.benchmark.CarSales server b none $ITERS > fifo
time run_java org.capnproto.benchmark.CarSales client b packed $ITERS < fifo | run_java org.capnproto.benchmark.CarSales server b packed $ITERS > fifo


ITERS=10000
time run_java org.capnproto.benchmark.CatRank object b none $ITERS
time run_java org.capnproto.benchmark.CatRank bytes b none $ITERS
time run_java org.capnproto.benchmark.CatRank bytes b packed $ITERS
time run_java org.capnproto.benchmark.CatRank client b none $ITERS < fifo | run_java org.capnproto.benchmark.CatRank server b none $ITERS > fifo
time run_java org.capnproto.benchmark.CatRank client b packed $ITERS < fifo | run_java org.capnproto.benchmark.CatRank server b packed $ITERS > fifo


ITERS=2000000
time run_java org.capnproto.benchmark.Eval object b none $ITERS
time run_java org.capnproto.benchmark.Eval bytes b none $ITERS
time run_java org.capnproto.benchmark.Eval bytes b packed $ITERS
time run_java org.capnproto.benchmark.Eval client b none $ITERS < fifo | run_java org.capnproto.benchmark.Eval server b none $ITERS > fifo
time run_java org.capnproto.benchmark.Eval client b packed $ITERS < fifo | run_java org.capnproto.benchmark.Eval server b packed $ITERS > fifo

