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
package org.sonar.batch.components;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.batch.ProfileLoader;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.RuleQuery;

/**
 * Loads {@link RulesProfile} from remote Sonar server.
 */
public class RemoteProfileLoader implements ProfileLoader {

  private static final Logger LOG = LoggerFactory.getLogger(RemoteProfileLoader.class);

  public RulesProfile load(Project project) {
    RulesProfile profile = RulesProfile.create();
    try {
      // TODO hard-coded values
      Sonar sonar = Sonar.create("http://localhost:9000");
      RuleQuery ruleQuery = new RuleQuery("java").setActive(true).setProfile("Sonar for Sonar");
      List<org.sonar.wsclient.services.Rule> wsRules = sonar.findAll(ruleQuery);
      for (org.sonar.wsclient.services.Rule wsRule : wsRules) {
        Rule rule = materialize(wsRule);
        profile.activateRule(rule, null);
      }
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
    }
    return profile;
  }

  private Rule materialize(org.sonar.wsclient.services.Rule wsRule) {
    String repositoryKey = StringUtils.substringBefore(wsRule.getKey(), ":");
    String key = StringUtils.substringAfter(wsRule.getKey(), ":");
    RulePriority severity = RulePriority.valueOf(wsRule.getSeverity());
    Rule rule = Rule.create()
        .setRepositoryKey(repositoryKey)
        .setKey(key)
        .setConfigKey(wsRule.getConfigKey())
        .setSeverity(severity);
    if (wsRule.getParams() != null) {
      for (org.sonar.wsclient.services.RuleParam wsParam : wsRule.getParams()) {
        rule.createParameter(wsParam.getName()).setDefaultValue(wsParam.getValue());
      }
    }
    return rule;
  }

}
