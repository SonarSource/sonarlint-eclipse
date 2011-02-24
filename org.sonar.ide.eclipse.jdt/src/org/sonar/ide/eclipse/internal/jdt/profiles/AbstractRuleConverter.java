/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010 SonarSource
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

package org.sonar.ide.eclipse.internal.jdt.profiles;

import org.apache.commons.lang.StringUtils;
import org.sonar.ide.eclipse.jdt.profiles.ISonarRuleConverter;
import org.sonar.wsclient.services.Rule;
import org.sonar.wsclient.services.RuleParam;

/**
 * @author Jérémie Lagarde
 * @since 1.1.0
 */
public abstract class AbstractRuleConverter implements ISonarRuleConverter {

  private String plugin;
  private String key;

  public AbstractRuleConverter(String plugin, String key) {
    this.plugin = plugin;
    this.key = key;
  }

  public final boolean canConvert(Rule rule) {
    return (rule != null && plugin.equalsIgnoreCase(rule.getRepository()) && key.equalsIgnoreCase(rule.getKey()));
  }
  
  protected String getParam(Rule rule, String name) {
    return getParam(rule, name, null);
  }

  protected String getParam(Rule rule, String name, String defaultValue) {
    for (RuleParam param : rule.getParams()) {
      if (param.getName().equals(name))
        return (StringUtils.isBlank(param.getValue())) ? defaultValue : param.getValue();
    }
    return null;
  }
  protected String[] getParams(Rule rule, String name) {
    return getParams(rule, name, null);
  }

  protected String[] getParams(Rule rule, String name, String[] defaultValues) {
    for (RuleParam param : rule.getParams()) {
      if (param.getName().equals(name))
        return (StringUtils.isBlank(param.getValue())) ? defaultValues : StringUtils.split(param.getValue());
    }
    return null;
  }

}
