/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2020 SonarSource SA
 * sonarlint@sonarsource.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarlint.eclipse.its.utils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.TextConsole;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class CaptureScreenshotAndConsoleOnFailure implements TestRule {
  @Override
  public final Statement apply(final Statement base, final Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        try {
          base.evaluate();
        } catch (Throwable onHold) {
          String fileName = constructFilename(description, ".png");
          new SWTWorkbenchBot().captureScreenshot(fileName);

          IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();

          IConsole[] consoles = manager.getConsoles();
          for (IConsole iConsole : consoles) {
            if ("SonarLint".equals(iConsole.getName())) {
              FileUtils.write(new File(constructFilename(description, "-console.txt")), ((TextConsole) iConsole).getDocument().get(), StandardCharsets.UTF_8);
            }
          }

          throw onHold;
        }
      }

      private String constructFilename(final Description description, String suffix) {
        return "./target/output/"
          + description.getClassName() + "."
          + description.getMethodName() + suffix;
      }
    };
  }

}
