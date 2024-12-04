SonarQube for IDE: Eclipse
==========================

SonarQube for IDE: Eclipse helps you deliver [Clean Code](https://www.sonarsource.com/solutions/clean-code/?utm_medium=referral&utm_source=github&utm_campaign=clean-code&utm_content=sonarlint-eclipse) in your IDE.

Installing and using
--------------------

See https://docs.sonarsource.com/sonarqube-for-ide/eclipse/getting-started/installation/ and https://marketplace.eclipse.org/content/sonarlint

For offline installation and older versions see the update site archive at https://binaries.sonarsource.com/?prefix=SonarLint-for-Eclipse/releases/

Have Questions or Feedback?
--------------------------

For SonarQube for IDE support questions ("How do I?", "I got this error, why?", ...), please first read the [FAQ](https://community.sonarsource.com/t/frequently-asked-questions/7204) and then head to the [SonarSource forum](https://community.sonarsource.com/c/help/sl). There are chances that a question similar to yours has already been answered. 

Be aware that this forum is a community, so the standard pleasantries ("Hi", "Thanks", ...) are expected. And if you don't get an answer to your thread, you should sit on your hands for at least three days before bumping it. Operators are not standing by. :-)


Contributing
------------

If you would like to see a new feature, please create a new thread in the forum ["Suggest new features"](https://community.sonarsource.com/c/suggestions/features).

Please be aware that we are not actively looking for feature contributions. The truth is that it's extremely difficult for someone outside SonarSource to comply with our roadmap and expectations. Therefore, we typically only accept minor cosmetic changes and typo fixes.

With that in mind, if you would like to submit a code contribution, please create a pull request for this repository. Please explain your motives to contribute this change: what problem you are trying to fix, what improvement you are trying to make.

Make sure that you follow our [code style](https://github.com/SonarSource/sonar-developer-toolset#code-style) and all tests are passing.

Development setup in Eclipse
----------------------------

There are a few requirements for developing next to basic knowledge of Maven, Tycho and Eclipse plug-in development:
- Eclipse IDE for RCP and RAP Developers (includes m2e, PDE)
- RedDeer to run ITs

Normally, m2e will automatically suggest to install missing connectors (Tycho configurators, ...) or wants to configure
missing lifecycle mappings. This can all be done later.

1. Run `mvn clean verify -DskipTests` on the command line to fetch artifacts referenced in the parent pom
 - for forks use `-Dskip-sonarsource-repo` as the reference to the CFamily analyzer is not available on Maven Central
2. In Eclipse, import the project root as a Maven project
3. In Eclipse, import the project root of the ITs as a Maven project and add them to the main project
4. Open `target-platforms/dev.target` with the target platform editor
    - Click **Set as Target Platform** (or **Reload Target Platform**) in the top-right corner

At this point you should be all set.
Following the explanations [here](https://github.com/trustin/os-maven-plugin) may help.

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

 - for forks use `-Dskip-sonarsource-repo` as the reference to the CFamily analyzer is not available on Maven Central

Running ITs
-----------

Please see the **README.md** inside the *its* folder!

Adding a dependency
-------------------

We should avoid adding external dependencies, as we want all bundles we provide in our update site to be signed, and we don't want to sign third-party components. Third-party libs should be bundled/shaded into
our own plugins.

### License

Copyright 2015-2024 SonarSource.

Licensed under the [GNU Lesser General Public License, Version 3.0](http://www.gnu.org/licenses/lgpl.txt)
