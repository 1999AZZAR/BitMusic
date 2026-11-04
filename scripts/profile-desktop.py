#!/usr/bin/env python3
"""Profile a packaged Linux desktop build with isolated state, offline networking and muted local playback."""

import argparse
import hashlib
import json
import os
from pathlib import Path
import statistics
import struct
import subprocess
import tempfile
import time
import wave


def process_tree(root):
    pending, found = [root], []
    while pending:
        pid = pending.pop()
        try:
            pending.extend(int(value) for value in Path(f"/proc/{pid}/task/{pid}/children").read_text().split())
            found.append(pid)
        except FileNotFoundError:
            pass
    return found


def sample(root):
    result = dict(rss_kib=0, pss_kib=0, swap_kib=0, threads=0, processes=0, cpu_ticks=0)
    for pid in process_tree(root):
        try:
            fields = Path(f"/proc/{pid}/stat").read_text().rsplit(")", 1)[1].split()
            result["cpu_ticks"] += int(fields[11]) + int(fields[12])
            result["threads"] += int(fields[17])
            for line in Path(f"/proc/{pid}/smaps_rollup").read_text().splitlines():
                key, _, value = line.partition(":")
                if key in ("Rss", "Pss", "Swap"):
                    result[{"Rss": "rss_kib", "Pss": "pss_kib", "Swap": "swap_kib"}[key]] += int(value.split()[0])
            result["processes"] += 1
        except (FileNotFoundError, ProcessLookupError):
            pass
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("app_dir", type=Path, help="Directory containing lib/app/BitMusic.cfg")
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--time-scale", type=float, default=1.0, help="0.5 to 2; compare runs with the same value")
    args = parser.parse_args()
    if not 0.5 <= args.time_scale <= 2:
        parser.error("Time scale must be between 0.5 and 2")
    app = args.app_dir.resolve() / "lib/app"
    config = app / "BitMusic.cfg"
    if not config.is_file():
        parser.error(f"Missing packaged application: {config}")
    options = []
    artifacts = {}
    for line in config.read_text().splitlines():
        if line.startswith("java-options="):
            option = line.removeprefix("java-options=").replace("$APPDIR", str(app))
            if not option.startswith("-Dbitchord.") or option.startswith("-Dbitchord.version="):
                options.append(option)
        elif line.startswith("app.classpath="):
            artifact = Path(line.removeprefix("app.classpath=").replace("$APPDIR", str(app)))
            if artifact.name.startswith(("desktopApp-", "desktopUi-", "shared-jvm-")):
                artifacts[artifact.name] = hashlib.sha256(artifact.read_bytes()).hexdigest()
    with tempfile.TemporaryDirectory(prefix="bitmusic-profile-", dir="/var/tmp/scratch") as temp:
        work = Path(temp)
        audio = work / "probe.wav"
        with wave.open(str(audio), "wb") as output:
            output.setparams((2, 2, 44100, 0, "NONE", "not compressed"))
            import math
            block = b"".join(struct.pack("<hh", value, value) for value in
                             (int(8000 * math.sin(2 * math.pi * 440 * index / 44100)) for index in range(44100)))
            for _ in range(600):
                output.writeframesraw(block)
        source = Path(__file__).parent / "profiling/DesktopResourceScenario.java"
        subprocess.run(["javac", "-d", str(work), str(source)], check=True)
        env = dict(os.environ, XDG_STATE_HOME=str(work / "state"), XDG_CACHE_HOME=str(work / "cache"),
                   XDG_CONFIG_HOME=str(work / "config"), XDG_DATA_HOME=str(work / "data"),
                   DBUS_SESSION_BUS_ADDRESS=f"unix:path={work}/no-session-bus")
        command = ["java", *options, f"-Duser.home={work}", f"-Djava.util.prefs.userRoot={work}/preferences",
                   "-cp", f"{work}:{app}/*", "DesktopResourceScenario", str(audio), str(args.time_scale)]
        log_path = args.output.with_suffix(".log")
        args.output.parent.mkdir(parents=True, exist_ok=True)
        samples = []
        with log_path.open("w") as log:
            process = subprocess.Popen(command, env=env, stdout=log, stderr=log)
            started, previous_at, previous = time.monotonic(), time.monotonic(), sample(process.pid)
            phase = "startup"
            try:
                while process.poll() is None:
                    time.sleep(1)
                    now, current = time.monotonic(), sample(process.pid)
                    elapsed = now - started
                    markers = [line.removeprefix("PROFILE_PHASE=") for line in log_path.read_text().splitlines()
                               if line.startswith("PROFILE_PHASE=")]
                    if markers:
                        phase = markers[-1]
                    cpu = max(0, current["cpu_ticks"] - previous["cpu_ticks"]) / os.sysconf("SC_CLK_TCK") / (now - previous_at) * 100
                    if current["processes"]:
                        samples.append(dict(seconds=round(elapsed, 2), phase=phase, cpu_percent=round(cpu, 2), **current))
                    previous_at, previous = now, current
                    if elapsed > 170 * args.time_scale:
                        raise TimeoutError("Desktop scenario did not finish; inspect the log")
                if process.returncode:
                    raise RuntimeError(f"Desktop scenario exited {process.returncode}; inspect {log_path}")
            finally:
                if process.poll() is None:
                    process.terminate()
                    try:
                        process.wait(timeout=10)
                    except subprocess.TimeoutExpired:
                        process.kill()
                        process.wait()
        log_text = log_path.read_text()
        for phase in ("idle", "playback", "hidden-playback", "hidden-paused", "visible-paused"):
            if f"PROFILE_PHASE={phase}" not in log_text:
                raise RuntimeError(f"Scenario did not reach {phase}; inspect {log_path}")
        if "loudness: 'Resource probe'" not in log_text or "audio output opened:" not in log_text:
            raise RuntimeError(f"Local audio fixture did not play; inspect {log_path}")
        summary = {}
        for phase in sorted({row["phase"] for row in samples}):
            rows = [row for row in samples if row["phase"] == phase][5:]
            if rows:
                summary[phase] = {key: round(statistics.median(row[key] for row in rows), 2)
                                  for key in ("rss_kib", "pss_kib", "cpu_percent", "threads", "processes", "swap_kib")}
        args.output.write_text(json.dumps(dict(app_dir=str(args.app_dir), workload="offline muted local WAV, default UI",
                                             artifacts=artifacts, time_scale=args.time_scale,
                                             summary=summary, samples=samples), indent=2) + "\n")
        print(json.dumps(summary, indent=2))


if __name__ == "__main__":
    main()
