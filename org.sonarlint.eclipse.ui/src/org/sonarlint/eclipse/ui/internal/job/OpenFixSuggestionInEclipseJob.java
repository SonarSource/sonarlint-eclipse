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
package org.sonarlint.eclipse.ui.internal.job;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;
import org.sonarlint.eclipse.core.analysis.SonarLintLanguage;
import org.sonarlint.eclipse.core.internal.telemetry.SonarLintTelemetry;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.dialog.FixSuggestionDialog;
import org.sonarlint.eclipse.ui.internal.extension.SonarLintUiExtensionTracker;
import org.sonarlint.eclipse.ui.internal.util.PlatformUtils;
import org.sonarsource.sonarlint.core.rpc.protocol.client.fix.FixSuggestionDto;

/**
 *  "Open fix suggestion": For a specific file there are "one-to-many" suggestions coming to be displayed to the user
 *                         one after another. The behavior before the actual logic is similar to the one implemented
 *                         for the "Open in IDE" feature.
 */
public class OpenFixSuggestionInEclipseJob extends AbstractOpenInEclipseJob {
  //
  private static final int CHANGE_MARGIN = 15;

  private final FixSuggestionDto fixSuggestion;

  public OpenFixSuggestionInEclipseJob(FixSuggestionDto fixSuggestion, ISonarLintProject project) {
    super("Open fix suggestion in IDE", project, true);

    this.fixSuggestion = fixSuggestion;
  }

  @Override
  IStatus actualRun() throws CoreException {
    // Get all the static information that doesn't need to be calculated for every change
    var iFile = (IFile) file.getResource();
    var suggestionId = fixSuggestion.suggestionId();
    var explanation = fixSuggestion.explanation();

    var statusRef = new AtomicReference<IStatus>();
    statusRef.set(Status.OK_STATUS);
    var changeIndexRef = new AtomicInteger(0);

    Display.getDefault().syncExec(() -> {
      // Open the editor initially so we can access the document that we want to change
      var part = PlatformUtils.openEditor(iFile);
      if (!(part instanceof ITextEditor)) {
        statusRef.set(Status.CANCEL_STATUS);
        return;
      }
      var editor = (ITextEditor) part;

      // Get the language of the file opened inside the editor via the extension point in order to be able to access
      // the language/plug-in specific diff viewer in the dialog!
      SonarLintLanguage language = null;
      var configurationProviders = SonarLintUiExtensionTracker.getInstance().getSyntaxHighlightingProvider();
      for (var configurationProvider : configurationProviders) {
        language = configurationProvider.getEditorLanguage(editor);
        if (language != null) {
          break;
        }
      }

      // This is used for later replacing the content, we won't work on the document directly by calculating offsets
      // and so on and instead use the text and replace the content in there!
      var document = editor.getDocumentProvider().getDocument(editor.getEditorInput());

      // When a change was accepted, the number of lines change and therefore the position of the following changes. If
      // the user accepts a change, we have to calculate the created difference of the "before" and "after" so the next
      // change suggestion can still be found in the document!
      var difference = 0;

      // Iterate over every change
      for (var change : fixSuggestion.fileEdit().changes()) {
        var documentContent = document.get();
        var startLine = change.beforeLineRange().getStartLine();

        // i) Find the suggestion in the file, if it does not exist, continue to the next one
        var lineOfChange = StringUtils.getLineOfSubstring(documentContent, change.before());
        if (lineOfChange == -1
          || lineOfChange < startLine + difference - CHANGE_MARGIN
          || lineOfChange > startLine + difference + CHANGE_MARGIN) {
          continue;
        }

        PlatformUtils.openEditor(iFile, lineOfChange + 1);

        var dialog = new FixSuggestionDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
          language, explanation, change.before(), change.after());
        var result = dialog.open();

        changeIndexRef.set(fixSuggestion.fileEdit().changes().indexOf(change));
        if (result == IDialogConstants.CANCEL_ID) {
          statusRef.set(Status.CANCEL_STATUS);
          break;
        }

        if (result == IDialogConstants.SKIP_ID) {
          // Send telemetry about it being declined
          SonarLintTelemetry.handledFixSuggestion(suggestionId, changeIndexRef.get(), false);
          continue;
        }

        // Calculate the new difference
        difference = StringUtils.getNumberOfLines(change.before()) - StringUtils.getNumberOfLines(change.after());

        // Replace the document content
        document.set(documentContent.replace(change.before(), change.after()));

        // Send telemetry about it being accepted
        SonarLintTelemetry.handledFixSuggestion(suggestionId, changeIndexRef.get(), true);
      }
    });

    // When the user stopped the whole process at some point, then we have to inform the telemetry that all following
    // change suggestions (including the one where the user aborted) were declined!
    var status = statusRef.get();
    if (status == Status.CANCEL_STATUS) {
      for (var i = changeIndexRef.get(); i < fixSuggestion.fileEdit().changes().size(); i++) {
        SonarLintTelemetry.handledFixSuggestion(suggestionId, i, false);
      }
    }

    return statusRef.get();
  }

  @Override
  String getIdeFilePath() {
    return fixSuggestion.fileEdit().idePath().toString();
  }
}
