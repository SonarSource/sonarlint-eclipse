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

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.batch.ProfileLoader;
import org.sonar.wsclient.Host;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.WSClientFactory;
import org.sonar.wsclient.services.RuleQuery;

import java.util.List;

/**
 * Loads {@link RulesProfile} from remote Sonar server.
 */
public class RemoteProfileLoader implements ProfileLoader {

  private static final Logger LOG = LoggerFactory.getLogger(RemoteProfileLoader.class);

  private final RuleFinder ruleFinder;

  public RemoteProfileLoader(RuleFinder ruleFinder) {
    this.ruleFinder = ruleFinder;
  }

  public RulesProfile load(Project project) {
    RulesProfile profile = RulesProfile.create("Sonar for Sonar", "java");
    try {
      // TODO hard-coded values
      // Sonar sonar = WSClientFactory.create(new Host("http://localhost:9000"));
      Sonar sonar = WSClientFactory.create(new Host("http://nemo.sonarsource.org"));
      RuleQuery ruleQuery = new RuleQuery(profile.getLanguage()).setProfile(profile.getName()).setActive(true);
      List<org.sonar.wsclient.services.Rule> wsRules = sonar.findAll(ruleQuery);
      for (org.sonar.wsclient.services.Rule wsRule : wsRules) {
        activate(profile, wsRule);
      }
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
    }
    return profile;
  }

  public void activate(RulesProfile profile, org.sonar.wsclient.services.Rule wsRule) {
    String repositoryKey = StringUtils.substringBefore(wsRule.getKey(), ":");
    String key = StringUtils.substringAfter(wsRule.getKey(), ":");
    Rule rule = ruleFinder.findByKey(repositoryKey, key);
    // Don't activate rules, which we can't find locally
    // SONAR-2205 seems useless and we can work with Sonar 2.6
    if (rule != null) {
      RulePriority severity = RulePriority.valueOf(wsRule.getSeverity());
      ActiveRule activeRule = profile.activateRule(rule, severity);
      if (wsRule.getParams() != null) {
        for (org.sonar.wsclient.services.RuleParam wsParam : wsRule.getParams()) {
          if (wsParam.getValue() != null) {
            activeRule.setParameter(wsParam.getName(), wsParam.getValue());
          }
        }
      }
    }
  }

}
