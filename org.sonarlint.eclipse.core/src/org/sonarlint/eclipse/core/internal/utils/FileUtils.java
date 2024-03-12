/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2024 SonarSource SA
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
package org.sonarlint.eclipse.core.internal.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Locale;
import org.sonarlint.eclipse.core.SonarLintLogger;

public class FileUtils {

  private FileUtils() {
    // Utility class
  }

  public static void deleteRecursively(Path dir) {
    try (var filesStream = Files.walk(dir)) {
      filesStream
        .sorted(Comparator.reverseOrder())
        .map(Path::toFile)
        .forEach(File::delete);
    } catch (Exception e) {
      SonarLintLogger.get().error("Unable to delete directory: " + dir, e);
    }
  }

  public static void mkdirs(Path path) {
    try {
      Files.createDirectories(path);
    } catch (IOException e) {
      throw new IllegalStateException("Unable to create directory: " + path, e);
    }
  }

  /**
   *  Check basic Java installation structures for macOS / Linux / Windows
   *  -> ${value}/bin/java or ${value}/bin/java.exe
   *  -> We do not care for macOS JDK bundles (build like a macOS application)!
   *
   *  @param installationDirectory where to look
   *  @return true if executable found, false otherwise
   */
  public static boolean checkForJavaExecutable(Path installationDirectory) {
    // Check basic Java installation structures for macOS / Linux / Windows
    // -> ${value}/bin/java or ${value}/bin/java.exe
    // -> We do not care for macOS JDK bundles (build like a macOS application)!
    Path executable;

    var osName = System.getProperty("os.name");
    if (osName != null && osName.toLowerCase(Locale.getDefault()).startsWith("win")) {
      executable = installationDirectory.resolve("bin/java.exe");
    } else {
      executable = installationDirectory.resolve("bin/java");
    }

    return Files.exists(executable);
  }
}
