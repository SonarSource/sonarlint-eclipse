#!/usr/bin/env bash
set -euo pipefail

# Copy settings file
cp .github/scripts/settings_${1-latest-java-11_e424}.xml "$HOME/.m2/settings.xml"
