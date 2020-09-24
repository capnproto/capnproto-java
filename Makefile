CAPNP_CXX_FLAGS=$(shell pkg-config capnp --cflags --libs)

ifeq ($(CAPNP_CXX_FLAGS),)
$(warning "Warning: pkg-config failed to find compilation configuration for capnp.")
$(warning "Falling back to a guess based on the location of the capnp executable.")
CAPNP_PREFIX=$(shell dirname $(shell which capnp))/..
CAPNP_CXX_FLAGS=-I $(CAPNP_PREFIX)/include -L $(CAPNP_PREFIX)/lib -lkj -lcapnp
endif

PREFIX ?= /usr/local

CXX=g++
CXX_FLAGS=-std=c++14 $(CAPNP_CXX_FLAGS)

CAPNPC_JAVA_SOURCES=compiler/src/main/cpp/capnpc-java.c++

.PHONY: all clean

all : capnpc-java

clean :
	rm -f capnpc-java capnpc-java.exe

capnpc-java : $(CAPNPC_JAVA_SOURCES)
	$(CXX) $(CAPNPC_JAVA_SOURCES) $(CXX_FLAGS) -o capnpc-java

install:
	mkdir -p ${PREFIX}/bin
	mkdir -p ${PREFIX}/include/capnp/
	cp capnpc-java ${PREFIX}/bin
	cp compiler/src/main/schema/capnp/java.capnp ${PREFIX}/include/capnp/

MINGW_LIBS=~/src/capnproto/c++/build-mingw/.libs/libcapnp.a ~/src/capnproto/c++/build-mingw/.libs/libkj.a
MINGW_CXX=i686-w64-mingw32-g++
MINGW_FLAGS=-O2 -DNDEBUG -I/usr/local/include -std=c++14 -static -static-libgcc -static-libstdc++
capnpc-java.exe : $(CAPNPC_JAVA_SOURCES)
	$(MINGW_CXX) $(MINGW_FLAGS) $(CAPNPC_JAVA_SOURCES) $(MINGW_LIBS) -o capnpc-java.exe
