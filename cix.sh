#!/bin/bash

set -euo pipefail

mvn clean verify -Dtarget.platform=$TARGET_PLATFORM -Dtycho.disableP2Mirrors=true
