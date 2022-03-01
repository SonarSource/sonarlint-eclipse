SonarLint for Eclipse
=====================

Installing and using
--------------------

See https://www.sonarlint.org and https://marketplace.eclipse.org/content/sonarlint  

For offline installation and older versions see the update site archive at https://binaries.sonarsource.com/SonarLint-for-Eclipse/releases/

Have Question or Feedback?
--------------------------

For SonarLint support questions ("How do I?", "I got this error, why?", ...), please first read the [FAQ](https://community.sonarsource.com/t/frequently-asked-questions/7204) and then head to the [SonarSource forum](https://community.sonarsource.com/c/help/sl). There are chances that a question similar to yours has already been answered. 

Be aware that this forum is a community, so the standard pleasantries ("Hi", "Thanks", ...) are expected. And if you don't get an answer to your thread, you should sit on your hands for at least three days before bumping it. Operators are not standing by. :-)


Contributing
------------

If you would like to see a new feature, please create a new thread in the forum ["Suggest new features"](https://community.sonarsource.com/c/suggestions/features).

Please be aware that we are not actively looking for feature contributions. The truth is that it's extremely difficult for someone outside SonarSource to comply with our roadmap and expectations. Therefore, we typically only accept minor cosmetic changes and typo fixes.

With that in mind, if you would like to submit a code contribution, please create a pull request for this repository. Please explain your motives to contribute this change: what problem you are trying to fix, what improvement you are trying to make.

Make sure that you follow our [code style](https://github.com/SonarSource/sonar-developer-toolset#code-style) and all tests are passing.

Development setup in Eclipse
----------------------------

We assume basic knowledge of Eclipse PDE and Tycho. 

You need to have a few Eclipse plugins:
* Eclipse RCP
* Eclipse Plug-in developement environment
* m2e
* RedDeer to run ITs

Normally, m2e will automatically suggest to install missing connectors (Tycho configurators, ...)

1. Run `mvn verify` on the command line to fetch artifacts referenced in the parent pom
2. In Eclipse, import the project root as Maven project
3. (Optional) In Eclipse, import the `its/` folder as Maven project
4. Open `target-platforms/dev.target` with the target platform editor
    - Click **Set as Target Platform** (or **Reload Target Platform**) in the top-right corner

At this point you should be all set, unless Eclipse is not able to generate protobuf sources.
Following the explanations [here](https://github.com/trustin/os-maven-plugin) may help.

In some (older?) flavors of Eclipse, you may need to install `m2eclipse` and then Tycho extension to `m2eclipse`:

1. Window -> Preferences -> Maven -> Discovery -> Open Catalog
2. Install **Tycho Configurator**

### Eclipse quirks

Strange issues in Eclipse and their remedies.

- Sometimes, for no apparent reason, the generated protobuf classes may disappear,
  and Eclipse may report compilation errors when resolving these class names, symbols.
  The workaround is to run `mvn compile` and refresh the views in Eclipse (click on the project and press `F5`).

Running
-------

Open `plugin.xml` of `org.sonarline.eclipse.core` (for example), and see the **Run** and **Debug** buttons in the top-right corner.

Running plugin unit tests
-------------------------

In Eclipse:

1. Create a run configuration by running a test class first with **Run As... / JUnit Plug-in Test**

2. Edit the configuration

    - On the **Test** tab, uncheck **Run in UI thread**
    - On the **Main** tab, under **Program to Run**, select **Run an application** with value **[No Application] - Headless Mode**

With Maven:

    mvn clean verify

Running ITs
-----------

To run ITs for the default target platform and SonarQube version you can use a helper script:

    ./scripts/run-its.sh

This assumes that the project was already `mvn` installed. You may want to run a specific test to avoid running everything:

    ./scripts/run-its.sh -Dtest=SimpleNameOfClass

Run with `-h` or `--help` to see other options.

The script uses Xephyr and assumes the `metacity` window manager is present.
The purpose of this is to open windows in an isolated X server to avoid interference with your desktop.

If you get some error when opening the JS Editor, read:
http://stackoverflow.com/questions/36317684/eclipse-jsdt-internal-error-noclassdeffounderror-jdk-nashorn-internal-runtime

Adding a dependency
-------------------

We should avoid adding external dependencies, as we want all bundles we provide in our update site to be signed, and we don't want to sign third-party components. Third-party libs should be bundled/shaded into
our own plugins.

### License

Copyright 2015-2022 SonarSource.

Licensed under the [GNU Lesser General Public License, Version 3.0](http://www.gnu.org/licenses/lgpl.txt)
