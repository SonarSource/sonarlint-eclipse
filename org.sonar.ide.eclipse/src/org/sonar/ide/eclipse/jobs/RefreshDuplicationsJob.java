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
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.eclipse.internal.EclipseSonar;
import org.sonar.ide.shared.duplications.Duplication;

/**
 * This class load duplications in background.
 * 
 * @author Evgeny Mandrikov
 */
public class RefreshDuplicationsJob extends AbstractRefreshModelJob<Duplication> {

  public RefreshDuplicationsJob(final List<IResource> resources) {
    super(resources, SonarPlugin.MARKER_DUPLICATION_ID);
  }

  @Override
  protected Collection<Duplication> retrieveDatas(EclipseSonar sonar, IResource resource) {
    return sonar.search(resource).getDuplications();
  }

  @Override
  protected Integer getLine(final Duplication duplication) {
    return duplication.getStart();
  }

  @Override
  protected String getMessage(final Duplication duplication) {
    // TODO Godin : improve message and so on
    return "Duplicates code from " + duplication.getTargetResource() + ":" + duplication.getTargetStart();
  }

  @Override
  protected Integer getPriority(final Duplication duplication) {
    return new Integer(IMarker.PRIORITY_LOW);
  }

  @Override
  protected Integer getSeverity(final Duplication duplication) {
    return new Integer(IMarker.SEVERITY_WARNING);
  }
}
