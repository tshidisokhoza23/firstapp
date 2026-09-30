#!/usr/bin/env bash
set -e

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$DIR"

docker run --rm \
  --user $(id -u):$(id -g) \
  -e HOME=/tmp \
  -e LOGNAME=$USER \
  -e USER=$USER \
  -v "$DIR":/work \
  -w /work \
  swift:5.10 \
  bash -c "git clone --depth 1 --branch 2.42.0 https://github.com/yonaskolb/XcodeGen.git /tmp/xcodegen && cd /tmp/xcodegen && swift build -c release && /tmp/xcodegen/.build/release/xcodegen generate --spec /work/project.yml --project /work"

