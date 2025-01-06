/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2025 SonarSource SA
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

import org.osgi.framework.Version;

/** In SonarLint we have a qualifier (the build number) but that can be ignored */
public class SonarLintVersion {
  public final int major;
  public final int minor;
  public final int patch;

  public SonarLintVersion(Version bundleVersion) {
    major = bundleVersion.getMajor();
    minor = bundleVersion.getMinor();
    patch = bundleVersion.getMicro();
  }

  public boolean isNewerThan(SonarLintVersion other) {
    return (major > other.major)
      || (major == other.major && minor > other.minor)
      || (major == other.major && minor == other.minor && patch > other.patch);
  }

  @Override
  public String toString() {
    return major + "." + minor + "." + patch;
  }
}
