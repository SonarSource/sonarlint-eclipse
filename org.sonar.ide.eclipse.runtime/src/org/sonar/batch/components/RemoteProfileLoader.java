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
package org.sonar.batch.components;

import org.apache.commons.lang.StringUtils;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.batch.ProfileLoader;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.ResourceQuery;
import org.sonar.wsclient.services.RuleQuery;

import java.util.List;

/**
 * Loads {@link RulesProfile} from remote Sonar server.
 */
public class RemoteProfileLoader implements ProfileLoader {

  private final Sonar sonar;
  private final RuleFinder ruleFinder;

  public RemoteProfileLoader(Sonar sonar, RuleFinder ruleFinder) {
    this.sonar = sonar;
    this.ruleFinder = ruleFinder;
  }

  public RulesProfile load(Project project) {
    try {
      String profileName = sonar.find(ResourceQuery.createForMetrics(project.getKey(), CoreMetrics.PROFILE_KEY))
          .getMeasure(CoreMetrics.PROFILE_KEY).getData();

      RulesProfile profile = RulesProfile.create(profileName, project.getLanguageKey());

      RuleQuery ruleQuery = new RuleQuery(profile.getLanguage()).setProfile(profile.getName()).setActive(true);
      List<org.sonar.wsclient.services.Rule> wsRules = sonar.findAll(ruleQuery);
      for (org.sonar.wsclient.services.Rule wsRule : wsRules) {
        activate(profile, wsRule);
      }
      return profile;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
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
