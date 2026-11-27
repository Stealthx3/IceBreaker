#!/usr/bin/env python3
"""
Turns the downloads of a fresh desktop-only Gradle build into flatpak-builder `file` sources.

Input: a log of the build's `--debug` lines ("Performing HTTP GET: <url>" and "Resource missing")
and the Gradle home the build filled. Every fetched Maven file becomes a source laid out under
`offline-repository/` with its repository path, so one local Maven repository replaces Google,
Maven Central and the plugin portal for an `--offline` build. Each checksum is taken from the file
Gradle cached; a file the cache does not hold is downloaded once to hash it. The capture runs on
x86_64, so each `-linux-x64` file is limited to x86_64 and gets its `-linux-arm64` twin for aarch64.

usage: gradle-sources.py <capture.log> <gradle-home> <out.json>
"""
import hashlib
import json
import re
import sys
import urllib.request
from pathlib import Path

REPOSITORIES = (
    "https://repo.maven.apache.org/maven2/",
    "https://dl.google.com/dl/android/maven2/",
    "https://plugins.gradle.org/m2/",
)
GET = re.compile(r"Performing HTTP GET: (\S+)")
HOST_ARCH_MARK = "-linux-x64"
ARM_ARCH_MARK = "-linux-arm64"
MISSING = re.compile(r"Resource missing\. \[HTTP GET: (\S+)\]")


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1 << 20), b""):
            digest.update(block)
    return digest.hexdigest()


def cached(home: Path, relative: str) -> Path | None:
    parts = relative.split("/")
    if len(parts) < 4:
        return None
    group, module, version, name = ".".join(parts[:-3]), parts[-3], parts[-2], parts[-1]
    folder = home / "caches/modules-2/files-2.1" / group / module / version
    return next(folder.glob(f"*/{name}"), None)


def with_arch_twins(sources: list[dict]) -> list[dict]:
    result = []
    for source in sources:
        if HOST_ARCH_MARK not in source["url"]:
            result.append(source)
            continue
        result.append({**source, "only-arches": ["x86_64"]})
        url = source["url"].replace(HOST_ARCH_MARK, ARM_ARCH_MARK)
        with urllib.request.urlopen(url) as response:
            digest = hashlib.sha256(response.read()).hexdigest()
        result.append({
            **source,
            "url": url,
            "sha256": digest,
            "dest": source["dest"].replace(HOST_ARCH_MARK, ARM_ARCH_MARK),
            "dest-filename": source["dest-filename"].replace(HOST_ARCH_MARK, ARM_ARCH_MARK),
            "only-arches": ["aarch64"],
        })
    return result


def main(log: Path, home: Path, out: Path) -> None:
    text = log.read_text()
    missing = set(MISSING.findall(text))
    urls = sorted({url for url in GET.findall(text) if url not in missing})
    sources, skipped = [], []
    for url in urls:
        base = next((repo for repo in REPOSITORIES if url.startswith(repo)), None)
        if base is None or url.endswith("maven-metadata.xml"):
            skipped.append(url)
            continue
        relative = url[len(base):]
        local = cached(home, relative)
        if local is not None:
            digest = sha256(local)
        else:
            with urllib.request.urlopen(url) as response:
                digest = hashlib.sha256(response.read()).hexdigest()
        folder, name = relative.rsplit("/", 1)
        sources.append({
            "type": "file",
            "url": url,
            "sha256": digest,
            "dest": f"offline-repository/{folder}",
            "dest-filename": name,
        })
    sources = with_arch_twins(sources)
    out.write_text(json.dumps(sources, indent=2) + "\n")
    print(f"{len(sources)} sources, {len(skipped)} skipped")
    for url in skipped:
        print(f"  skipped {url}")


if __name__ == "__main__":
    main(Path(sys.argv[1]), Path(sys.argv[2]), Path(sys.argv[3]))
