#!/bin/sh

if [ "$1" = "" ]; then
  TARGET=e35.target
else
  TARGET=$1
fi
echo Updating $TARGET

# Use the following command to update .target file to match contents of remote p2 repositories.
mvn org.sonatype.tycho.extras:tycho-version-bump-plugin:0.11.0-SNAPSHOT:update-target \
 -Dtycho.mode=maven -Dtycho.version=0.11.0-SNAPSHOT \
 -Dtarget=$TARGET
