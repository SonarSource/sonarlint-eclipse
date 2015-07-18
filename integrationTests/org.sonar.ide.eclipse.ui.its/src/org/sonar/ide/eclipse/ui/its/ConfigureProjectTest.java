/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2013 SonarSource
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package org.sonar.ide.eclipse.ui.its;

import com.google.common.base.Joiner;
import java.util.List;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.junit.Test;
import org.sonar.ide.eclipse.core.internal.resources.ISonarProject;
import org.sonar.ide.eclipse.core.internal.resources.SonarProject;
import org.sonar.ide.eclipse.ui.its.bots.ConfigureProjectsWizardBot;
import org.sonar.ide.eclipse.ui.its.bots.ImportProjectBot;
import org.sonar.ide.eclipse.ui.its.bots.JavaPackageExplorerBot;

import static org.assertj.core.api.Assertions.fail;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ConfigureProjectTest extends AbstractSQEclipseUITest {
  private static final String PROJECT_NAME = "reference";

  @Test
  public void canAssociateWithSonar() throws Exception {
    new ImportProjectBot(bot).setPath(getProjectPath(PROJECT_NAME)).finish();

    new JavaPackageExplorerBot(bot)
      .expandAndSelect(PROJECT_NAME)
      .clickContextMenu("Configure", "Associate with SonarQube...");

    ConfigureProjectsWizardBot projectWizardBot = new ConfigureProjectsWizardBot(bot);

    assertThat(projectWizardBot.getAssociatedProjectText(0), is("reference on " + getSonarServerUrl() + " (org.sonar-ide.tests:reference)"));
    projectWizardBot.finish();

    IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(PROJECT_NAME);
    ISonarProject sonarProject = SonarProject.getInstance(project);
    assertThat(sonarProject, is(notNullValue()));
    assertThat(sonarProject.getKey(), is("org.sonar-ide.tests:reference"));

    new JavaPackageExplorerBot(bot)
      .expandAndSelect(PROJECT_NAME)
      .clickContextMenu("SonarQube", "Change Project Association...");

    projectWizardBot = new ConfigureProjectsWizardBot(bot);
    projectWizardBot.editRow(0);

    // SONARIDE-362 Verify you can search for projects with less than 3 characters
    List<String> autoCompleteProposals = projectWizardBot.getAutoCompleteProposals("p2");
    assertTrue("P2 was not found in autocomplete: " + Joiner.on(", ").join(autoCompleteProposals), autoCompleteProposals.contains("P2"));

    // Select second choice in content assist to take the branch
    projectWizardBot.editRow(0);
    autoCompleteProposals = projectWizardBot.getAutoCompleteProposals("reference");
    try {
      projectWizardBot.autoCompleteProposal("reference", "reference BRANCH-0.9");
    } catch (Exception e) {
      fail("List of content assist proposals: " + Joiner.on(',').join(autoCompleteProposals), e);
    }
    assertThat(projectWizardBot.getAssociatedProjectText(0), is("reference BRANCH-0.9 on " + getSonarServerUrl() + " (org.sonar-ide.tests:reference:BRANCH-0.9)"));
    projectWizardBot.finish();

    sonarProject = SonarProject.getInstance(project);
    assertThat(sonarProject, is(notNullValue()));
    assertThat(sonarProject.getKey(), is("org.sonar-ide.tests:reference:BRANCH-0.9"));
  }

}
