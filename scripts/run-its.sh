#!/usr/bin/env bash

set -euo pipefail

testDISPLAY=:51

init_display() {
    Xephyr -screen 1024x768 $testDISPLAY &
    export DISPLAY=$testDISPLAY
    metacity --sm-disable --replace &
}

print_help() {
    cat << EOF
usage: $0 [-h|--help] [--init] [-p TARGET_PLATFORM] [-s SQ_VERSION] [JAVA_ARGS]...

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

target_platform=e48
sq_version=LATEST_RELEASE

args=()
while [ $# != 0 ]; do
    case "$1" in
        --init) init_display; exit ;;
        -h|--help) print_help; exit ;;
        -p) expect_param "$@"; target_platform=$2; shift 2 ;;
        -s) expect_param "$@"; sq_version=$2; shift 2 ;;
        *) args+=($1); shift ;;
    esac
done

set -- "${args[@]}"

REPO_URL="file://$PWD/org.sonarlint.eclipse.site/target/repository/"
export DISPLAY=$testDISPLAY

set -x
mvn -f its/pom.xml -B -e -V clean verify -Dtarget.platform=$target_platform -Dtycho.localArtifacts=ignore -Dtycho.disableP2Mirrors=true -Dsonarlint-eclipse.p2.url=$REPO_URL -Dsonar.runtimeVersion=$sq_version "$@"
