#!/bin/bash
set -euo pipefail

echo "Installing dependencies..."

sudo apt-get update
sudo apt-get install -y \
  build-essential \
  dbus-x11 \
  ffmpeg \
  gettext-base \
  libwebkit2gtk-4.* \
  metacity \
  xvfb

sudo apt-get clean
sudo rm -rf /var/lib/apt/lists/*

echo "UI dependencies installed successfully"