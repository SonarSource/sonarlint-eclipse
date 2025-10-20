#!/usr/bin/env bash
set -euo pipefail

# Resolve Java homes from mise
JAVA_11_HOME=$( mise where java@11)
JAVA_17_HOME=$( mise where java@17)
JAVA_21_HOME=$( mise where java@21)

# Export toolchains envs
echo "JAVA_HOME_11_X64=$JAVA_11_HOME" >> "$GITHUB_ENV"
echo "JAVA_HOME_17_X64=$JAVA_17_HOME" >> "$GITHUB_ENV"
echo "JAVA_HOME_21_X64=$JAVA_21_HOME" >> "$GITHUB_ENV"

# Make Maven/Tycho runtime use Java 17
echo "JAVA_HOME=$JAVA_17_HOME" >> "$GITHUB_ENV"
echo "$JAVA_17_HOME/bin" >> "$GITHUB_PATH"

# Configure Maven toolchains
mkdir -p "$HOME/.m2"
cp .github/scripts/toolchains.xml "$HOME/.m2/toolchains.xml"

java -version
mvn -version