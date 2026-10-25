#include <jni.h>
#include <android/log.h>
#include "frame_accurate_cut.h"
#include <exception>
#include "mpv_context.h"
#include <algorithm>
#include <cmath>
#include <stdexcept>
#include <string>

extern "C"
JNIEXPORT jlongArray JNICALL
Java_com_rec_gpiv_player_MpvNative_nativeTestCutFrames(
    JNIEnv* env, jobject thiz, jint fd, jlong frameA, jlong frameB,
    jdouble positionA, jdouble positionB, jlong handle, jdoubleArray selection
) {
    try {
        VideoCrop crop;
        if (selection) {
            if (env->GetArrayLength(selection) != 4) throw std::runtime_error("Invalid crop selection");
            auto* context = reinterpret_cast<MpvContext*>(handle);
            if (!context || !context->mpv) throw std::runtime_error("Player unavailable");
            auto property = [&](const char* name) {
                double value = 0;
                if (mpv_get_property(context->mpv, name, MPV_FORMAT_DOUBLE, &value) < 0 || !std::isfinite(value))
                    throw std::runtime_error(std::string("Video geometry unavailable: ") + name);
                return value;
            };
            double rect[4];
            env->GetDoubleArrayRegion(selection, 0, 4, rect);
            const double w = property("osd-dimensions/w"), h = property("osd-dimensions/h");
            const double ml = property("osd-dimensions/ml"), mt = property("osd-dimensions/mt");
            const double vw = w - ml - property("osd-dimensions/mr");
            const double vh = h - mt - property("osd-dimensions/mb");
            if (w <= 0 || h <= 0 || vw <= 0 || vh <= 0) throw std::runtime_error("Invalid video geometry");
            const double left = std::clamp((rect[0] * w - ml) / vw, 0.0, 1.0);
            const double top = std::clamp((rect[1] * h - mt) / vh, 0.0, 1.0);
            const double right = std::clamp((rect[2] * w - ml) / vw, 0.0, 1.0);
            const double bottom = std::clamp((rect[3] * h - mt) / vh, 0.0, 1.0);
            if (left >= right || top >= bottom) throw std::runtime_error("Selection outside video");
            const double angle = property("video-out-params/rotate") * std::acos(-1.0) / 180.0;
            const double c = std::cos(angle), s = std::sin(angle);
            // Invert display rotation before cropping decoded source pixels.
            crop = {1, 1, 0, 0};
            for (double x : {left, right}) for (double y : {top, bottom}) {
                const double u = std::clamp(c * (x - 0.5) + s * (y - 0.5) + 0.5, 0.0, 1.0);
                const double v = std::clamp(-s * (x - 0.5) + c * (y - 0.5) + 0.5, 0.0, 1.0);
                crop.left = std::min(crop.left, u); crop.right = std::max(crop.right, u);
                crop.top = std::min(crop.top, v); crop.bottom = std::max(crop.bottom, v);
            }
            char* filters = mpv_get_property_string(context->mpv, "vf");
            const std::string vf = filters ? filters : "";
            mpv_free(filters);
            if (vf.find("gpiv-mirror-horizontal") != std::string::npos) {
                const double oldLeft = crop.left;
                crop.left = 1 - crop.right; crop.right = 1 - oldLeft;
            }
            if (vf.find("gpiv-mirror-vertical") != std::string::npos) {
                const double oldTop = crop.top;
                crop.top = 1 - crop.bottom; crop.bottom = 1 - oldTop;
            }
        }
        const auto result = cutFrameAccurately(fd, "/data/data/com.rec.gpiv/cache/frameA.mov",
            positionA, positionB, crop);
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
