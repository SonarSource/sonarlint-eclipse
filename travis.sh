#!/bin/bash

set -euo pipefail

function installTravisTools {
  curl -sSL https://raw.githubusercontent.com/sonarsource/travis-utils/master/install.sh | bash
}

function start_xvfb {
  export DISPLAY=:99.0
  sh -e /etc/init.d/xvfb start
}

function install_jars {
  echo "Install jars into local maven repository"

  mkdir -p ~/.m2/repository
  cp -r /tmp/travis-utils/m2repo/* ~/.m2/repository
}

installTravisTools
mvn verify -B -e -V -Dtycho.disableP2Mirrors=true -Dtarget.platform=$TARGET_PLATFORM

if [ "${TARGET_PLATFORM}" == "e44" ] || [ "${TARGET_PLATFORM}" == "e45" ]
then
  cd integrationTests
  start_xvfb
  install_jars
  mvn clean verify -Dsonar-eclipse.p2.url=file:///home/travis/build/SonarSource/sonar-eclipse/org.sonar.ide.eclipse.site/target/repository/ -Dsonar.runtimeVersion=DEV -DjavaVersion=LATEST_RELEASE -DcppVersion=LATEST_RELEASE -DpythonVersion=LATEST_RELEASE
fi  

