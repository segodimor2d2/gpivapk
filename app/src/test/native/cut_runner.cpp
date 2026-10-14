#include "frame_accurate_cut.h"
#include <fcntl.h>
#include <unistd.h>
#include <cstdio>
#include <cstdlib>
#include <exception>
int main(int argc, char** argv) {
    if (argc != 5) return 2;
    int fd = open(argv[1], O_RDONLY);
    try {
        auto r = cutFrameAccurately(fd, argv[2], atof(argv[3]), atof(argv[4]));
        printf("%lld %lld %d %d\n", (long long)r.startPts, (long long)r.endPts, r.timeBaseNum, r.timeBaseDen);
        close(fd);
        return 0;
    } catch (const std::exception& e) { fprintf(stderr, "%s\n", e.what()); close(fd); return 1; }
}
