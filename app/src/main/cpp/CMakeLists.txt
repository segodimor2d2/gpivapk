cmake_minimum_required(VERSION 3.22.1)

project("gpiv_native")

add_library(
    gpiv_native
    SHARED
    mpv_native.cpp
    mpv_context.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    gpiv_native
    ${log-lib}
)
