#!/usr/bin/env bash
set -euo pipefail

echo "Checking SLCORE version consistency..."

# Extract SLCORE version from pom.xml (similar to get_current_version in set_maven_build_version.sh)
SLCORE_VERSION="$(mvn -q -Dtycho.mode=maven help:evaluate -Dexpression=sloop.version -DforceStdout 2>/dev/null)"

if [[ -z "${SLCORE_VERSION}" ]]; then
  echo "❌ Property 'sloop.version' must be set to be re-used by the platform-specific bundles!"
  # Show detailed error output
  mvn -X -Dtycho.mode=maven help:evaluate -Dexpression=sloop.version -DforceStdout
  exit 1
fi

echo "✅ SLCORE version found in pom.xml: ${SLCORE_VERSION}"

# Check if version exists in target platform file
if ! grep -q "${SLCORE_VERSION}" target-platforms/commons-build.target; then
  echo "❌ SLCORE version for Maven and Eclipse target platform (PDE) doesn't match!"
  echo "Expected version '${SLCORE_VERSION}' not found in target-platforms/commons-build.target"
  exit 2
fi

echo "✅ SLCORE version consistency check passed"