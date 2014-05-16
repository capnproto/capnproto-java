CXX=clang++
CXX_FLAGS=-std=c++11 -stdlib=libc++ `pkg-config capnp --cflags --libs`

CAPNPC_JAVA_SOURCES=generator/src/main/cpp/compiler/capnpc-java.c++

.PHONY: all clean addressbook

all : capnpc-java

clean :
	rm capnpc-java

capnpc-java : $(CAPNPC_JAVA_SOURCES)
	$(CXX) $(CAPNPC_JAVA_SOURCES) $(CXX_FLAGS) -g -o capnpc-java

addressbook : capnpc-java
	PWD=pwd
	mkdir -p examples/src/main/generated
	capnp compile -I$(PWD)/generator/src/main/cpp/compiler --src-prefix=examples/src/main/schema -o./capnpc-java:examples/src/main/generated examples/src/main/schema/addressbook.capnp
	sbt examples/compile
