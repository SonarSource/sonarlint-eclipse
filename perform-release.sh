#!/bin/sh

if [ $# -ne 2 ]
then
  echo "Usage: `basename $0` {tagVersion} {nextVersion}"
  exit
fi

timestamp=`date -u +%Y%m%d%H%M`
tagVersion=$1
releaseVersion=$tagVersion-$timestamp
nextVersion=$2.qualifier

echo "tagVersion=$tagVersion"
echo "releaseVersion=$releaseVersion"
echo "nextVersion=$nextVersion"
echo -e "Are you sure (y/N)? \c "
read confirm
if [ "-"$confirm != "-y" ]; then
  exit
fi

mvn org.sonatype.tycho:tycho-versions-plugin:set-version -Dtycho.mode=maven -DnewVersion=$releaseVersion
mvn clean install

svn ci -m "prepare release eclipse-sonar-plugin-$tagVersion"
svn copy https://svn.codehaus.org/sonar-ide/trunk/eclipse/ https://svn.codehaus.org/sonar-ide/tags/eclipse-sonar-plugin-$tagVersion -m "copy for tag eclipse-sonar-plugin-$tagVersion"

mvn org.sonatype.tycho:tycho-versions-plugin:set-version -Dtycho.mode=maven -DnewVersion=$nextVersion

svn ci -m "prepare for next development iteration"
