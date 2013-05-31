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
package org.sonar.ide.eclipse.ui.internal.views;

import com.google.common.collect.Lists;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;
import org.sonar.ide.eclipse.core.internal.markers.MarkerUtils;
import org.sonar.ide.eclipse.core.internal.resources.SonarProject;
import org.sonar.ide.eclipse.ui.internal.ISonarConstants;
import org.sonar.ide.eclipse.ui.internal.SonarUrls;

import java.util.List;

/**
 * Display details of a rule in a web browser
 */
public class RuleDescriptionWebView extends AbstractLinkedSonarWebView<IMarker> implements ISelectionListener {

  private static final Logger LOG = LoggerFactory.getLogger(RuleDescriptionWebView.class);

  public static final String ID = ISonarConstants.PLUGIN_ID + ".views.RuleDescriptionWebView";

  @Override
  protected void open(IMarker element) {
    SonarProject sonarProject = SonarProject.getInstance(element.getResource());
    try {
      String url = new SonarUrls().ruleDescriptionUrl("" + element.getAttribute(MarkerUtils.SONAR_MARKER_RULE_KEY_ATTR), sonarProject.getUrl());
      super.open(sonarProject, url);
    } catch (CoreException e) {
      LOG.error("Unable to open rule description", e);
    }
  }

  @Override
  protected IMarker findSelectedElement(IWorkbenchPart part, ISelection selection) {
    try {
      if (selection instanceof IStructuredSelection) {
        List<IMarker> selectedSonarMarkers = Lists.newArrayList();

        @SuppressWarnings("rawtypes")
        List elems = ((IStructuredSelection) selection).toList();
        for (Object elem : elems) {
          if (elem instanceof IMarker) {
            IMarker marker = (IMarker) elem;
            if (SonarCorePlugin.MARKER_ID.equals(marker.getType()) || SonarCorePlugin.NEW_ISSUE_MARKER_ID.equals(marker.getType())) {
              selectedSonarMarkers.add(marker);
            }
          }
          else if (elem instanceof IAdaptable) {
            IMarker marker = (IMarker) ((IAdaptable) elem).getAdapter(IMarker.class);
            if (marker != null && (SonarCorePlugin.MARKER_ID.equals(marker.getType()) || SonarCorePlugin.NEW_ISSUE_MARKER_ID.equals(marker.getType()))) {
              selectedSonarMarkers.add(marker);
            }
          }
        }

        if (!selectedSonarMarkers.isEmpty()) {
          return selectedSonarMarkers.get(0);
        }
      }
    } catch (Exception e) {
      return null;
    }
    return null;
  }

}
