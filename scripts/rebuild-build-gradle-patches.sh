#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

usage() {
  cat <<'USAGE'
Usage: scripts/rebuild-build-gradle-patches.sh [-- <gradle args>]

Rebuilds single-file patches for build.gradle.kts (mirage-server + mirage-api).
This uses the paperweight task: rebuildPaperSingleFilePatches

Examples:
  scripts/rebuild-build-gradle-patches.sh
  scripts/rebuild-build-gradle-patches.sh -- --stacktrace --no-daemon
USAGE
}

gradle_args=()
while [[ $# -gt 0 ]]; do
  case "$1" in
    -h|--help)
      usage
      exit 0
      ;;
    --)
      shift
      gradle_args+=("$@")
      break
      ;;
    *)
      gradle_args+=("$1")
      shift
      ;;
  esac
done

if [[ ! -f "$ROOT/mirage-server/build.gradle.kts" && ! -f "$ROOT/mirage-api/build.gradle.kts" ]]; then
  echo "No mirage build.gradle.kts files found under $ROOT."
  exit 1
fi

(cd "$ROOT" && ./gradlew :rebuildPaperSingleFilePatches "${gradle_args[@]}")
