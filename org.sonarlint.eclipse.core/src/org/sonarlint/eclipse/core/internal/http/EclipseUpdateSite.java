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
package org.sonarlint.eclipse.core.internal.http;

import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Pattern;
import org.eclipse.jdt.annotation.Nullable;
import org.osgi.framework.Version;
import org.sonarlint.eclipse.core.internal.backend.SonarLintBackendService;
import org.sonarlint.eclipse.core.internal.utils.SonarLintVersion;

/** Used to interact with the official SonarLint for Eclipse Update Site */
public class EclipseUpdateSite {
  private static final String COMPOSITE_CONTENT_XML = "https://binaries.sonarsource.com/SonarLint-for-Eclipse/releases/compositeContent.xml";
  private static final String COMPOSITE_ARTIFACTS_XML = "https://binaries.sonarsource.com/SonarLint-for-Eclipse/releases/compositeArtifacts.xml";

  private static final Pattern PATTERN_UPDATE_SITE = Pattern.compile("\\<child\\s+location\\=\\\"(.*?)\\\"\\/\\>", Pattern.CASE_INSENSITIVE);
  private static final Pattern PATTERN_VERSION = Pattern.compile("(\\d{1,2}\\.\\d{1,2}\\.\\d{1,2}\\.\\d{5,}?)", Pattern.CASE_INSENSITIVE);

  private static HttpClient client = null;

  private EclipseUpdateSite() {
    // utility class
  }

  /**
   *  Get the newest available SonarLint for Eclipse version if possible from the official Eclipse Update Site.
   *
   *  @return the newest version when found, null otherwise
   */
  @Nullable
  public static SonarLintVersion getNewestVersion() {
    var xml = getEclipseUpdateSiteContent();
    if (xml == null) {
      return null;
    }

    return parseXmlIntoSonarLintVersion(xml);
  }

  /** This way the HTTP client is only created on demand and in that case only once */
  private static HttpClient getClient() {
    if (client == null) {
      client = new HttpClient(SonarLintBackendService.get().getHttpConfiguration());
    }

    return client;
  }

  /**
   *  Loads the content from the official SonarLint for Eclipse Update Site via either the compositeContent.xml or the
   *  compositeArtifacts.xml if the former is not available.
   *
   *  @return website content if Eclipse Update Site was available, null otherwise
   */
  @Nullable
  public static String getEclipseUpdateSiteContent() {
    var httpClient = getClient();

    var response = httpClient.getWebsiteContent(COMPOSITE_CONTENT_XML);
    if (response != null) {
      return response;
    }
    response = httpClient.getWebsiteContent(COMPOSITE_ARTIFACTS_XML);
    if (response != null) {
      return response;
    }
    return null;
  }

  /**
   *  Parses the linked Eclipse Update Sites and after that versions to find the newest version provided. Based on the
   *  official Eclipse Equinox p2 definition: https://wiki.eclipse.org/Equinox/p2/Composite_Repositories_(new)
   *
   *  Our SonarLint for Eclipse Update Site contains more than one: The latest for Java 8 based IDEs and newer ones.
   *
   *  @param xml content from either the compositeContent.xml / compositeArtifacts.xml
   *  @return newest version if it could be parsed, null otherwise
   */
  @Nullable
  public static SonarLintVersion parseXmlIntoSonarLintVersion(String xml) {
    var updateSites = new ArrayList<String>();
    var matcher = PATTERN_UPDATE_SITE.matcher(xml);
    while (matcher.find()) {
      updateSites.add(matcher.group());
    }
    if (updateSites.isEmpty()) {
      return null;
    }

    var versions = new ArrayList<Version>();
    for (var updateSite : updateSites) {
      matcher = PATTERN_VERSION.matcher(updateSite);
      while (matcher.find()) {
        versions.add(Version.valueOf(matcher.group()));
      }
    }

    if (versions.isEmpty()) {
      return null;
    }
    Collections.sort(versions, Comparable::compareTo);
    return new SonarLintVersion(versions.get(versions.size() - 1));
  }
}
