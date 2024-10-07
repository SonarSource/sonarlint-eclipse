# SonarLint for Eclipse: Integration tests

This project including its modules contains the integration tests (including compatibility against
[iBuilds](https://download.eclipse.org/eclipse/downloads/), the in-development version of the Eclipse IDE) for
SonarLint that are relying on [Eclipse RedDeer](https://github.com/eclipse/reddeer) as the test framework connected
to [Eclipse Tycho](https://github.com/eclipse-tycho/tycho).

The tests are split into separate modules for testing in Standalone Mode only, in Connected Mode with SonarQube and
SonarCloud. Additionally, one module is used for shared logic. This Maven/Tycho project is completely decoupled from
the main SonarLint for Eclipse project.

## Building

In order to build the integration tests, the main SonarLint for Eclipse project must be build first to create the
Eclipse Update Site. On the CI/CD pipeline this is not done in one step, but locally it has to be done.

The target platform provided must be one of the supported in the `target-platforms` folder inside the main SonarLint
for Eclipse project directory.

```
cd $SONARLINT_FOR_ECLIPSE_FOLDER
mvn clean verify -DskipTests
cd its/
mvn clean verify -Dtarget.platform=latest-java-17_e431 -Dtycho.localArtifacts=ignore -Dsonarlint-eclipse.p2.url="file://{path to repo}/org.sonarlint.eclipse.site/target/repository" -DskipTests
```

When all the projects are opened inside Eclipse and the target platform correctly configured, this is done in the
background (not for the main SonarLint for Eclipse project tho if not also imported into Eclipse, which is required).

## Running the Tests

When imported inside Eclipse, the tests can be run simply via the RedDeer context menu options, but only tests of one
module should be run at most, or only one test class or one specific test. This is due to the different modules using
different "runtime definitions" (the `.product` files) for the Tycho/Surefire test runtime for starting an Eclipse IDE
instance.

### Standalone Mode / iBuilds compatibility

To run all the tests, or a specific one with the additionally command line option `-Dtest={TestClassName}`, for the
Standalone Mode, the following command has to be run that excludes the tests for the Connected Mode with SonarQube and
SonarCloud.

The target platform provided must be one of the supported in the `target-platforms` folder inside the main SonarLint
for Eclipse project directory. For Standalone Mode all target platforms are useful, for the iBuilds compatibility the
`ibuilds` target platform must be used.

```
mvn clean verify -Dtarget.platform=latest-java-17_e431 -Dtycho.localArtifacts=ignore -Dsonarlint-eclipse.p2.url="file://{path to repo}/org.sonarlint.eclipse.site/target/repository" -P \!connectedModeSc,\!connectedModeSq
```

### Connected Mode (SQ)

To run all the tests, or a specific one with the additionally command line option `-Dtest={TestClassName}`, for the
Connected Mode with SonarQube, the following command has to be run that excludes the tests for the Standalone Mode and
the Connected Mode with SonarCloud. With the additional command line option `-Dsonar.runtimeVersion={version alias}`
(see [Orchestrator documentation](https://github.com/SonarSource/orchestrator?tab=readme-ov-file#version-aliases)) the
specific SonarQube version can be configured, the default one is `LATEST_RELEASE`.

The target platform provided should be run against the latest one linked to the Eclipse IDE that is shipped with Java
17 (Eclipse IDE 2024-03 / 4.31). This is also what the CI/CD pipeline is doing in order to provide the correct runtime
for the SonarQube instance provided via [Orchestrator](https://github.com/SonarSource/orchestrator) for the latest
supported LTA version, the latest supported version and the currently in-development version (of SonarQube).

```
mvn clean verify -Dtarget.platform=latest-java-17_e431 -Dtycho.localArtifacts=ignore -Dsonarlint-eclipse.p2.url="file://{path to repo}/org.sonarlint.eclipse.site/target/repository" -P \!standaloneMode,\!connectedModeSc
```

### Connected Mode (SC)

To run all the tests, or a specific one with the additionally command line option `-Dtest={TestClassName}`, for the
Connected Mode with SonarCloud, the following command has to be run that excludes the tests for the Standalone Mode and
the Connected Mode with SonarQube.

The target platform provided must be one of the supported in the `target-platforms` folder inside the main SonarLint
for Eclipse project directory. Compared to the integration tests for the Connected Mode with SonarCloud there is no
dependency on the Java version and the CI/CD pipeline will run against the latest one linked to the Eclipse IDE that is
shipped with Java 21 (the current Java runtime used for upcoming releases as well).

Due to us requiring the password for the SonarCloud staging environment, we have to pass it directly to the command as
the Eclipse IDE runtime won't pick it up otherwise via `System.getenv("SONARCLOUD_IT_PASSWORD")`!

```
env SONARCLOUD_IT_PASSWORD=$SONARCLOUD_IT_PASSWORD mvn clean verify -Dtarget.platform=latest-java-21 -Dtycho.localArtifacts=ignore -Dsonarlint-eclipse.p2.url="file://{path to repo}/org.sonarlint.eclipse.site/target/repository" -P \!standaloneMode,\!connectedModeSq
```
