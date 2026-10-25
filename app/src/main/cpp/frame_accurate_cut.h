#pragma once

#include <cstdint>
#include <string>

struct AccurateCutResult {
    int64_t startPts;
    int64_t endPts;
    int timeBaseNum;
    int timeBaseDen;
};

struct VideoCrop {
    double left = 0, top = 0, right = 1, bottom = 1;
};

// A e B são tempos de apresentação em segundos; ambos os frames são incluídos.
// Lança std::runtime_error e remove a saída incompleta em caso de falha.
AccurateCutResult cutFrameAccurately(int fd, const std::string& outputPath,
                                   double positionA, double positionB, const VideoCrop& crop = {});
