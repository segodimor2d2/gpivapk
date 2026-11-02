"""Test the production packet-copy routine with host FFmpeg libraries.
Run: python3 app/src/test/native/test_keyframe_copy_cut.py
Requires c++, pkg-config, ffmpeg, ffprobe and libavcodec/format/util headers.
"""
import json
import shlex
import subprocess
import tempfile
from pathlib import Path

CPP = Path(__file__).resolve().parents[4] / 'app/src/main/cpp'


def run(*args):
    result = subprocess.run([str(a) for a in args], capture_output=True)
    assert result.returncode == 0, result.stderr.decode()
    return result.stdout


def probe(path):
    return json.loads(run('ffprobe', '-v', 'error', '-show_frames', '-show_streams',
                          '-show_format', '-of', 'json', path))


def hashes(path):
    data = run('ffmpeg', '-v', 'error', '-i', path, '-map', '0:v:0',
               '-fps_mode', 'passthrough', '-f', 'framemd5', '-').decode()
    return [line.rsplit(',', 1)[-1].strip() for line in data.splitlines() if not line.startswith('#')]


def check(runner, source, target, a, b):
    info = probe(source)
    frames = [f for f in info['frames'] if f['media_type'] == 'video']
    origin = float(info['format']['start_time'])
    times = [float(f['best_effort_timestamp_time']) for f in frames]
    keys = [i for i, f in enumerate(frames) if f['key_frame']]
    first = max(i for i in keys if times[i] <= a + origin + 1e-6)
    last = next((i for i in keys if times[i] >= b + origin - 1e-6), len(frames) - 1)
    run(runner, source, target, a, b)
    actual = probe(target)
    output_frames = [f for f in actual['frames'] if f['media_type'] == 'video']
    assert hashes(target) == hashes(source)[first:last+1], (source.name, a, b, first, last)
    actual_times = [float(f['best_effort_timestamp_time']) for f in output_frames]
    expected_times = [t-times[first] for t in times[first:last+1]]
    assert len(actual_times) == len(expected_times)
    assert all(abs(x-y) < .002 for x, y in zip(actual_times, expected_times)), (actual_times, expected_times)
    # Validate packet timing and relative audio offset, not just decoded pictures.
    packets = lambda path: json.loads(run('ffprobe', '-v', 'error', '-show_packets', '-of', 'json', path))['packets']
    source_audio = [p for p in packets(source) if p['codec_type'] == 'audio' and
                    float(p['pts_time']) + float(p['duration_time']) > times[first] and
                    float(p['pts_time']) < times[last] + float(frames[last].get('pkt_duration_time', 1/30))]
    output_audio = [p for p in packets(target) if p['codec_type'] == 'audio']
    if source_audio:
        assert output_audio
        assert abs(float(output_audio[0]['pts_time']) -
                   (float(source_audio[0]['pts_time']) - times[first])) < .002
    print(f'PASS {source.name}: A={a:.3f} B={b:.3f}, frames {first}..{last}, exact decoded content/timing')


def main():
    with tempfile.TemporaryDirectory(prefix='gpiv-copy-cut-') as directory:
        tmp = Path(directory)
        flags = shlex.split(run('pkg-config', '--cflags', '--libs', 'libavcodec', 'libavformat', 'libavutil').decode())
        runner = tmp/'copy-cut'
        run('c++', '-std=c++17', '-O2', '-I'+str(CPP), Path(__file__).with_name('copy_cut_runner.cpp'),
            CPP/'keyframe_copy_cut.cpp', '-o', runner, *flags)
        cfr = tmp/'cfr.mp4'
        run('ffmpeg', '-v', 'error', '-y', '-f', 'lavfi', '-i', 'testsrc2=size=160x96:rate=30:duration=8',
            '-f', 'lavfi', '-i', 'sine=frequency=440:sample_rate=48000:duration=8',
            '-c:v', 'libx264', '-g', '60', '-keyint_min', '60', '-sc_threshold', '0', '-bf', '3', '-c:a', 'aac', cfr)
        mkv = tmp/'millisecond-timebase.mkv'
        run('ffmpeg', '-v', 'error', '-y', '-i', cfr, '-c', 'copy', mkv)
        shifted = tmp/'shifted.mp4'
        run('ffmpeg', '-v', 'error', '-y', '-i', cfr, '-c', 'copy', '-output_ts_offset', '7', shifted)
        vfr = tmp/'vfr.mp4'
        run('ffmpeg', '-v', 'error', '-y', '-i', cfr, '-vf', "select='not(eq(mod(n,5),0))'",
            '-fps_mode', 'vfr', '-c:v', 'libx264', '-g', '48', '-keyint_min', '48', '-sc_threshold', '0',
            '-bf', '3', '-an', vfr)
        for source in [cfr, mkv, shifted, vfr]:
            check(runner, source, tmp/'cut.mov', 2.7, 4.8)
            check(runner, source, tmp/'eof.mov', 6.7, 7.5)
        check(runner, cfr, tmp/'exact.mov', 2, 4)
        check(runner, cfr, tmp/'single.mov', 2, 2)
        check(runner, cfr, tmp/'start.mov', 0, 1)
        # Invalid markers fail and never leave a partial output.
        target = tmp/'invalid.mov'
        result = subprocess.run([str(runner), str(cfr), str(target), '5', '2'], capture_output=True)
        assert result.returncode != 0 and not target.exists()
        print('PASS invalid markers')


if __name__ == '__main__':
    main()
