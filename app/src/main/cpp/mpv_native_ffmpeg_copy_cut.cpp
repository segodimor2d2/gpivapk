#include <jni.h>
#include <android/log.h>
#include "keyframe_copy_cut.h"
#include <exception>

extern "C"
JNIEXPORT jlongArray JNICALL
Java_com_rec_gpiv_player_MpvNative_nativeTestCopyCutFrames(
    JNIEnv* env, jobject, jint fd, jlong frameA, jlong frameB,
    jdouble positionA, jdouble positionB
) {
    try {
        const auto result = copyCutAtKeyframes(fd, "/data/data/com.rec.gpiv/cache/frameA.mov",
            positionA, positionB);
        __android_log_print(ANDROID_LOG_INFO, "GPIV_NATIVE",
            "FFmpeg COPY: markers=%.6f..%.6f resolved PTS=%lld..%lld time_base=%d/%d",
            positionA, positionB, static_cast<long long>(result.startPts),
            static_cast<long long>(result.endPts), result.timeBaseNum, result.timeBaseDen);
        jlongArray array = env->NewLongArray(6);
        if (array) {
            jlong values[] = {frameA, result.startPts, frameB, result.endPts,
                result.timeBaseNum, result.timeBaseDen};
            env->SetLongArrayRegion(array, 0, 6, values);
        }
        return array;
    } catch (const std::exception& error) {
        __android_log_print(ANDROID_LOG_ERROR, "GPIV_NATIVE", "FFmpeg COPY: %s", error.what());
        return nullptr;
    }
}
