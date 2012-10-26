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
package org.sonar.batch;

import org.sonar.api.BatchExtension;
import org.sonar.api.Extension;
import org.sonar.api.Plugin;
import org.sonar.api.batch.SupportedEnvironment;
import org.sonar.api.rules.RuleRepository;
import org.sonar.api.utils.AnnotationUtils;

public class PluginModule extends Module {

  private static final String[] BLACK_LIST = {
    "org.sonar.plugins.core.security.",
    "org.sonar.plugins.core.timemachine.",
    "org.sonar.plugins.core.sensors.AsynchronousMeasuresSensor",
    "org.sonar.plugins.core.sensors.GenerateAlertEvents",
    "org.sonar.plugins.core.sensors.CloseReviewsDecorator"
  };

  private final Plugin plugin;

  public PluginModule(Plugin plugin) {
    this.plugin = plugin;
  }

  @Override
  protected void configure() {
    for (Object extension : plugin.getExtensions()) {
      if (shouldRegisterExtension(extension)) {
        addComponent(getExtensionKey(extension), extension);
      }
    }
  }

  private boolean shouldRegisterExtension(Object extension) {
    return (isType(extension, BatchExtension.class) && isSupportsEnvironment(extension) && isNotBlackListed(extension))
      || isType(extension, RuleRepository.class);
  }

  private boolean isNotBlackListed(Object extension) {
    Class clazz = (extension instanceof Class ? (Class) extension : extension.getClass());
    String extensionClassName = clazz.getCanonicalName();
    for (String black : BLACK_LIST) {
      if (extensionClassName.startsWith(black)) {
        return false;
      }
    }
    return true;
  }

  private boolean isSupportsEnvironment(Object extension) {
    Class clazz = (extension instanceof Class ? (Class) extension : extension.getClass());
    SupportedEnvironment env = AnnotationUtils.getClassAnnotation(clazz, SupportedEnvironment.class);
    if (env == null) {
      return true;
    }
    return false;
  }

  /**
   * @TODO copied from {@link org.sonar.batch.bootstrap.BatchPluginRepository}
   */
  protected static boolean isType(Object extension, Class<? extends Extension> extensionClass) {
    Class clazz = (extension instanceof Class ? (Class) extension : extension.getClass());
    return extensionClass.isAssignableFrom(clazz);
  }

  /**
   * @TODO copied from {@link org.sonar.core.plugin.AbstractPluginRepository}
   */
  protected static Object getExtensionKey(Object component) {
    if (component instanceof Class) {
      return component;
    }
    return component.getClass().getCanonicalName() + "-" + component.toString();
  }

}
