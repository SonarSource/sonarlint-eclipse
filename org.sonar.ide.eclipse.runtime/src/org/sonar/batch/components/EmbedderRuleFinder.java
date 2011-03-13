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

import com.google.common.collect.Maps;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.rules.RuleRepository;
import org.sonar.api.utils.Logs;

import java.util.Collection;
import java.util.Map;

public class EmbedderRuleFinder implements RuleFinder {

  private final RuleRepository[] repositories;

  private final Map<String, Map<String, Rule>> rules = Maps.newHashMap();

  public EmbedderRuleFinder(RuleRepository[] repositories) {
    this.repositories = repositories;
  }

  public void start() {
    for (RuleRepository repository : repositories) {
      registerRepository(repository);
    }
  }

  private void registerRepository(RuleRepository repository) {
    Map<String, Rule> rulesByKey = Maps.newHashMap();
    for (Rule rule : repository.createRules()) {
      rule.setRepositoryKey(repository.getKey());
      rulesByKey.put(rule.getKey(), rule);
    }
    Logs.INFO.info("Registered repository " + repository.getKey() + "/" + repository.getLanguage()
        + " with " + rulesByKey.size() + " rules");
    rules.put(repository.getKey(), rulesByKey);
  }

  public Rule find(RuleQuery query) {
    throw new EmbedderUnsupportedOperationException();
  }

  public Collection<Rule> findAll(RuleQuery query) {
    throw new EmbedderUnsupportedOperationException();
  }

  public Rule findById(int id) {
    throw new EmbedderUnsupportedOperationException("Searching rule by id doesn't make sense without database");
  }

  public Rule findByKey(String repositoryKey, String key) {
    Map<String, Rule> rulesByKey = rules.get(repositoryKey);
    if (rulesByKey != null) {
      return rulesByKey.get(key);
    }
    return null;
  }

}
