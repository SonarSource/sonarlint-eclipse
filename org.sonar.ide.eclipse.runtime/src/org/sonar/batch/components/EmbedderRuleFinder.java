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

import java.util.Collection;

import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;

public class EmbedderRuleFinder implements RuleFinder {

  private RulesProfile profile;

  public EmbedderRuleFinder(RulesProfile profile) {
    this.profile = profile;
  }

  public Rule find(RuleQuery query) {
    throw new UnsupportedOperationException();
  }

  public Collection<Rule> findAll(RuleQuery query) {
    throw new UnsupportedOperationException();
  }

  public Rule findById(int id) {
    throw new UnsupportedOperationException();
  }

  public Rule findByKey(String repositoryKey, String key) {
    ActiveRule activeRule = profile.getActiveRule(repositoryKey, key);
    if (activeRule != null) {
      return activeRule.getRule();
    }
    return null;
  }

}
