/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2018 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.util;

import java.util.function.Predicate;
import java.util.stream.Stream;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.texteditor.ITextEditor;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.markers.MarkerUtils;
import org.sonarlint.eclipse.core.internal.resources.ProjectsProviderUtils;
import org.sonarlint.eclipse.core.internal.utils.PreferencesUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.server.actions.JobUtils;
import org.sonarlint.eclipse.ui.internal.views.locations.IssueLocationsView;
import org.sonarsource.sonarlint.core.client.api.common.RuleKey;

import static org.sonarlint.eclipse.ui.internal.markers.ShowIssueFlowsMarkerResolver.ISSUE_FLOW_ANNOTATION_TYPE;

public class DeactivateRuleUtils {

  private DeactivateRuleUtils() {
    // Utility class
  }

  /**
   * Deactivate the rule associated with a marker.
   */
  public static void deactivateRule(IMarker marker) {
    RuleKey ruleKey = MarkerUtils.getRuleKey(marker);
    if (ruleKey == null) {
      return;
    }

    removeReportIssuesMarkers(ruleKey);
    removeAnnotations(marker);

    PreferencesUtils.excludeRule(ruleKey);
    Predicate<ISonarLintFile> filter = f -> !f.getProject().isBound();
    JobUtils.scheduleAnalysisOfOpenFiles((ISonarLintProject) null, TriggerType.EXCLUSION_CHANGE, filter);
  }

  private static void removeAnnotations(IMarker marker) {
    IEditorPart editorPart;
    try {
      IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
      editorPart = IDE.openEditor(page, marker);
    } catch (PartInitException e) {
      SonarLintLogger.get().error("Could not get IEditorPart to remove annotations for deactivated rule");
      return;
    }

    if (!(editorPart instanceof ITextEditor)) {
      return;
    }

    ITextEditor textEditor = (ITextEditor) editorPart;
    IssueLocationsView view = (IssueLocationsView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(IssueLocationsView.ID);
    if (view != null) {
      view.setShowAnnotations(false);
    }
    IEditorInput editorInput = textEditor.getEditorInput();
    IAnnotationModel annotationModel = textEditor.getDocumentProvider().getAnnotationModel(editorInput);

    annotationModel.getAnnotationIterator().forEachRemaining(a -> {
      if (ISSUE_FLOW_ANNOTATION_TYPE.equals(a.getType())) {
        annotationModel.removeAnnotation(a);
      }
    });
  }

  private static void removeReportIssuesMarkers(RuleKey ruleKey) {
    ProjectsProviderUtils.allProjects().stream()
      .filter(p -> p.isOpen() && !p.isBound())
      .forEach(p -> findReportMarkers(p)
        .filter(m -> ruleKey.equals(MarkerUtils.getRuleKey(m)))
        .forEach(m -> {
          try {
            m.delete();
          } catch (CoreException e) {
            SonarLintLogger.get().error("Could not delete marker for deactivated rule: " + ruleKey);
          }
        }));
  }

  private static Stream<IMarker> findReportMarkers(ISonarLintProject project) {
    try {
      IMarker[] markers = project.getResource().findMarkers(SonarLintCorePlugin.MARKER_REPORT_ID, false, IResource.DEPTH_INFINITE);
      return Stream.of(markers);
    } catch (CoreException e) {
      SonarLintLogger.get().error("Could not get report markers for project: " + project.getName());
      return Stream.empty();
    }
  }
}
