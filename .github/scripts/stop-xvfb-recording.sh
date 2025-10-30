#!/usr/bin/env bash
set -euo pipefail

echo "Stopping ffmpeg recording..."
pkill -SIGINT -f ffmpeg || true

echo "Waiting for ffmpeg to finish writing..."
for i in $(seq 1 30); do
  pgrep ffmpeg >/dev/null || break
  sleep 1
done

echo "Stopping Xvfb..."
pkill -f Xvfb || true

echo "Cleanup complete"