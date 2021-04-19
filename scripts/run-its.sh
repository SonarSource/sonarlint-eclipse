#!/usr/bin/env bash

set -euo pipefail

print_help() {
    cat << EOF
usage: $0 [-h|--help] [-p TARGET_PLATFORM] [-s SQ_VERSION] [JAVA_ARGS]...

Example JAVA_ARGS of interest:

    -DdebugPort=8000
    -Dtest=RuleExclusionsTest

EOF
}

expect_param() {
    if [[ $# < 2 ]]; then
        echo "fatal: the option $1 expects a parameter"
        exit 1
    fi
}

target_platform=2019-06
sq_version=LATEST_RELEASE

args=()
while [ $# != 0 ]; do
    case "$1" in
        -h|--help) print_help; exit ;;
        -p) expect_param "$@"; target_platform=$2; shift 2 ;;
        -s) expect_param "$@"; sq_version=$2; shift 2 ;;
        *) args+=($1); shift ;;
    esac
done

set -- "${args[@]}"

REPO_URL="file://$PWD/org.sonarlint.eclipse.site/target/repository/"

set -x
mvn -f its/pom.xml -B -e -V clean verify -Dtarget.platform=$target_platform -Dtycho.localArtifacts=ignore -Dtycho.disableP2Mirrors=true -Dsonarlint-eclipse.p2.url=$REPO_URL -Dsonar.runtimeVersion=$sq_version "$@"
