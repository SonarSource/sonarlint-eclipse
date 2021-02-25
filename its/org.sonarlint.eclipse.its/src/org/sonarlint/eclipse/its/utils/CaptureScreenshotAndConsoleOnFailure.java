/*
 * SonarLint for Eclipse ITs
 * Copyright (C) 2009-2021 SonarSource SA
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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;
import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
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
          try {
            String fileName = constructFilename(description, ".png");
            SWTWorkbenchBot bot = new SWTWorkbenchBot();
            bot.captureScreenshot(fileName);
            bot.closeAllShells();

            WorkspaceHelpers.withSonarLintConsole(bot, c -> {
              try {
                FileUtils.write(new File(constructFilename(description, "-console.txt")), c.getDocument().get(), StandardCharsets.UTF_8);
              } catch (IOException e) {
                throw new IllegalStateException(e);
              }
              return null;
            });
          } catch (Exception e) {
            onHold.addSuppressed(e);
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
