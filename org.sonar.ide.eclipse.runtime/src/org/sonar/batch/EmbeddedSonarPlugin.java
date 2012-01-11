/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010-2012 SonarSource
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

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.util.IOUtil;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class EmbeddedSonarPlugin extends Plugin {

  public static final String PLUGIN_ID = "org.sonar.ide.eclipse.runtime";

  private IPluginsManager pluginsManager;

  private static EmbeddedSonarPlugin plugin;

  public EmbeddedSonarPlugin() {
    plugin = this; // NOSONAR
  }

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);

    File stateDir = getStateLocation().toFile();
    File workDir = new File(stateDir, "sonar-plugins");
    pluginsManager = PluginsManagerFactory.newPluginsManager(workDir);

    Bundle bundle = context.getBundle();
    Enumeration<String> e = bundle.getEntryPaths("/plugins/");
    while (e.hasMoreElements()) {
      String entry = e.nextElement();
      URL url = bundle.getEntry(entry);
      InputStream input = url.openStream();
      File file = new File(workDir, entry);
      OutputStream output = FileUtils.openOutputStream(file);
      IOUtil.copy(input, output);
      IOUtil.close(input);
      pluginsManager.install(file);
    }

    pluginsManager.start();
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    pluginsManager.stop();

    super.stop(context);
  }

  public static EmbeddedSonarPlugin getDefault() {
    return plugin;
  }

  private final List<SonarLogListener> listeners = new ArrayList<SonarLogListener>();

  public void addSonarLogListener(SonarLogListener listener) {
    listeners.add(listener);
  }

  public void removeSonarLogListener(SonarLogListener listener) {
    listeners.remove(listener);
  }

  public void log(LogEntry logEntry) {
    for (SonarLogListener listener : listeners) {
      listener.logged(logEntry);
    }
  }

  public org.sonar.api.Plugin[] getPlugins() {
    return pluginsManager.getPlugins();
  }

  private CustomProjectComponentsModule customizer = new CustomProjectComponentsModule();

  public CustomProjectComponentsModule getSonarCustomizer() {
    return customizer;
  }
}
