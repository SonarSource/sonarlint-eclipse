/*
 * Sonar Eclipse
 * Copyright (C) 2010-2013 SonarSource
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
package org.sonar.ide.eclipse.ui.internal.markers;

import org.apache.commons.lang.ObjectUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.PlatformUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ide.eclipse.core.internal.markers.MarkerUtils;
import org.sonar.ide.eclipse.ui.ISonarResolver;
import org.sonar.ide.eclipse.ui.internal.Messages;
import org.sonar.ide.eclipse.ui.internal.views.IssueEditorWebView;

import java.text.MessageFormat;

/**
 * @author Jérémie Lagarde
 */
public class EditIssueMarkerResolver implements ISonarResolver {

  private static final Logger LOG = LoggerFactory.getLogger(EditIssueMarkerResolver.class);

  private String label;
  private String description;

  public boolean canResolve(final IMarker marker) {
    try {
      final Object ruleName = marker.getAttribute(MarkerUtils.SONAR_MARKER_RULE_NAME_ATTR);
      label = MessageFormat.format(Messages.EditIssueMarkerResolver_label, ruleName);
      description = Messages.EditIssueMarkerResolver_description;
      final Object issueId = marker.getAttribute(MarkerUtils.SONAR_MARKER_ISSUE_ID_ATTR);
      final boolean isNew = Boolean.TRUE.equals(marker.getAttribute(MarkerUtils.SONAR_MARKER_IS_NEW_ATTR));
      return issueId != null && !isNew;
    } catch (final CoreException e) {
      return false;
    }
  }

  public String getDescription() {
    return description;
  }

  public String getLabel() {
    return label;
  }

  public boolean resolve(final IMarker marker, final IFile cu) {
    try {

      final String issueId = ObjectUtils.toString(marker.getAttribute(MarkerUtils.SONAR_MARKER_ISSUE_ID_ATTR));
      IssueEditorWebView view = (IssueEditorWebView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(IssueEditorWebView.ID);
      view.open(issueId, marker.getResource());
    } catch (Exception e) {
      LOG.error("Unable to open Issue Editor Web View", e);
    }
    return false;
  }
}
