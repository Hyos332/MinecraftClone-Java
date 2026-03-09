#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR"

export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"

./gradlew run "$@"
