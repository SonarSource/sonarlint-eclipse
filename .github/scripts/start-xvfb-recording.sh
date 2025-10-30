#!/usr/bin/env bash
set -euo pipefail

RECORDING_NAME="${1:-recording}"
DISPLAY="${DISPLAY:-:10}"

echo "Starting Xvfb on display ${DISPLAY}"
Xvfb "${DISPLAY}" -screen 0 1920x1080x24 > Xvfb.out 2>&1 &

echo "Starting metacity window manager"
metacity --sm-disable --replace &

sleep 10

echo "Starting video recording: ${RECORDING_NAME}.mp4"
ffmpeg -loglevel warning \
  -f x11grab \
  -video_size 1920x1080 \
  -i "${DISPLAY}" \
  -codec:v libx264 \
  -r 12 \
  "${GITHUB_WORKSPACE}/${RECORDING_NAME}.mp4" &