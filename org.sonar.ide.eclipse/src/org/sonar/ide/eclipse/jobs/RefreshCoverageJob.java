/*
 * Copyright (C) 2010 Evgeny Mandrikov
 *
 * Sonar-IDE is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar-IDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar-IDE; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse.jobs;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.shared.coverage.CoverageLine;
import org.sonar.ide.shared.coverage.CoverageLoader;
import org.sonar.wsclient.Sonar;

/**
 * This class load code coverage in background.
 * 
 * @link http://jira.codehaus.org/browse/SONARIDE-60
 * 
 * @author Jérémie Lagarde
 */
public class RefreshCoverageJob extends AbstractRefreshModelJob<CoverageLine> {

  public RefreshCoverageJob(final List<IResource> resources) {
    super(resources, SonarPlugin.MARKER_COVERAGE_ID);
  }

  @Override
  protected Collection<CoverageLine> retrieveDatas(final Sonar sonar, final String resourceKey, final ICompilationUnit unit) {
    try {
      return CoverageLoader.getCoverageLines(sonar, resourceKey);
    } catch (final Exception e) {
      return Collections.emptyList();
    }
  }

  @Override
  protected Integer getLine(final CoverageLine coverage) {
    return coverage.getLine();
  }

  @Override
  protected String getMessage(final CoverageLine coverage) {
    // TODO jérémie : improve message and so on
    return "Coverage " + coverage.getHits() + ":" + coverage.getBranchHits();
  }

  @Override
  protected Integer getPriority(final CoverageLine Coverage) {
    return new Integer(IMarker.PRIORITY_LOW);
  }

  @Override
  protected Integer getSeverity(final CoverageLine coverage) {
    final String hits = coverage.getHits();
    final String branchHits = coverage.getBranchHits();
    final boolean hasLineCoverage = (null != hits);
    final boolean hasBranchCoverage = (null != branchHits);
    final boolean lineIsCovered = (hasLineCoverage && Integer.parseInt(hits) > 0);
    final boolean branchIsCovered = (hasBranchCoverage && "100%".equals(branchHits));

    if (lineIsCovered) {
      if (branchIsCovered) {
        return new Integer(IMarker.SEVERITY_INFO);
      } else if (hasBranchCoverage) {
        return new Integer(IMarker.SEVERITY_WARNING);
      } else {
        return new Integer(IMarker.SEVERITY_INFO);
      }
    } else if (hasLineCoverage) {
      return new Integer(IMarker.SEVERITY_ERROR);
    }
    return new Integer( -1);
  }
}
