/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2015 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.ide.eclipse.core.internal.jobs;

import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.util.List;
import java.util.Properties;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.sonar.ide.eclipse.core.internal.Messages;
import org.sonar.ide.eclipse.core.internal.jobs.functions.AnalyzeProjectJobFunction;
import org.sonar.ide.eclipse.core.internal.resources.SonarProperty;

/**
 * Live preview analysis job.
 *
 */
@SuppressWarnings("nls")
public class AnalyzeProjectJob extends Job {

  private final AnalyzeProjectJobFunction analyzeProjectJobFunction;

  /**
   * @param request Job request
   */
  public AnalyzeProjectJob(final AnalyseProjectRequest request) {
    super(Messages.AnalyseProjectJob_title);
    // Prevent modifications of project during analysis
    setRule(ResourcesPlugin.getWorkspace().getRuleFactory().buildRule());
    analyzeProjectJobFunction = new AnalyzeProjectJobFunction(request);
  }

  @Override
  protected IStatus run(final IProgressMonitor monitor) {
    return analyzeProjectJobFunction.run(monitor);
  }

  /**
   * @param incremental
   */
  @VisibleForTesting
  public void setIncremental(final boolean incremental) {
    analyzeProjectJobFunction.setIncremental(incremental);
  }

  @VisibleForTesting
  public void createMarkersFromReportOutput(final File outputFile) {
    analyzeProjectJobFunction.createMarkersFromReportOutput(outputFile);
  }

  @VisibleForTesting
  public File configureAnalysis(final IProgressMonitor monitor, final Properties properties, final List<SonarProperty> extraProps) {
    return analyzeProjectJobFunction.configureAnalysis(monitor, properties, extraProps);
  }

}
