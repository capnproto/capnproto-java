CAPNP_CXX_FLAGS=$(shell pkg-config capnp --cflags --libs)

ifeq ($(CAPNP_CXX_FLAGS),)
$(warning "Warning: pkg-config failed to find compilation configuration for capnp.")
$(warning "Falling back to a guess based on the location of the capnp executable.")
CAPNP_PREFIX=$(shell dirname $(shell which capnp))/..
CAPNP_CXX_FLAGS=-I $(CAPNP_PREFIX)/include -L $(CAPNP_PREFIX)/lib -lkj -lcapnp
endif

CXX=g++
CXX_FLAGS=-std=c++14 $(CAPNP_CXX_FLAGS)

CAPNPC_JAVA_SOURCES=compiler/src/main/cpp/capnpc-java.c++

.PHONY: all clean

all : capnpc-java

clean :
	rm -f capnpc-java capnpc-java.exe

capnpc-java : $(CAPNPC_JAVA_SOURCES)
	$(CXX) $(CAPNPC_JAVA_SOURCES) $(CXX_FLAGS) -o capnpc-java


MINGW_LIBS=~/src/capnproto/c++/build-mingw/.libs/libcapnp.a ~/src/capnproto/c++/build-mingw/.libs/libkj.a
MINGW_CXX=i686-w64-mingw32-g++
MINGW_FLAGS=-O2 -DNDEBUG -I/usr/local/include -std=c++14 -static -static-libgcc -static-libstdc++
capnpc-java.exe : $(CAPNPC_JAVA_SOURCES)
	$(MINGW_CXX) $(MINGW_FLAGS) $(CAPNPC_JAVA_SOURCES) $(MINGW_LIBS) -o capnpc-java.exe

addressbook : capnpc-java
	PWD=pwd
	mkdir -p examples/src/main/generated
	capnp compile -I$(PWD)/compiler/src/main/schema --src-prefix=examples/src/main/schema -o./capnpc-java:examples/src/main/generated examples/src/main/schema/addressbook.capnp
