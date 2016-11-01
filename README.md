SonarLint Eclipse Plugin
=============
[![Build Status](https://travis-ci.org/SonarSource/sonarlint-eclipse.svg?branch=master)](https://travis-ci.org/SonarSource/sonarlint-eclipse)

Building
--------

    mvn clean install

Development setup in Eclipse
----------------------------

Install `m2eclipse` and then Tycho extension to `m2eclipse`:

1. Window -> Preferences -> Maven -> Discovery -> Open Catalog
2. Install **Tycho Configurator**
3. Import everything as Maven project
4. Activate `target-platform-e46-dev.target` as current target platform

Running
-------

Open `plugin.xml` of `org.sonarline.eclipse.core` (for example), and see the **Run** and **Debug** buttons in the top-right corner.

Running tests
-------------

In Eclipse:

1. Create a run configuration by running a test class first with **Run As... / JUnit Plug-in Test**

2. Edit the configuration

    - On the **Test** tab, uncheck **Run in UI thread**
    - On the **Main** tab, under **Program to Run**, select **Run an application** with value **[No Application] - Headless Mode**

With Maven:

    mvn clean verify

Adding a dependency
-------------------

Must be osgi bundle.

### For Maven

Add the artifact to the parent pom.

Run `mvn compile` to get the artifact downloaded so that you can inspect its manifest in the jar.

Find the name of the bundle from its manifest, see the `Bundle-SymbolicName` property.

Edit the manifest of the package where you want to add the dependency (for example: `org.sonarlint.eclipse.core/META-INF/MANIFEST.MF`), add the bundle in the `Require-Bundle` property, using its symbolic name.

If the bundle is not needed at runtime, don't forget to mark it optional, to avoid including in the package.
(Edit properties on the **Dependencies** tab, or append `;resolution:=optional`)

To verify the content of the package: `mvn clean package` and check content of the ZIP in plugins folder.

### For Eclipse

Add to `target-platform-e45/target-platform-e45-dev.target` (or whatever target you use) the path of the artifact in your local maven repository as a filesystem path, similar to already existing dependencies.

In the target editor (or open `plugin.xml`), click **Set as Target Platform**.
Note that this will trigger a compilation in Eclipse.

At this point, and if the artifact exists at the specified path, it should be usable, and Eclipse will be able to compile the project.
