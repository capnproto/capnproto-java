CXX=g++ -std=c++11

CAPNPC_JAVA_SOURCES=src/compiler/capnpc-java.c++

.PHONY: all clean addressbook

all : capnpc-java addressbook


clean :
	rm -rf capnpc-java capnp

capnpc-java : $(CAPNPC_JAVA_SOURCES)
	$(CXX) -I/usr/local/include -L/usr/local/lib -lkj -lcapnp $(CAPNPC_JAVA_SOURCES) -o capnpc-java


addressbook : capnpc-java
	capnp compile -o ./capnpc-java examples/addressbook.capnp
