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
package org.sonarlint.eclipse.core.internal.jobs;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.analysis.IPreAnalysisContext;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

public class DefaultPreAnalysisContext implements IPreAnalysisContext {

  private final ISonarLintProject project;
  private final Map<String, String> analysisProperties;
  private final Path tempDir;
  private final Map<ISonarLintFile, EclipseInputFile> filesToAnalyze;

  public DefaultPreAnalysisContext(ISonarLintProject project, Map<String, String> analysisProperties, List<EclipseInputFile> filesToAnalyze, Path tempDir) {
    this.project = project;
    this.analysisProperties = analysisProperties;
    this.filesToAnalyze = Collections
      .unmodifiableMap(filesToAnalyze.stream()
        .collect(Collectors.toMap(EclipseInputFile::getFile, Function.identity())));
    this.tempDir = tempDir;
  }

  @Override
  public ISonarLintProject getProject() {
    return project;
  }

  @Override
  public void setAnalysisProperty(String key, String value) {
    if (analysisProperties.containsKey(key)) {
      SonarLintLogger.get().debug("Property '" + key + "' was already set to value '" + analysisProperties.get(key) + "' and will be overridden by '" + value + "'");
    }
    analysisProperties.put(key, value);
  }

  @Override
  public void setAnalysisProperty(String key, Collection<String> values) {
    setAnalysisProperty(key, StringUtils.joinWithCommaSkipNull(values));
  }

  @Override
  public Collection<ISonarLintFile> getFilesToAnalyze() {
    return filesToAnalyze.keySet();
  }

  /**
   * Used by CDT that need physical path to analyze files
   */
  public String getLocalPath(ISonarLintFile file) {
    return filesToAnalyze.get(file).getPath();
  }

  @Override
  public Path getAnalysisTemporaryFolder() {
    return tempDir;
  }

}
