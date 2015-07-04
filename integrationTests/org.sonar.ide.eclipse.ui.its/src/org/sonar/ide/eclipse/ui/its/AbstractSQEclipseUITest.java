/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2013 SonarSource
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package org.sonar.ide.eclipse.ui.its;

import java.io.IOException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.finders.UIThreadRunnable;
import org.eclipse.swtbot.swt.finder.results.VoidResult;
import org.eclipse.ui.PlatformUI;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.core.internal.servers.SonarServersManager;
import org.sonar.ide.eclipse.ui.its.utils.CaptureScreenshotOnFailure;
import org.sonar.ide.eclipse.ui.its.utils.SwtBotUtils;
import org.sonar.ide.eclipse.ui.its.utils.WorkspaceHelpers;

/**
 * @author Evgeny Mandrikov
 */
@SuppressWarnings("restriction")
public abstract class AbstractSQEclipseUITest extends AbstractSQEclipseTest {

  protected static SWTWorkbenchBot bot;

  @Rule
  public CaptureScreenshotOnFailure screenshot = new CaptureScreenshotOnFailure();

  @BeforeClass
  public final static void beforeClass() throws Exception {
    // Remove all configured servers and set default
    SonarServersManager serversManager = ((SonarServersManager) SonarCorePlugin.getServersManager());
    serversManager.clean();
    serversManager.addServer(SonarCorePlugin.getServersManager().create("for-its", getSonarServerUrl(), "", ""));

    // Trick to force activation of the Shell on xvfb
    UIThreadRunnable.syncExec(new VoidResult() {
      @Override
      public void run() {
        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().forceActive();
      }
    });

    bot = new SWTWorkbenchBot();

    SwtBotUtils.closeViewQuietly(bot, "org.eclipse.ui.internal.introview");
    SwtBotUtils.closeViewQuietly(bot, "org.eclipse.ui.views.ContentOutline");
    SwtBotUtils.closeViewQuietly(bot, "org.eclipse.mylyn.tasks.ui.views.tasks");

    // Clean out projects left over from previous test runs
    clean();

    SwtBotUtils.openPerspective(bot, JavaUI.ID_PERSPECTIVE);

  }

  @AfterClass
  public final static void afterClass() throws Exception {
    try {
      clean();
    } catch (Exception e) {
      // Silently ignore exceptions at this point
      System.err.println("[WARN] Error during cleanup: " + e.getMessage());
    }
  }

  private static void clean() throws InterruptedException, CoreException {
    WorkspaceHelpers.cleanWorkspace(bot);
    bot.resetWorkbench();
  }

  public static String getProjectPath(String name) throws IOException {
    return getProject(name).getCanonicalPath();
  }
}
