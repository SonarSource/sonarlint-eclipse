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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintGlobalConfiguration;

public class JavaRuntimeUtils {
  public static final String SYSTEM_PROPERTY_ECLIPSE_VM = "eclipse.vm";
  public static final String SYSTEM_PROPERTY_JAVA_HOME = "java.home";
  public static final String SYSTEM_PROPERTY_JAVA_CLASS_VERSION = "java.class.version";
  public static final float JAVA_17_CLASS_VERSION = 61.0f;

  private JavaRuntimeUtils() {
    // utility class
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

  /** Get the Java runtime information consumed by SonarLint out of process that matches all the criteria */
  public static JavaRuntimeInformation getJavaRuntime() {
    // Check if user provided Java runtime, cannot check if actually Java 17+!
    var javaPath = SonarLintGlobalConfiguration.getJava17Path();
    if (javaPath != null && checkForJavaExecutable(javaPath)) {
      return new JavaRuntimeInformation(JavaRuntimeProvider.SELF_MANAGED, javaPath);
    }

    // Check if Eclipse provided Java runtime -> eclipse.vm must match java.home
    try {
      var eclipseVm = System.getProperty(SYSTEM_PROPERTY_ECLIPSE_VM);
      var javaHome = System.getProperty(SYSTEM_PROPERTY_JAVA_HOME);
      var javaClassVersion = System.getProperty(SYSTEM_PROPERTY_JAVA_CLASS_VERSION);
      if (eclipseVm != null && javaHome != null && javaClassVersion != null) {
        var eclipseVmAbsolutePath = Paths.get(eclipseVm).toAbsolutePath();
        var javaHomeAbsolutePath = Paths.get(javaHome).toAbsolutePath();
        var javaClassVersionFloat = Float.parseFloat(javaClassVersion);
        if (eclipseVmAbsolutePath.startsWith(javaHomeAbsolutePath) && javaClassVersionFloat >= JAVA_17_CLASS_VERSION) {
          return new JavaRuntimeInformation(JavaRuntimeProvider.ECLIPSE_MANAGED, javaHomeAbsolutePath);
        }
      }
    } catch (Exception err) {
      SonarLintLogger.get().error("Cannot check Eclipse provided Java runtime to be used by SonarLint", err);
    }

    return new JavaRuntimeInformation(JavaRuntimeProvider.SONARLINT_BUNDLED, null);
  }

  public enum JavaRuntimeProvider {
    SONARLINT_BUNDLED,
    ECLIPSE_MANAGED,
    SELF_MANAGED;
  }

  public static class JavaRuntimeInformation {
    private final JavaRuntimeProvider provider;
    @Nullable
    private final Path path;

    public JavaRuntimeInformation(JavaRuntimeProvider provider, @Nullable Path path) {
      this.provider = provider;
      this.path = path;
    }

    public JavaRuntimeProvider getProvider() {
      return provider;
    }

    @Nullable
    public Path getPath() {
      return path;
    }
  }
}
