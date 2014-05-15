CXX=clang++ -std=c++11 -stdlib=libc++ `pkg-config capnp --cflags --libs`

CAPNPC_JAVA_SOURCES=generator/src/main/cpp/compiler/capnpc-java.c++

.PHONY: all clean addressbook

all : capnpc-java addressbook

clean :
	rm capnpc-java
	sbt clean

capnpc-java : $(CAPNPC_JAVA_SOURCES)
	$(CXX) -I/usr/local/include -g $(CAPNPC_JAVA_SOURCES) -o capnpc-java

addressbook : capnpc-java
	PWD=pwd
	capnp compile -I$(PWD)/generator/src/main/cpp/compiler --src-prefix=examples/src/main/schema -o./capnpc-java:examples/src/main/generated examples/src/main/schema/addressbook.capnp
	sbt examples/"run read"
