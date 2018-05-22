# capnproto-java cmake build

Of course, capnproto needs to be pre-installed somewhere.

## Using cmake

mkdir build
cd build
cmake -DCAPNP_PKG_PATH=[path of Capnproto pkgconfig directory (with capnp.pc)] [path of CMakeLists.txt]

## Using ccmake gui

mkdir build
cd build
ccmake [path of CMakeLists.txt]
.. set pkgconfig path manually
