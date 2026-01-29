#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

usage() {
  cat <<'USAGE'
Usage: scripts/rebuild-feature-patches.sh [targets...] [-- <gradle args>]

Targets:
  all          Rebuild Minecraft + Paper server + Paper API feature patches (default)
  minecraft    Rebuild Minecraft feature patches
  paper        Rebuild Paper server feature patches
  paper-api    Rebuild Paper API feature patches

Examples:
  scripts/rebuild-feature-patches.sh
  scripts/rebuild-feature-patches.sh minecraft
  scripts/rebuild-feature-patches.sh paper paper-api -- --stacktrace --no-daemon
USAGE
}

gradle_args=()
targets=()
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
    -*)
      gradle_args+=("$1")
      shift
      ;;
    *)
      targets+=("$1")
      shift
      ;;
  esac
done

if [[ ${#targets[@]} -eq 0 ]]; then
  targets=(all)
fi

final_targets=()
for target in "${targets[@]}"; do
  case "$target" in
    all)
      final_targets+=(minecraft paper paper-api)
      ;;
    *)
      final_targets+=("$target")
      ;;
  esac
done

has_commits_ahead_of_file() {
  local repo="$1"
  git -C "$repo" rev-parse --verify -q file >/dev/null 2>&1 || return 1
  local count
  count="$(git -C "$repo" rev-list --count file..HEAD)"
  [[ "$count" != "0" ]]
}

note_no_commits() {
  local label="$1"
  local repo="$2"
  echo "Skipping $label: no commits ahead of 'file' in $repo."
  echo "Tip: commit your change in that repo first, then rerun."
}

run_gradle() {
  (cd "$ROOT" && ./gradlew "$@" "${gradle_args[@]}")
}

ran_any=0

for target in "${final_targets[@]}"; do
  case "$target" in
    minecraft)
      if ! has_commits_ahead_of_file "$ROOT/mirage-server/src/minecraft/java"; then
        note_no_commits "Minecraft feature patches" "$ROOT/mirage-server/src/minecraft/java"
        continue
      fi
      run_gradle :mirage-server:rebuildMinecraftFeaturePatches \
        -x :mirage-server:rebuildMinecraftFilePatches
      ran_any=1
      ;;
    paper)
      if ! has_commits_ahead_of_file "$ROOT/paper-server"; then
        note_no_commits "Paper server feature patches" "$ROOT/paper-server"
        continue
      fi
      run_gradle :mirage-server:rebuildPaperServerFeaturePatches \
        -x :mirage-server:rebuildPaperServerFilePatches \
        -x :mirage-server:rebuildServerFilePatches
      ran_any=1
      ;;
    paper-api)
      if ! has_commits_ahead_of_file "$ROOT/paper-api"; then
        note_no_commits "Paper API feature patches" "$ROOT/paper-api"
        continue
      fi
      run_gradle :rebuildPaperApiFeaturePatches \
        -x :rebuildPaperApiFilePatches
      ran_any=1
      ;;
    *)
      echo "Unknown target: $target"
      usage
      exit 2
      ;;
  esac
done

if [[ "$ran_any" -eq 0 ]]; then
  echo "No feature patches rebuilt."
  exit 1
fi
