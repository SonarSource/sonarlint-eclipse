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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;

public class RemoteProfileLoaderTest {
  private RuleFinder ruleFinder;
  private RemoteProfileLoader loader;
  private RulesProfile profile;

  @Before
  public void setUp() {
    ruleFinder = mock(RuleFinder.class);
    loader = new RemoteProfileLoader(ruleFinder);
    profile = mock(RulesProfile.class);
  }

  @Test
  public void shouldNotActivateRuleIfNotFound() {
    org.sonar.wsclient.services.Rule wsRule = new org.sonar.wsclient.services.Rule();
    loader.activate(profile, wsRule);

    verifyNoMoreInteractions(profile);
  }

  @Test
  public void shouldActivateRule() {
    Rule rule = Rule.create().setRepositoryKey("pmd").setKey("rule");
    when(ruleFinder.findByKey("pmd", "rule")).thenReturn(rule);

    org.sonar.wsclient.services.Rule wsRule = new org.sonar.wsclient.services.Rule()
        .setRepository("pmd")
        .setKey("pmd:rule")
        .setSeverity("MAJOR");
    loader.activate(profile, wsRule);

    verify(profile).activateRule(rule, RulePriority.MAJOR);
    verifyNoMoreInteractions(profile);
  }
}
