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
package org.sonarlint.eclipse.core.internal.telemetry;

import java.net.MalformedURLException;
import java.net.URL;
import org.sonarlint.eclipse.core.documentation.SonarLintDocumentation;

public enum LinkTelemetry {

  RULES_SELECTION_DOCS("rulesSelectionDocs", SonarLintDocumentation.RULES_SELECTION),
  CONNECTED_MODE_DOCS("connectedModeDocs", SonarLintDocumentation.CONNECTED_MODE_LINK),
  COMPARE_SERVER_PRODUCTS("compareServerProducts", SonarLintDocumentation.COMPARE_SERVER_PRODUCTS_LINK),
  SONARQUBE_EDITIONS_DOWNLOADS("sonarQubeEditionsDownloads", SonarLintDocumentation.SONARQUBE_EDITIONS_LINK),
  SONARCLOUD_PRODUCT_PAGE("sonarCloudProductPage", SonarLintDocumentation.SONARCLOUD_PRODUCT_LINK),
  SONARCLOUD_SIGNUP_PAGE("sonarCloudSignUpPage", SonarLintDocumentation.SONARCLOUD_SIGNUP_LINK);

  private final String linkId;
  private final URL url;

  LinkTelemetry(String linkId, String url) {
    this.linkId = linkId;
    try {
      this.url = new URL(url);
    } catch (MalformedURLException e) {
      throw new IllegalStateException(e);
    }
  }

  public String getLinkId() {
    return this.linkId;
  }

  public URL getUrl() {
    return this.url;
  }

}
