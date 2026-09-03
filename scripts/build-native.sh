#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "$ROOT_DIR"

if ! command -v cargo >/dev/null 2>&1; then
  echo "cargo is required to build the native workspace." >&2
  exit 1
fi

echo "==> Building native libraries"
cargo build \
  --manifest-path native/Cargo.toml \
  --release \
  --package dreamdisplays-native \
  --package dreamdisplays-lav \
  --locked

echo "==> Testing native libraries"
cargo test \
  --manifest-path native/Cargo.toml \
  --release \
  --package dreamdisplays-native \
  --package dreamdisplays-lav \
  --locked

echo "==> Native build completed"