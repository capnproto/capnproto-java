RUN_JAVA=java -cp "../output/*:target/classes/"

benchmarks:
	mkfifo fifo
	time $(RUN_JAVA) org.capnproto.benchmark.CarSales object no-reuse none 10000
	time $(RUN_JAVA) org.capnproto.benchmark.CarSales bytes no-reuse none 10000
	time $(RUN_JAVA) org.capnproto.benchmark.CarSales bytes no-reuse packed 10000
	time $(RUN_JAVA) org.capnproto.benchmark.CarSales client no-reuse none 10000 < fifo | $(RUN_JAVA) org.capnproto.benchmark.CarSales server no-reuse none 10000 > fifo
	time $(RUN_JAVA) org.capnproto.benchmark.CarSales client no-reuse packed 10000 < fifo | $(RUN_JAVA) org.capnproto.benchmark.CarSales server no-reuse packed 10000 > fifo
	time $(RUN_JAVA) org.capnproto.benchmark.CatRank object no-reuse none 100000
	time $(RUN_JAVA) org.capnproto.benchmark.CatRank bytes no-reuse none 100000
	time $(RUN_JAVA) org.capnproto.benchmark.CatRank bytes no-reuse packed 100000
	time $(RUN_JAVA) org.capnproto.benchmark.CatRank client no-reuse none 100000 < fifo | $(RUN_JAVA) org.capnproto.benchmark.CatRank server no-reuse none 100000 > fifo
	time $(RUN_JAVA) org.capnproto.benchmark.CatRank client no-reuse packed 100000 < fifo | $(RUN_JAVA) org.capnproto.benchmark.CatRank server no-reuse packed 100000 > fifo
	time $(RUN_JAVA) org.capnproto.benchmark.Eval object no-reuse none 2000000
	time $(RUN_JAVA) org.capnproto.benchmark.Eval bytes no-reuse none 2000000
	time $(RUN_JAVA) org.capnproto.benchmark.Eval bytes no-reuse packed 2000000
	time $(RUN_JAVA) org.capnproto.benchmark.Eval client no-reuse none 2000000 < fifo | $(RUN_JAVA) org.capnproto.benchmark.Eval server no-reuse none 2000000 > fifo
	time $(RUN_JAVA) org.capnproto.benchmark.Eval client no-reuse packed 2000000 < fifo | $(RUN_JAVA) org.capnproto.benchmark.Eval server no-reuse packed 2000000 > fifo
	rm fifo
