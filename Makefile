all: local test build-compiler generate-benchmarks-schema generate-examples-schema

local: build-compiler build-packages
	mkdir -p output
	cp capnproto-runtime/target/*.jar output/
	cp capnproto-compiler/bin/* output/
	cp capnproto-compiler/schema/java.capnp output/

benchmark: generate-benchmarks-schema local
	cd capnproto-benchmarks && $(MAKE)
	rm -f capnproto-benchmarks/fifo

test:
	mvn test

build-packages:
	mvn package

build-compiler:
	cd capnproto-compiler && $(MAKE)

generate-benchmarks-schema: build-compiler
	cd capnproto-benchmarks/src/main/schema && $(MAKE) benchmarkschema

generate-examples-schema: build-compiler
	cd capnproto-examples/src/main/schema && $(MAKE) exampleschema
