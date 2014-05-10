CXX=g++ -std=c++11

CAPNP_SOURCES=\
	src/capnp/ListPointer.java\
	src/capnp/PointerReader.java\
	src/capnp/SegmentReader.java\
	src/capnp/StructReader.java\
	src/capnp/Text.java\
	src/capnp/WireHelpers.java\
	src/capnp/WirePointer.java\
	src/capnp/WordPointer.java

CAPNPC_JAVA_SOURCES=src/compiler/capnpc-java.c++

.PHONY: all clean addressbook

all : capnpc-java addressbook capnp

clean :
	rm -rf capnpc-java capnp

capnp : capnp/PointerReader.class

capnp/PointerReader.class : $(CAPNP_SOURCES)
	javac -d . $(CAPNP_SOURCES)

capnpc-java : $(CAPNPC_JAVA_SOURCES)
	$(CXX) -I/usr/local/include -L/usr/local/lib -lkj -lcapnp $(CAPNPC_JAVA_SOURCES) -o capnpc-java


addressbook : capnpc-java
	capnp compile -o ./capnpc-java examples/addressbook.capnp
