project(capnproto-java)

cmake_minimum_required(VERSION 3.9)

find_package(PkgConfig REQUIRED)

set(CMAKE_CXX_STANDARD 11)
set(CAPNPC_JAVA_SOURCES ../compiler/src/main/cpp/capnpc-java.c++)

if(CAPNP_PKG_PATH)
  set(ENV{PKG_CONFIG_PATH} "$ENV{PKG_CONFIG_PATH}:${CAPNP_PKG_PATH}")
else()
  find_path(CAPNP_PKG_PATH_VAR "capnp/lib/pkgconfig" HINTS ${CAPNP_PKG_PATH})
  set(ENV{PKG_CONFIG_PATH} "$ENV{PKG_CONFIG_PATH}:${CAPNP_PKG_PATH_VAR}")
endif()

pkg_search_module(PKG_CAPNP REQUIRED capnp)

if(PKG_CAPNP_FOUND)
  message(STATUS "Capnp found!")
  add_executable(capnpc-java
    ${CAPNPC_JAVA_SOURCES}
  )

  target_include_directories(capnpc-java
    PRIVATE
      ${PKG_CAPNP_INCLUDE_DIRS}
  )

  target_link_libraries(capnpc-java
    ${PKG_CAPNP_LDFLAGS}
  )
else()
  message(FATAL_ERROR "Capnp not found!")
endif()
