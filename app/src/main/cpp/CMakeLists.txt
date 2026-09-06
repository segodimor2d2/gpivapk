cmake_minimum_required(VERSION 3.22.1)

project("gpiv_native")

add_library(
    gpiv_native
    SHARED
    mpv_native.cpp
    mpv_context.cpp
    mpv_context_egl.cpp
    mpv_context_render.cpp
    mpv_context_surface.cpp
    mpv_context_events.cpp
    mpv_context_mpv.cpp
    mpv_context_jni.cpp
    mpv_context_stream.cpp
)

target_include_directories(
    gpiv_native
    PRIVATE
    "${CMAKE_CURRENT_LIST_DIR}/../../../../../mpv-android/buildscripts/deps/ffmpeg"
)

add_library(
    mpv
    SHARED
    IMPORTED
)

add_library(
    avcodec
    SHARED
    IMPORTED
)

set_target_properties(
    avcodec
    PROPERTIES
    IMPORTED_LOCATION
        "${CMAKE_CURRENT_LIST_DIR}/../jniLibs/arm64-v8a/libavcodec.so"
)

set_target_properties(
    mpv
    PROPERTIES
    IMPORTED_LOCATION
        "${CMAKE_CURRENT_LIST_DIR}/../jniLibs/arm64-v8a/libmpv.so"

    INTERFACE_INCLUDE_DIRECTORIES
        "${CMAKE_CURRENT_LIST_DIR}/../../../../../mpv/include;${CMAKE_CURRENT_LIST_DIR}/../../../../../mpv-android/buildscripts/include"
)

find_library(
    log-lib
    log
)

find_library(
    android-lib
    android
)

find_library(
    egl-lib
    EGL
)

find_library(
    gles-lib
    GLESv2
)

target_link_libraries(
    gpiv_native
    mpv
    avcodec
    ${log-lib}
    ${android-lib}
    ${egl-lib}
    ${gles-lib}
)
