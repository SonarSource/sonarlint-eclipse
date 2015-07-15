#!/bin/bash

set -euo pipefail

function installTravisTools {
  curl -sSL https://raw.githubusercontent.com/sonarsource/travis-utils/v10/install.sh | bash
  source /tmp/travis-utils/env.sh
}

mvn verify -B -e -V -Dtycho.disableP2Mirrors=true -Dtarget.platform=$TARGET_PLATFORM

if [ "${TARGET_PLATFORM}" == "e44" ] || [ "${TARGET_PLATFORM}" == "e45" ]
then
  installTravisTools
  travis_start_xvfb

  cd integrationTests
  mvn verify -Dsonar-eclipse.p2.url=file:///home/travis/build/SonarSource/sonar-eclipse/org.sonar.ide.eclipse.site/target/repository/ -Dsonar.runtimeVersion=DEV -DjavaVersion=LATEST_RELEASE -DcppVersion=LATEST_RELEASE -DpythonVersion=LATEST_RELEASE
fi
