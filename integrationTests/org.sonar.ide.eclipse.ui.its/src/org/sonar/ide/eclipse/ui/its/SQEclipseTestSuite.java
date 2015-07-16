/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2013 SonarSource
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package org.sonar.ide.eclipse.ui.its;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.locator.FileLocation;
import java.io.File;
import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.Platform;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.osgi.framework.Bundle;

/**
 * Common test case for sonar-ide/eclipse projects.
 */
@RunWith(Suite.class)
@SuiteClasses({CompareWithSonarActionTest.class, ConfigurationTest.class, ConfigureProjectTest.class, IssuesViewTest.class,
  LocalAnalysisTest.class, M2eConnectorTest.class, NonSonarProjectsFilterTest.class, OpenInBrowserActionTest.class})
public class SQEclipseTestSuite {

  private static File javaProfile = null;
  private static File pythonProfile = null;

  static {
    try {
      Bundle bundle = Platform.getBundle("org.sonar.ide.eclipse.ui.its");
      javaProfile = File.createTempFile("profile", ".xml");
      javaProfile.deleteOnExit();
      pythonProfile = File.createTempFile("profile", ".xml");
      pythonProfile.deleteOnExit();
      FileUtils.copyURLToFile(bundle.getEntry("it-profile_java.xml"), javaProfile);
      FileUtils.copyURLToFile(bundle.getEntry("it-profile_py.xml"), pythonProfile);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Orchestrator.builderEnv()
    .addPlugin("java")
    .addPlugin("python")
    .setOrchestratorProperty("pmdVersion", "2.3")
    .addPlugin("pmd")
    .restoreProfileAtStartup(FileLocation.of(javaProfile))
    .restoreProfileAtStartup(FileLocation.of(pythonProfile))
    .build();

}
