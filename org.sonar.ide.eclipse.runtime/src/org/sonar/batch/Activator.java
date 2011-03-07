/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch;

import java.io.File;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

public class Activator extends Plugin {

  private IPluginsManager pluginsManager;

  private static Activator activator;

  public Activator() {
    this.activator = this;
  }

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);

    File stateDir = getStateLocation().toFile();
    File workDir = new File(stateDir, "sonar-plugins");
    pluginsManager = PluginsManagerFactory.newPluginsManager(workDir);

    pluginsManager.install(getCorePluginFromMavenRepo("sonar-pmd-plugin"));
    pluginsManager.install(getCorePluginFromMavenRepo("sonar-squid-java-plugin"));
    pluginsManager.install(getCorePluginFromMavenRepo("sonar-findbugs-plugin"));
    pluginsManager.install(getCorePluginFromMavenRepo("sonar-checkstyle-plugin"));
    pluginsManager.install(getCorePluginFromMavenRepo("sonar-design-plugin"));
    pluginsManager.install(getCorePluginFromMavenRepo("sonar-cpd-plugin"));

    pluginsManager.start();
  }

  private static File getCorePluginFromMavenRepo(String artifactId) {
    return getPluginFromMavenRepo("org.codehaus.sonar.plugins", artifactId, "2.7-SNAPSHOT");
  }

  private static File getPluginFromMavenRepo(String groupId, String artifactId, String version) {
    String userHomePath = System.getProperty("user.home");
    String repositoryPath = userHomePath + "/.m2/repository/";
    String artifactPath = groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".jar";
    return new File(repositoryPath + artifactPath);
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    pluginsManager.stop();

    super.stop(context);
  }

  public static Activator getDefault() {
    return activator;
  }

  public org.sonar.api.Plugin[] getPlugins() {
    return pluginsManager.getPlugins();
  }
}
