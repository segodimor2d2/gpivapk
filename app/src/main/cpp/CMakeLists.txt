cmake_minimum_required(VERSION 3.22.1)

project("gpiv_native")

# ============================================================
# Dependências externas
# ============================================================

# Por padrão, assume a estrutura atual:
#
# 03android/
# ├── gpivapk/
# ├── mpv/
# └── mpv-android/
#
# Os caminhos podem ser sobrescritos externamente com:
#
# -DGPIV_MPV_ROOT=/caminho/para/mpv
# -DGPIV_MPV_ANDROID_ROOT=/caminho/para/mpv-android

set(
    GPIV_MPV_ROOT
    "${CMAKE_CURRENT_LIST_DIR}/../../../../../mpv"
    CACHE PATH
    "Root directory of the MPV source tree"
)

set(
    GPIV_MPV_ANDROID_ROOT
    "${CMAKE_CURRENT_LIST_DIR}/../../../../../mpv-android"
    CACHE PATH
    "Root directory of the mpv-android source tree"
)

set(
    GPIV_FFMPEG_ROOT
    "${GPIV_MPV_ANDROID_ROOT}/buildscripts/deps/ffmpeg"
    CACHE PATH
    "Root directory of the FFmpeg source tree"
)

message(STATUS "GPIV_MPV_ROOT        = ${GPIV_MPV_ROOT}")
message(STATUS "GPIV_MPV_ANDROID_ROOT = ${GPIV_MPV_ANDROID_ROOT}")
message(STATUS "GPIV_FFMPEG_ROOT     = ${GPIV_FFMPEG_ROOT}")

if(NOT EXISTS "${GPIV_MPV_ROOT}/include/mpv/client.h")
    message(FATAL_ERROR
        "MPV não encontrado: ${GPIV_MPV_ROOT}"
    )
endif()

if(NOT EXISTS "${GPIV_MPV_ANDROID_ROOT}/buildscripts/include")
    message(FATAL_ERROR
        "mpv-android não encontrado: ${GPIV_MPV_ANDROID_ROOT}"
    )
endif()

if(NOT EXISTS "${GPIV_FFMPEG_ROOT}/libavcodec/jni.h")
    message(FATAL_ERROR
        "FFmpeg não encontrado: ${GPIV_FFMPEG_ROOT}"
    )
endif()

# ============================================================
# Biblioteca principal
# ============================================================

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


# ============================================================
# Headers externos
# ============================================================

target_include_directories(
    gpiv_native
    PRIVATE
    "${GPIV_FFMPEG_ROOT}"
)


# ============================================================
# MPV
# ============================================================

add_library(
    mpv
    SHARED
    IMPORTED
)


# ============================================================
# FFmpeg / avcodec
# ============================================================

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
        "${GPIV_MPV_ROOT}/include;${GPIV_MPV_ANDROID_ROOT}/buildscripts/include"
)


# ============================================================
# Android libraries
# ============================================================

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


# ============================================================
# Link
# ============================================================

target_link_libraries(
    gpiv_native
    mpv
    avcodec
    ${log-lib}
    ${android-lib}
    ${egl-lib}
    ${gles-lib}
)
