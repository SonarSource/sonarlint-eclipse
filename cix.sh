#!/bin/bash

set -euo pipefail

mvn clean verify -Dtarget.platform=$TARGET_PLATFORM
