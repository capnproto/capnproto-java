CXX=g++ -std=c++11

CAPNPC_JAVA_SOURCES=src/compiler/capnpc-java.c++

.PHONY: all clean

all : capnpc-java


clean :
	rm -rf capnpc-java capnp

capnpc-java : $(CAPNPC_JAVA_SOURCES)
	$(CXX) -I/usr/local/include -L/usr/local/lib -lkj -lcapnp $(CAPNPC_JAVA_SOURCES) -o capnpc-java
