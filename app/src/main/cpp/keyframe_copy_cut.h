#pragma once
#include "frame_accurate_cut.h"

// Copy packets between keyframes enclosing the presentation-time markers.
// The final keyframe is included. If no later keyframe exists, use EOF.
AccurateCutResult copyCutAtKeyframes(int fd, const std::string& outputPath,
                                    double positionA, double positionB);
