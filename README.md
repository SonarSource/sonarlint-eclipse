Sonar Eclipse
=============

To build the plugin
-------------------

    mvn clean install


To develop the plugin with Eclipse
----------------------------------

You need to install m2eclipse and then Tycho extension to m2eclipse:

    Window -> Preferences -> Maven -> Discovery -> Open Catalog
    Install *Tycho Configurator*
    Then import everything as Maven project
    Activate target-platform-e42-dev.target as current target platform.

