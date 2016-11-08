#!/bin/bash

set -euo pipefail

BUILD_ID=`date -u +%Y%m%d%H%M`
CURRENT_VERSION=`mvn help:evaluate -Dtycho.mode=maven -Dexpression="project.version" | grep -v '^\[\|Download\w\+\:'`
RELEASE_VERSION=`echo $CURRENT_VERSION | sed "s/-.*//g"`

NEW_VERSION="$RELEASE_VERSION.$BUILD_ID"

echo "CI_BUILD_NUMBER=$BUILD_ID" > build.properties

echo "Replacing version $CURRENT_VERSION with $NEW_VERSION"

mvn org.eclipse.tycho:tycho-versions-plugin:0.26.0:set-version -Dtycho.mode=maven -DnewVersion=$NEW_VERSION -B -e

PROJECT_VERSION=$NEW_VERSION

if [ "${GITHUB_BRANCH}" == "master" ] && [ "$IS_PULLREQUEST" == "false" ]; then
  echo '======= Build, deploy and analyze master'

  # Fetch all commit history so that SonarQube has exact blame information
  # for issue auto-assignment
  # This command can fail with "fatal: --unshallow on a complete repository does not make sense" 
  # if there are not enough commits in the Git repository (even if Travis executed git clone --depth 50).
  # For this reason errors are ignored with "|| true"
  git fetch --unshallow || true

  mvn org.jacoco:jacoco-maven-plugin:prepare-agent deploy sonar:sonar \
      -Pdeploy-sonarsource,coverage \
      -Dtycho.disableP2Mirrors=true \
      -Dmaven.test.redirectTestOutputToFile=false \
      -Dsonar.host.url=$SONAR_HOST_URL \
      -Dsonar.login=$SONAR_TOKEN \
      -Dsonar.projectVersion=$CURRENT_VERSION \
      -B -e -V $*

elif [ "$IS_PULLREQUEST" != "false" ] && [ -n "${GITHUB_TOKEN-}" ]; then
  echo '======= Build and analyze pull request'
  echo '======= with deploy'
    mvn org.jacoco:jacoco-maven-plugin:prepare-agent deploy sonar:sonar \
      -Pdeploy-sonarsource \
      -Dtycho.disableP2Mirrors=true \
      -Dmaven.test.redirectTestOutputToFile=false \
      -Dsonar.analysis.mode=issues \
      -Dsonar.github.pullRequest=$PULL_REQUEST \
      -Dsonar.github.repository=$GITHUB_REPO \
      -Dsonar.github.oauth=$GITHUB_TOKEN \
      -Dsonar.host.url=$SONAR_HOST_URL \
      -Dsonar.login=$SONAR_TOKEN \
      -B -e -V $*

else
  echo '======= Build, no analysis, no deploy'

  mvn verify \
      -Dmaven.test.redirectTestOutputToFile=false \
      -Dtycho.disableP2Mirrors=true \
      -B -e -V $*
fi
