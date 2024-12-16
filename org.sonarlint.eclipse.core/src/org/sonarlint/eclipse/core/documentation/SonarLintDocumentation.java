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
package org.sonarlint.eclipse.core.documentation;

public class SonarLintDocumentation {

  private static final String BASE_DOCS_URL = "https://docs.sonarsource.com/sonarqube-for-ide/eclipse";
  public static final String CLEAN_AS_YOU_CODE = BASE_DOCS_URL + "/clean-as-you-code-in-the-ide";
  public static final String CONNECTED_MODE_LINK = BASE_DOCS_URL + "/team-features/connected-mode/";
  public static final String CONNECTED_MODE_BENEFITS = CONNECTED_MODE_LINK + "#benefits";
  public static final String CONNECTED_MODE_SETUP_LINK = BASE_DOCS_URL + "/team-features/connected-mode-setup/";
  public static final String CONNECTED_MODE_SHARING = CONNECTED_MODE_SETUP_LINK + "#reuse-the-binding-configuration";
  public static final String VERSION_SUPPORT_POLICY = CONNECTED_MODE_SETUP_LINK + "#sonarlint-sonarqube-version-support-policy";
  public static final String BRANCH_AWARENESS = CONNECTED_MODE_LINK + "#branch-awareness";
  public static final String SECURITY_HOTSPOTS_LINK = BASE_DOCS_URL + "/using-sonarlint/security-hotspots/";
  public static final String TAINT_VULNERABILITIES_LINK = BASE_DOCS_URL + "/using-sonarlint/taint-vulnerabilities/";
  public static final String ON_THE_FLY_VIEW_LINK = BASE_DOCS_URL + "/using-sonarlint/investigating-issues/#the-on-the-fly-view";
  public static final String ISSUE_TYPES_LINK = BASE_DOCS_URL + "/using-sonarlint/investigating-issues/#issue-types";
  public static final String REPORT_VIEW_LINK = BASE_DOCS_URL + "/using-sonarlint/investigating-issues/#the-report-view";
  public static final String ISSUE_PERIOD_LINK = BASE_DOCS_URL + "/using-sonarlint/investigating-issues/#focusing-on-new-code";
  public static final String MARK_ISSUES_LINK = BASE_DOCS_URL + "/using-sonarlint/fixing-issues/#marking-issues";
  public static final String FILE_EXCLUSIONS = BASE_DOCS_URL + "/using-sonarlint/file-exclusions/";
  public static final String RULES = BASE_DOCS_URL + "/using-sonarlint/rules/";
  public static final String RULES_SELECTION = RULES + "#rule-selection";
  public static final String TROUBLESHOOTING_LINK = BASE_DOCS_URL + "/troubleshooting/";
  public static final String ADVANCED_CONFIGURATION = BASE_DOCS_URL + "/team-features/advanced-configuration/";
  public static final String PROVIDE_JAVA_RUNTIME_LINK = ADVANCED_CONFIGURATION + "#providing-a-java-runtime";

  private static final String BASE_MARKETING_URL = "https://www.sonarsource.com";
  public static final String COMPARE_SERVER_PRODUCTS_LINK = BASE_MARKETING_URL + "/open-source-editions";
  public static final String SONARQUBE_EDITIONS_LINK = COMPARE_SERVER_PRODUCTS_LINK + "/sonarqube-community-edition";
  public static final String SONARCLOUD_PRODUCT_LINK = BASE_MARKETING_URL + "/products/sonarcloud";
  public static final String SONARCLOUD_SIGNUP_LINK = SONARCLOUD_PRODUCT_LINK + "/signup";

  public static final String SONARQUBE_SMART_NOTIFICATIONS = "https://docs.sonarsource.com/sonarqube/latest/user-guide/sonarlint-connected-mode/#smart-notifications";
  public static final String SONARCLOUD_SMART_NOTIFICATIONS = "https://docs.sonarsource.com/sonarcloud/improving/sonarlint/#smart-notifications";

  public static final String COMMUNITY_FORUM = "https://community.sonarsource.com/c/sl/11";
  public static final String COMMUNITY_FORUM_ECLIPSE_RELEASES = "https://community.sonarsource.com/tags/c/sl/sonarlint-releases/37/eclipse";

  public static final String GITHUB_RELEASES = "https://github.com/SonarSource/sonarlint-eclipse/releases";

  // Links to Eclipse plug-ins that are used and have sub-plug-ins
  public static final String ECLIPSE_JDT = "https://projects.eclipse.org/projects/eclipse.jdt";
  public static final String ECLIPSE_CDT = "https://projects.eclipse.org/projects/tools.cdt";
  public static final String ECLIPSE_M2E = "https://projects.eclipse.org/projects/technology.m2e";
  public static final String ECLIPSE_BUILDSHIP = "https://projects.eclipse.org/projects/tools.buildship";

  // Eclipse CDT documentation
  public static final String ECLIPSE_CDT_DOCS = "https://help.eclipse.org/latest/index.jsp?topic=%2Forg.eclipse.cdt.doc.user%2Fconcepts%2Fcdt_o_home.htm";

  private SonarLintDocumentation() {
    // utility class
  }

}
