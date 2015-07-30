#!/bin/bash

set -euo pipefail

function installTravisTools {
  mkdir ~/.local
  curl -sSL https://github.com/SonarSource/travis-utils/tarball/v15 | tar zx --strip-components 1 -C ~/.local
  source ~/.local/bin/install
}

build_snapshot "SonarSource/sonar-runner"

mvn verify -B -e -V -Dtycho.disableP2Mirrors=true -Dtarget.platform=$TARGET_PLATFORM

if [ "${RUN_ITS}" == "true" ]
then
  installTravisTools

  build_snapshot "SonarSource/sonarqube"

  start_xvfb
  metacity --sm-disable --replace &

  cd integrationTests
  mvn verify -Dsonar-eclipse.p2.url=file:///home/travis/build/SonarSource/sonar-eclipse/org.sonar.ide.eclipse.site/target/repository/ -Dsonar.runtimeVersion=DEV -DjavaVersion=LATEST_RELEASE -DpythonVersion=LATEST_RELEASE
fi
