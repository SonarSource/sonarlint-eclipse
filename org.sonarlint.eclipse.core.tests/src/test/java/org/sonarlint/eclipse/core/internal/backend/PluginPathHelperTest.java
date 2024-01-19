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
package org.sonarlint.eclipse.core.internal.backend;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PluginPathHelperTest {

  @Test
  public void supportSpacesInAnalyzerLocation() throws MalformedURLException {
    Path path = PluginPathHelper
      .toPath(new URL("file:/C:/Program Files/Eclipse/2021-12/plugins/org.sonarlint.eclipse.core_7.2.1.42550/plugins/sonar-secrets-plugin-1.1.0.36766.jar"));
    assertThat(path).isNotNull();
  }

}
