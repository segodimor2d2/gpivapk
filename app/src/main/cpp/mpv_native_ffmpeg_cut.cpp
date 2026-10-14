#include <jni.h>
#include <android/log.h>
#include "frame_accurate_cut.h"
#include <exception>

extern "C"
JNIEXPORT jlongArray JNICALL
Java_com_rec_gpiv_player_MpvNative_nativeTestCutFrames(
    JNIEnv* env, jobject thiz, jint fd, jlong frameA, jlong frameB,
    jdouble positionA, jdouble positionB
) {
    try {
        const auto result = cutFrameAccurately(fd, "/data/data/com.rec.gpiv/cache/frameA.mov",
            positionA, positionB);
        jlongArray array = env->NewLongArray(6);
        if (array) {
            jlong values[] = {frameA, result.startPts, frameB, result.endPts,
                result.timeBaseNum, result.timeBaseDen};
            env->SetLongArrayRegion(array, 0, 6, values);
        }
        return array;
    } catch (const std::exception& error) {
        __android_log_print(ANDROID_LOG_ERROR, "GPIV_NATIVE", "Frame accurate cut: %s", error.what());
        return nullptr;
    }
}
