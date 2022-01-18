/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2022 SonarSource SA
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
package org.sonarlint.eclipse.cdt.internal;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.function.Predicate;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.IScannerInfoProvider;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.analysis.IPreAnalysisContext;
import org.sonarlint.eclipse.core.internal.resources.DefaultSonarLintProjectAdapter;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CProjectConfiguratorTest {
  private CdtUtils configurator;

  private BuildWrapperJsonFactory jsonFactory;
  private CCorePlugin cCorePlugin;
  private Predicate<IFile> fileValidator;
  private SonarLintLogger logger;

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  @Before
  public void setUp() {
    cCorePlugin = mock(CCorePlugin.class);
    jsonFactory = mock(BuildWrapperJsonFactory.class);
    fileValidator = mock(Predicate.class);
    logger = mock(SonarLintLogger.class);
    configurator = new CdtUtils(jsonFactory, cCorePlugin, fileValidator, (proj, path) -> null, logger);
  }

  @Test
  public void should_configure() throws Exception {
    var projectBaseDir = temp.newFolder().toPath();
    var project = mock(IProject.class);
    var file = mock(IFile.class);
    when(file.getProject()).thenReturn(project);
    var monitor = mock(IProgressMonitor.class);
    var infoProvider = mock(IScannerInfoProvider.class);
    var info = mock(IScannerInfo.class);

    when(cCorePlugin.getScannerInfoProvider(project)).thenReturn(infoProvider);
    when(project.getLocation()).thenReturn(Path.fromOSString(projectBaseDir.toString()));
    when(infoProvider.getScannerInformation(file)).thenReturn(info);
    when(fileValidator.test(file)).thenReturn(true);
    when(jsonFactory.create(anyCollection(), anyString())).thenReturn("json");

    var context = mock(IPreAnalysisContext.class);
    var slProject = new DefaultSonarLintProjectAdapter(project);
    when(context.getProject()).thenReturn(slProject);
    var slFile = mock(ISonarLintFile.class);
    when(slFile.getProjectRelativePath()).thenReturn(Paths.get("file1").toString());
    when(context.getFilesToAnalyze()).thenReturn(Collections.singleton(slFile));
    when(context.getAnalysisTemporaryFolder()).thenReturn(temp.getRoot().toPath());

    configurator.configure(context, monitor);

    // json created
    verify(jsonFactory).create(anyCollection(), eq(projectBaseDir.toAbsolutePath().toString()));

    // json written
    assertThat(temp.getRoot().toPath().resolve("build-wrapper-dump.json")).hasContent("json");

    // property created
    verify(context).setAnalysisProperty("sonar.cfamily.build-wrapper-output", temp.getRoot().toPath().toString());
    verify(context).setAnalysisProperty("sonar.cfamily.useCache", "false");

    // no errors
    verify(logger, never()).error(Mockito.any(), Mockito.any());
    verify(logger, never()).error(Mockito.any());
  }

}
