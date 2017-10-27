#!/bin/bash

set -euo pipefail

CURRENT_VERSION=`mvn help:evaluate -Dtycho.mode=maven -Dexpression="project.version" | grep -v '^\[\|Download\w\+\:'`
RELEASE_VERSION=`echo $CURRENT_VERSION | sed "s/-.*//g"`

NEW_VERSION="$RELEASE_VERSION.$BUILD_ID"
echo "NEW_VERSION=$NEW_VERSION" >> build.properties


echo "Replacing version $CURRENT_VERSION with $NEW_VERSION"

mvn org.eclipse.tycho:tycho-versions-plugin:0.26.0:set-version -Dtycho.mode=maven -DnewVersion=$NEW_VERSION -B -e

export PROJECT_VERSION=$NEW_VERSION

if [ "${GITHUB_BRANCH}" == "master" ] && [ "$IS_PULLREQUEST" == "false" ]; then
  echo '======= Build, deploy and analyze master'

  # Fetch all commit history so that SonarQube has exact blame information
  # for issue auto-assignment
  # This command can fail with "fatal: --unshallow on a complete repository does not make sense" 
  # if there are not enough commits in the Git repository (even if Travis executed git clone --depth 50).
  # For this reason errors are ignored with "|| true"
  git fetch --unshallow || true
  
  mvn org.jacoco:jacoco-maven-plugin:prepare-agent deploy \
      -Pdeploy-sonarsource,coverage,sign \
      -Dsonarsource.keystore.path=$SONARSOURCE_KEYSTORE_PATH \
      -Dsonarsource.keystore.password=$SONARSOURCE_KEYSTORE_PASS \
      -Dtycho.disableP2Mirrors=true \
      -Dmaven.test.redirectTestOutputToFile=false \
      -B -e -V $*

  REPO_URL="file://`pwd`/org.sonarlint.eclipse.site/target/repository/"   
      

  # Run ITs to collect IT coverage
  cd its
  mvn org.jacoco:jacoco-maven-plugin:prepare-agent verify \
      -Pcoverage \
      -Dtycho.localArtifacts=ignore \
      -Dtycho.disableP2Mirrors=true \
      -Dsonarlint-eclipse.p2.url=$REPO_URL \
      -B -e
      
  cd ..
  mvn sonar:sonar \
      -Pcoverage \
      -Dtycho.disableP2Mirrors=true \
      -Dsonar.host.url=$SONAR_HOST_URL \
      -Dsonar.login=$SONAR_TOKEN \
      -Dsonar.projectVersion=$CURRENT_VERSION \
      -Dsonar.analysis.buildNumber=$BUILD_ID \
      -Dsonar.analysis.pipeline=$BUILD_ID \
      -Dsonar.analysis.sha1=$GIT_SHA1  \
      -Dsonar.analysis.repository=$GITHUB_REPO \
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

    mvn sonar:sonar \
      -Dtycho.disableP2Mirrors=true \
      -Dsonar.host.url=$SONAR_HOST_URL \
      -Dsonar.login=$SONAR_TOKEN \
      -Dsonar.analysis.buildNumber=$BUILD_ID \
      -Dsonar.analysis.pipeline=$BUILD_ID \
      -Dsonar.analysis.sha1=$GIT_SHA1  \
      -Dsonar.analysis.repository=$GITHUB_REPO \
      -Dsonar.analysis.prNumber=$PULL_REQUEST \
      -Dsonar.branch.name=$GITHUB_BASE_BRANCH \
      -Dsonar.branch.target=$GITHUB_TARGET_BRANCH

elif [[ "${TRAVIS_BRANCH}" == "branch-"* ]] && [ "$IS_PULLREQUEST" == "false" ]; then
    echo '======= Build, no analysis'
    echo '======= with deploy'
    mvn deploy \
      -Pdeploy-sonarsource,sign \
      -Dsonarsource.keystore.path=$SONARSOURCE_KEYSTORE_PATH \
      -Dsonarsource.keystore.password=$SONARSOURCE_KEYSTORE_PASS \
      -Dtycho.disableP2Mirrors=true \
      -Dmaven.test.redirectTestOutputToFile=false \
      -B -e -V $*

else
  echo '======= Build, no analysis, no deploy'

  mvn verify \
      -Dmaven.test.redirectTestOutputToFile=false \
      -Dtycho.disableP2Mirrors=true \
      -B -e -V $*
fi
