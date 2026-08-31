cmake_minimum_required(VERSION 3.22.1)

project("gpiv_native")

add_library(
    gpiv_native
    SHARED
    mpv_native.cpp
    mpv_context.cpp
)

add_library(
    mpv
    SHARED
    IMPORTED
)

set_target_properties(
    mpv
    PROPERTIES
    IMPORTED_LOCATION
        "${CMAKE_CURRENT_LIST_DIR}/../jniLibs/arm64-v8a/libmpv.so"

    INTERFACE_INCLUDE_DIRECTORIES
        "/home/segodimo/05android/mpv/include"
)

find_library(
    log-lib
    log
)

target_link_libraries(
    gpiv_native
    mpv
    ${log-lib}
)
