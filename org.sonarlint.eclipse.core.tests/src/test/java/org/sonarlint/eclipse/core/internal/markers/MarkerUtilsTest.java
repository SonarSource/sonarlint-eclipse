/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2023 SonarSource SA
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
package org.sonarlint.eclipse.core.internal.markers;

import java.util.Collections;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ProjectScope;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.resources.DefaultSonarLintFileAdapter;
import org.sonarlint.eclipse.core.internal.resources.DefaultSonarLintProjectAdapter;
import org.sonarlint.eclipse.tests.common.SonarTestCase;
import org.sonarsource.sonarlint.core.commons.CleanCodeAttribute;
import org.sonarsource.sonarlint.core.commons.IssueSeverity;
import org.sonarsource.sonarlint.core.commons.TextRange;
import org.sonarsource.sonarlint.core.serverapi.proto.sonarqube.ws.Common.RuleType;

import static org.assertj.core.api.Assertions.assertThat;

public class MarkerUtilsTest extends SonarTestCase {

  private static IProject project;

  @BeforeClass
  public static void importProject() throws Exception {
    project = importEclipseProject("SimpleProject");
    // Configure the project
    SonarLintCorePlugin.getInstance().getProjectConfigManager().load(new ProjectScope(project), "A Project");
  }
  
  /** While we couldn't test this via integration tests, this must do it otherwise promotion failed due to SQ QG failing */
  @Test
  public void test_MarkerAttributesEncodingDecoding() {
    assertThat(MarkerUtils.decodeRuleType(null)).isNull();
    assertThat(MarkerUtils.decodeRuleType(RuleType.BUG.name()).name()).isEqualTo(RuleType.BUG.name());
    assertThat(MarkerUtils.decodeSeverity(null)).isNull();
    assertThat(MarkerUtils.decodeSeverity(IssueSeverity.BLOCKER.name()).name()).isEqualTo(IssueSeverity.BLOCKER.name());
    assertThat(MarkerUtils.encodeCleanCodeAttribute(null)).isNull();
    assertThat(MarkerUtils.encodeCleanCodeAttribute(CleanCodeAttribute.CLEAR)).isEqualTo(CleanCodeAttribute.CLEAR.name());
    assertThat(MarkerUtils.decodeCleanCodeAttribute(null)).isNull();
    assertThat(MarkerUtils.decodeCleanCodeAttribute(CleanCodeAttribute.CLEAR.name()).name()).isEqualTo(CleanCodeAttribute.CLEAR.name());
    assertThat(MarkerUtils.encodeHighestImpact(null)).isNull();
    assertThat(MarkerUtils.encodeHighestImpact(Collections.emptyMap())).isNull();
    assertThat(MarkerUtils.encodeImpacts(null)).isNull();
    assertThat(MarkerUtils.encodeImpacts(Collections.emptyMap())).isNull();
    assertThat(MarkerUtils.decodeImpacts(null)).isEqualTo(Collections.emptyMap());
  }

  @Test
  public void testPreciseIssueLocationSingleLine() throws Exception {
    var file = new DefaultSonarLintFileAdapter(new DefaultSonarLintProjectAdapter(project), project.getFile("src/main/java/ViolationOnFile.java"));
    var textRange = new TextRange(2, 23, 2, 31);
    var position = MarkerUtils.getPosition(file.getDocument(), textRange);
    assertThat(position.getOffset()).isEqualTo(54);
    assertThat(position.getLength()).isEqualTo(8);
    assertThat(file.getDocument().get(position.getOffset(), position.getLength())).isEqualTo("INSTANCE");
  }

  @Test
  public void testPreciseIssueLocationMultiLine() throws Exception {
    var file = new DefaultSonarLintFileAdapter(new DefaultSonarLintProjectAdapter(project), project.getFile("src/main/java/ViolationOnFile.java"));
    var textRange = new TextRange(4, 34, 5, 12);
    var position = MarkerUtils.getPosition(file.getDocument(), textRange);
    assertThat(position.getOffset()).isEqualTo(101);
    assertThat(position.getLength()).isEqualTo(18);
    assertThat(file.getDocument().get(position.getOffset(), position.getLength())).isEqualTo("\"foo\"\n     + \"bar\"");
  }

  @Test
  public void testNonexistentLine() throws Exception {
    var file = new DefaultSonarLintFileAdapter(new DefaultSonarLintProjectAdapter(project), project.getFile("src/main/java/ViolationOnFile.java"));
    var nonexistentLine = file.getDocument().getNumberOfLines() + 1;
    var position = MarkerUtils.getPosition(file.getDocument(), nonexistentLine);
    assertThat(position).isNull();
  }

  @Test
  public void testNonexistentPosition() throws Exception {
    var file = new DefaultSonarLintFileAdapter(new DefaultSonarLintProjectAdapter(project), project.getFile("src/main/java/ViolationOnFile.java"));
    var nonexistentLine = file.getDocument().getNumberOfLines() + 1;
    var position = MarkerUtils.getPosition(file.getDocument(), new TextRange(nonexistentLine, 0, nonexistentLine, 10));
    assertThat(position).isNull();
  }

}
