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
package org.sonar.batch.internal;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.Plugin;
import org.sonar.api.utils.ZipUtils;
import org.sonar.batch.IPluginsManager;
import org.sonar.core.classloaders.ClassLoadersCollection;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class PluginsManager implements IPluginsManager {

  private static final Logger LOG = LoggerFactory.getLogger(PluginsManager.class);

  private File workDir;

  private ClassLoader parentClassLoader;

  private Map<File, Manifest> manifests = new HashMap<File, Manifest>();
  private Map<String, String> plugins = new HashMap<String, String>();

  public PluginsManager(ClassLoader cl, File workDir) {
    this.parentClassLoader = cl;
    this.workDir = workDir;
  }

  public void install(File plugin) throws Exception {
    unzip(plugin, plugin.getName());
  }

  private ClassLoadersCollection collection;

  public void start() throws Exception {
    LOG.info("Starting Sonar Plugins Manager (workDir: {})", workDir);

    collection = new ClassLoadersCollection(parentClassLoader);

    for (Map.Entry<File, Manifest> entry : manifests.entrySet()) {
      File file = entry.getKey();
      Manifest manifest = entry.getValue();

      Attributes attributes = manifest.getMainAttributes();
      String childFirst = attributes.getValue("Plugin-ChildFirstClassLoader");
      String pluginKey = attributes.getValue("Plugin-Key");
      String pluginClass = attributes.getValue("Plugin-Class");
      String pluginDependencies = StringUtils.defaultString(attributes.getValue("Plugin-Dependencies"));

      File pluginDir = file.getParentFile().getParentFile();

      Collection<URL> urls = new ArrayList<URL>();
      urls.add(pluginDir.toURL());
      String[] deps = StringUtils.split(pluginDependencies);
      for (String dep : deps) {
        File depFile = new File(pluginDir, dep);
        urls.add(depFile.toURL());
      }
      LOG.debug("ClassPath for plugin {} : {}", pluginKey, urls);

      collection.createClassLoader(pluginKey, urls, "true".equals(childFirst));

      plugins.put(pluginKey, pluginClass);
    }

    collection.done();
  }

  public void stop() throws Exception {
    LOG.info("Stopping Sonar Plugins Manager");
  }

  private void unzip(File file, String name) throws Exception {
    File toDir = new File(workDir, name);
    ZipUtils.unzip(file, toDir);
    File manifestFile = new File(toDir, "META-INF/MANIFEST.MF");
    InputStream manifestStream = FileUtils.openInputStream(manifestFile);
    manifests.put(manifestFile, new Manifest(manifestStream));
    IOUtils.closeQuietly(manifestStream);
  }

  public ClassLoader getClassLoader(String pluginKey) throws Exception {
    return collection.get(pluginKey);
  }

  public Plugin[] getPlugins() {
    ArrayList<Plugin> result = new ArrayList<Plugin>();
    for (Map.Entry<String, String> entry : plugins.entrySet()) {
      Plugin plugin;
      try {
        plugin = (Plugin) getClassLoader(entry.getKey()).loadClass(entry.getValue()).newInstance();
        result.add(plugin);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return result.toArray(new Plugin[result.size()]);
  }

}
