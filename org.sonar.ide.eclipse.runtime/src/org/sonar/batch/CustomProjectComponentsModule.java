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

import java.util.Map;

import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomProjectComponentsModule extends Module {
  private static final Logger LOG = LoggerFactory.getLogger(CustomProjectComponentsModule.class);

  private Map<Class, Class> replacements = Maps.newHashMap();

  public void configure() {
    for (Map.Entry<Class, Class> entry : replacements.entrySet()) {
      Class<?> role = entry.getKey();
      Class<?> impl = entry.getValue();
      getContainer().removeComponent(role);
      getContainer().addComponent(role, impl);
    }
  }

  public <T> void replace(Class<T> role, Class<? extends T> impl) {
    LOG.info("Replacing {} by {}", role, impl);
    replacements.put(role, impl);
  }

  public void restore(Class role) {
    Class impl = replacements.remove(role);
    LOG.info("Removed replacement {} by {}", role, impl);
  }

}
