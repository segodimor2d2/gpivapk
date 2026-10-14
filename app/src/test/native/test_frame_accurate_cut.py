"""Integration test of the production cut routine with host FFmpeg development libraries.
Run: python3 app/src/test/native/test_frame_accurate_cut.py
Requires c++, pkg-config, ffmpeg, ffprobe, libavcodec/format/util and libswscale.
"""
import json
import shlex
import subprocess
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
CPP = ROOT / 'app/src/main/cpp'


def run(*args):
    result = subprocess.run([str(a) for a in args], capture_output=True)
    if result.returncode:
        raise AssertionError(result.stderr.decode())
    return result.stdout


def probe(path):
    return json.loads(run('ffprobe', '-v', 'error', '-select_streams', 'v:0',
                          '-show_frames', '-show_format', '-show_streams', '-of', 'json', path))


def pixels(path):
    raw = run('ffmpeg', '-v', 'error', '-i', path, '-map', '0:v:0',
              '-fps_mode', 'passthrough', '-pix_fmt', 'rgb24', '-f', 'rawvideo', '-')
    size = 160 * 96 * 3
    return [raw[i:i+size] for i in range(0, len(raw), size)]


def check_cut(runner, source, target, a, b, audio):
    info = probe(source)
    frames = info['frames']
    origin = float(info['format']['start_time'])
    times = [float(f['best_effort_timestamp_time']) for f in frames]
    run(runner, source, target, times[a] - origin, times[b] - origin)
    cut = probe(target)
    actual = [float(f['best_effort_timestamp_time']) for f in cut['frames']]
    expected = [t - times[a] for t in times[a:b+1]]
    assert len(actual) == len(expected), (len(actual), len(expected))
    assert all(abs(x-y) < 0.00002 for x, y in zip(actual, expected)), (actual, expected)
    # Verifica o conteúdo dos frames de borda, além da contagem e dos timestamps.
    original, output = pixels(source), pixels(target)
    for output_index, source_index in [(0, a), (-1, b)]:
        candidates = range(max(0, source_index-3), min(len(original), source_index+4))
        def error(index):
            return sum(abs(x-y) for x, y in zip(output[output_index], original[index]))
        best = min(candidates, key=error)
        assert best == source_index, ('wrong boundary frame', best, source_index)
    streams = json.loads(run('ffprobe', '-v', 'error', '-show_streams', '-of', 'json', target))['streams']
    audio_streams = [s for s in streams if s['codec_type'] == 'audio']
    assert bool(audio_streams) == audio
    if audio:
        assert abs(float(audio_streams[0]['start_time'])) < 0.022
        assert abs(float(audio_streams[0]['duration']) - float(cut['streams'][0]['duration'])) < 0.044
    print(f'PASS {source.name}: frames {a}..{b}, {len(actual)} frames, audio={audio}')


def main():
    with tempfile.TemporaryDirectory(prefix='gpiv-cut-') as directory:
        tmp = Path(directory)
        flags = shlex.split(run('pkg-config', '--cflags', '--libs', 'libavcodec',
                                'libavformat', 'libavutil', 'libswscale').decode())
        runner = tmp / 'cut'
        run('c++', '-std=c++17', '-O2', '-I'+str(CPP),
            Path(__file__).with_name('cut_runner.cpp'), CPP/'frame_accurate_cut.cpp',
            '-o', runner, *flags)
        cfr = tmp/'cfr.mp4'
        run('ffmpeg', '-v', 'error', '-y', '-f', 'lavfi', '-i',
            'testsrc2=size=160x96:rate=30:duration=4', '-f', 'lavfi', '-i',
            'sine=frequency=440:sample_rate=48000:duration=4', '-c:v', 'libx264',
            '-g', '60', '-bf', '3', '-c:a', 'aac', cfr)
        check_cut(runner, cfr, tmp/'cfr.mov', 17, 43, True)
        check_cut(runner, cfr, tmp/'eof.mov', 103, 119, True)
        check_cut(runner, cfr, tmp/'start.mov', 0, 11, True)
        check_cut(runner, cfr, tmp/'single.mov', 17, 17, True)
        vfr = tmp/'vfr.mp4'
        run('ffmpeg', '-v', 'error', '-y', '-i', cfr, '-an', '-vf',
            "select='not(eq(mod(n,5),0))'", '-fps_mode', 'vfr', '-c:v', 'libx264',
            '-g', '60', '-bf', '3', vfr)
        check_cut(runner, vfr, tmp/'vfr.mov', 13, 47, False)
        shifted = tmp/'shifted.mp4'
        run('ffmpeg', '-v', 'error', '-y', '-i', cfr, '-c', 'copy',
            '-output_ts_offset', '5', shifted)
        check_cut(runner, shifted, tmp/'shifted.mov', 17, 43, True)
        invalid = tmp/'invalid.mov'
        result = subprocess.run([str(runner), str(cfr), str(invalid), '5', '6'], capture_output=True)
        assert result.returncode != 0 and not invalid.exists()
        print('PASS invalid range: failure without partial output')


if __name__ == '__main__':
    main()
