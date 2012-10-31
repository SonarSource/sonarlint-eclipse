/*
 * Sonar Eclipse
 * Copyright (C) 2010-2012 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.ide.eclipse.runner;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

import java.util.ArrayList;
import java.util.List;

public class SonarRunnerPlugin extends Plugin {

  public static final String PLUGIN_ID = "org.sonar.ide.eclipse.runtime";

  private static SonarRunnerPlugin plugin;

  public SonarRunnerPlugin() {
    plugin = this; // NOSONAR
  }

  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
  }

  @Override
  public void stop(BundleContext context) throws Exception {
    super.stop(context);
  }

  public static SonarRunnerPlugin getDefault() {
    return plugin;
  }

  private final List<SonarRunnerLogListener> listeners = new ArrayList<SonarRunnerLogListener>();

  public void addSonarLogListener(SonarRunnerLogListener listener) {
    listeners.add(listener);
  }

  public void removeSonarLogListener(SonarRunnerLogListener listener) {
    listeners.remove(listener);
  }

  public void error(String msg) {
    for (SonarRunnerLogListener listener : listeners) {
      listener.error(msg);
    }
  }

  public void info(String msg) {
    for (SonarRunnerLogListener listener : listeners) {
      listener.info(msg);
    }
  }

}
