CAPNP_CXX_FLAGS=$(shell pkg-config capnp --cflags --libs)

ifeq ($(CAPNP_CXX_FLAGS),)
	$(warning "Warning: pkg-config failed to find compilation configuration for capnp.")
	$(warning "Falling back to a guess based on the location of the capnp executable.")
	CAPNP_PREFIX=$(shell dirname $(shell which capnp))/..
	CAPNP_CXX_FLAGS=-I $(CAPNP_PREFIX)/include -L $(CAPNP_PREFIX)/lib -lkj -lcapnp
endif

CXX=g++
CXX_FLAGS=-std=gnu++11 $(CAPNP_CXX_FLAGS)

CAPNPC_JAVA_SOURCES=src/capnpc-java.c++

.PHONY: all clean

all : capnpc-java

clean :
	rm -rf bin

capnpc-java : $(CAPNPC_JAVA_SOURCES)
	mkdir -p bin
	$(CXX) $(CAPNPC_JAVA_SOURCES) $(CXX_FLAGS) -o bin/capnpc-java

MINGW_LIBS=~/src/capnproto/c++/build-mingw/.libs/libcapnp.a ~/src/capnproto/c++/build-mingw/.libs/libkj.a
MINGW_CXX=i686-w64-mingw32-g++
MINGW_FLAGS=-O2 -DNDEBUG -I/usr/local/include -std=gnu++11 -static -static-libgcc -static-libstdc++
capnpc-java.exe : $(CAPNPC_JAVA_SOURCES)
	mkdir -p bin
	$(MINGW_CXX) $(MINGW_FLAGS) $(CAPNPC_JAVA_SOURCES) $(MINGW_LIBS) -o bin/capnpc-java.exe
