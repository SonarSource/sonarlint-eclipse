#!/bin/bash
# Tycho implementation instead of https://github.com/SonarSource/ci-github-actions/blob/master/config-maven/set_maven_project_version.sh
#
# Required environment variables (must be explicitly provided):
# - BUILD_NUMBER: Build number for versioning
#
# GitHub Actions auto-provided:
# - GITHUB_OUTPUT: Path to GitHub Actions output file
# - GITHUB_ENV: Path to GitHub Actions environment file

set -euo pipefail

: "${BUILD_NUMBER:?}"
: "${GITHUB_OUTPUT:?}" "${GITHUB_ENV:?}"

get_current_version() {
  if ! mvn -q -N -Dtycho.mode=maven help:evaluate -Dexpression="project.version" -DforceStdout; then
    echo "Failed to read project.version" >&2
    mvn -X -N -Dtycho.mode=maven help:evaluate -Dexpression="project.version" -DforceStdout
    return 1
  fi
}

set_project_version() {
  local current_version
  if ! current_version=$(get_current_version 2>&1); then
    echo -e "::error file=pom.xml,title=Maven project version::Could not get 'project.version' from Maven project\nERROR: $current_version"
    return 1
  fi

  # Saving the snapshot version to the output and environment variables
  # This is used by the sonar-scanner to set the value of sonar.projectVersion without the build number
  echo "current-version=$current_version" >> "$GITHUB_OUTPUT"
  echo "CURRENT_VERSION=$current_version" >> "$GITHUB_ENV"
  echo "CURRENT_VERSION=${current_version} (from pom.xml)"
  export CURRENT_VERSION="$current_version"

  local release_version="${current_version%"-SNAPSHOT"}"
  local dots="${release_version//[^.]/}"
  local dots_count="${#dots}"

  if [[ "$dots_count" -eq 0 ]]; then
    release_version="${release_version}.0.0"
  elif [[ "$dots_count" -eq 1 ]]; then
    release_version="${release_version}.0"
  elif [[ "$dots_count" -ne 2 ]]; then
    echo "::error file=pom.xml,title=Maven project version::Unsupported version '$current_version' with $((dots_count + 1)) digits."
    return 1
  fi
  release_version="${release_version}.${BUILD_NUMBER}"
  echo "Replacing version ${current_version} with ${release_version}"
  mvn -V org.eclipse.tycho:tycho-versions-plugin:3.0.5:set-version -Dtycho.mode=maven -DnewVersion=$release_version -DgenerateBackupPoms=false --batch-mode --no-transfer-progress --errors
  echo "project-version=$release_version" >> "$GITHUB_OUTPUT"
  echo "PROJECT_VERSION=$release_version" >> "$GITHUB_ENV"
  echo "PROJECT_VERSION=${release_version}"
  export PROJECT_VERSION="$release_version"
}

if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
  set_project_version
fi
