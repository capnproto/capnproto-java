CAPNP_CXX_FLAGS=$(shell pkg-config capnp --cflags --libs)

ifeq ($(CAPNP_CXX_FLAGS),)
$(warning "Warning: pkg-config failed to find compilation configuration for capnp.")
$(warning "Falling back to a guess based on the location of the capnp executable.")
CAPNP_PREFIX=$(shell dirname $(shell which capnp))/..
CAPNP_CXX_FLAGS=-I $(CAPNP_PREFIX)/include -L $(CAPNP_PREFIX)/lib -lkj -lcapnp
endif

CXX=g++
CXX_FLAGS=-std=c++11 $(CAPNP_CXX_FLAGS)

CAPNPC_JAVA_SOURCES=compiler/src/main/cpp/capnpc-java.c++

.PHONY: all clean

all : capnpc-java

clean :
	rm -f capnpc-java

capnpc-java : $(CAPNPC_JAVA_SOURCES)
	$(CXX) $(CAPNPC_JAVA_SOURCES) $(CXX_FLAGS) -o capnpc-java

addressbook : capnpc-java
	PWD=pwd
	mkdir -p examples/src/main/generated
	capnp compile -I$(PWD)/compiler/src/main/schema --src-prefix=examples/src/main/schema -o./capnpc-java:examples/src/main/generated examples/src/main/schema/addressbook.capnp
