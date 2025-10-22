#!/bin/bash
set -euo pipefail

echo "Unpacking P2 repository from site artifact..."

mkdir -p "$GITHUB_WORKSPACE/staged-repository"
ZIP=$(ls -1 site-artifact/*.zip | head -n1)
echo "Unzipping $ZIP"
unzip -q "$ZIP" -d "$GITHUB_WORKSPACE/staged-repository"

P2_DIR=$(find "$GITHUB_WORKSPACE/staged-repository" -type f \( -name 'artifacts.jar' -o -name 'artifacts.xml*' \) -printf '%h\n' | head -n1)

if [ -z "$P2_DIR" ]; then
  echo "::error::Failed to locate P2 repository in unzipped site"
  exit 1
fi

echo "P2_DIR=$P2_DIR" >> "$GITHUB_ENV"
echo "P2 repository unpacked to: $P2_DIR"