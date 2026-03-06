#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR"

mkdir -p out
find src/main/java -name "*.java" > .sources.list
javac -encoding UTF-8 -d out @.sources.list
rm -f .sources.list

java -cp out com.minecraftclone.Main
