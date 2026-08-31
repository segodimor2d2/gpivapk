#include <jni.h>
#include <android/log.h>

#define LOG_TAG "GPIV_NATIVE"

#define LOGI(...) \
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C"
JNIEXPORT void JNICALL
Java_com_rec_gpiv_player_MpvNative_nativeInitialize(
    JNIEnv* env,
    jobject thiz
)
{
    LOGI("JNI: initialize()");
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_rec_gpiv_player_MpvNative_nativeGetVersion(
    JNIEnv* env,
    jobject thiz
)
{
    return env->NewStringUTF("GPIV Native 0.1");
}

extern "C"
JNIEXPORT void JNICALL
Java_com_rec_gpiv_player_MpvNative_nativePlay(
    JNIEnv* env,
    jobject thiz
)
{
    LOGI("JNI: play()");
}
