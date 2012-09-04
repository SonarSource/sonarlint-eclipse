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

import org.junit.Test;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleRepository;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class EmbedderRuleFinderTest {
  @Test
  public void test() {
    final Rule rule1 = Rule.create("pmd", "rule1", "name1");
    RuleRepository ruleRepository = new RuleRepository("pmd", "java") {
      @Override
      public List<Rule> createRules() {
        return Arrays.asList(rule1);
      }
    };
    EmbedderRuleFinder ruleFinder = new EmbedderRuleFinder(new RuleRepository[] { ruleRepository });

    assertThat(ruleFinder.findByKey("pmd", "rule1"), is(rule1));
    assertThat(ruleFinder.findByKey("pmd", "notfound"), nullValue());
    assertThat(ruleFinder.findByKey("notfound", "rule1"), nullValue());
  }
}
