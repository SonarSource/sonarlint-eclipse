target-platform-e42.target is the one used during Maven/Tycho build. Thanks to Tycho some Maven dependencies are dynamically added to this target platform
(all dependencies declared in root pom.xml like commons-io, commons_lang, ...). For this to work, the external dependencies should have a valid osgi MANIFEST.

The problem is that in Eclipse there is no equivalent of this "magic" enhancement of the target platform. As a workaround I am maintaining
a different target platform file (target-platform-e42-dev.target) that should be the same as target-platform-e42.target plus reference to external JARs using the
syntax:
<location path="${M2_REPO}/com/googlecode/json-simple/json-simple/1.1.1" type="Directory"/>

Of course M2-REPO is a variable that should be declared in Eclipse.

It means that currently there are at least 2 places to update when someone wants to add/update/remove a dependency...
