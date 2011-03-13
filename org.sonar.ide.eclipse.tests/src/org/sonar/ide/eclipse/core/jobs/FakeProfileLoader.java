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
package org.sonar.ide.eclipse.core.jobs;

import org.sonar.api.CoreProperties;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.batch.ProfileLoader;

public class FakeProfileLoader implements ProfileLoader {

  public RulesProfile load(Project project) {
    RulesProfile profile = RulesProfile.create("Test", "java");
    Rule rule;

    rule = Rule.create()
        .setRepositoryKey(CoreProperties.PMD_PLUGIN)
        .setName("EmptyInitializer")
        .setKey("EmptyInitializer")
        .setConfigKey("rulesets/basic.xml/EmptyInitializer")
        .setSeverity(RulePriority.BLOCKER);
    profile.activateRule(rule, null);

    rule = Rule.create()
        .setRepositoryKey(CoreProperties.CHECKSTYLE_PLUGIN)
        .setName("Hide Utility Class Constructor")
        .setKey("com.puppycrawl.tools.checkstyle.checks.design.HideUtilityClassConstructorCheck")
        .setConfigKey("Checker/TreeWalker/HideUtilityClassConstructor")
        .setSeverity(RulePriority.MAJOR);
    profile.activateRule(rule, null);

    rule = Rule.create()
        .setRepositoryKey(CoreProperties.FINDBUGS_PLUGIN)
        .setName("Correctness - Self assignment of field")
        .setKey("SA_FIELD_SELF_ASSIGNMENT")
        .setConfigKey("SA_FIELD_SELF_ASSIGNMENT")
        .setSeverity(RulePriority.CRITICAL);
    profile.activateRule(rule, null);

    return profile;
  }
}
