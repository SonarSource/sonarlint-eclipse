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
package org.sonarlint.eclipse.ui.internal.dialog;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Locale;
import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;
import org.eclipse.compare.internal.Utilities;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.sonarlint.eclipse.core.analysis.SonarLintLanguage;
import org.sonarlint.eclipse.ui.internal.extension.SonarLintUiExtensionTracker;

public abstract class AbstractFixSuggestionDialog extends Dialog {
  @Nullable
  private final SonarLintLanguage language;
  private final String explanation;
  private final String beforeText;
  private final String afterText;
  private final int snippetIndex;
  private final int absoluteNumberOfChanges;
  private final CompareConfiguration mp;

  /**
   *  @param parentShell used for the dialog
   *  @param language used for determining the correct diff viewer for the language
   *  @param explanation shown about the suggestion to explain the diff
   *  @param beforeText what will be replaced
   *  @param afterText with what it will be replaced
   *  @param snippetIndex the number of the index (-1)
   *  @param absoluteNumberOfChanges all number of changes
   */
  protected AbstractFixSuggestionDialog(Shell parentShell, @Nullable SonarLintLanguage language, String explanation,
    String beforeText, String afterText, int snippetIndex, int absoluteNumberOfChanges) {
    super(parentShell);

    this.language = language;
    this.explanation = explanation;
    this.beforeText = beforeText;
    this.afterText = afterText;
    this.snippetIndex = snippetIndex;
    this.absoluteNumberOfChanges = absoluteNumberOfChanges;
    this.mp = new CompareConfiguration();
    mp.setProperty(CompareConfiguration.MIRRORED, true);
    mp.setLeftEditable(false);
    mp.setLeftLabel("Suggested code");
    mp.setRightEditable(false);
    mp.setRightLabel("Current code on the server");
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    var container = (Composite) super.createDialogArea(parent);
    container.setLayout(new GridLayout(1, true));

    // So the sub-classes can add a specific label with situational information!
    addLabel(container);

    var label = new Label(container, SWT.WRAP);
    label.setText(explanation);
    label.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

    TextMergeViewer diffViewer;
    if (language == null) {
      diffViewer = new TextMergeViewer(container, mp);
    } else {
      diffViewer = getDiffViewer(container);
    }
    // INFO: Because we mirrored the diff viewer the right becomes left and the left becomes right
    diffViewer.setInput(new DiffNode(new CodeNode(afterText), new CodeNode(beforeText)));
    diffViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    return container;
  }

  protected void addLabel(Composite container) {
    // Should be overwritten by sub-classes that provide situational information!
  }

  /** Because the "skip" button is not implemented by default, we have to do it on our own! */
  @Override
  protected void buttonPressed(int buttonId) {
    if (buttonId == IDialogConstants.SKIP_ID) {
      setReturnCode(IDialogConstants.SKIP_ID);
      close();
    }
    super.buttonPressed(buttonId);
  }

  @Override
  protected void configureShell(Shell newShell) {
    super.configureShell(newShell);

    newShell.setText(String.format("SonarQube Fix Suggestion (%d/%d)", snippetIndex + 1, absoluteNumberOfChanges));
    newShell.setSize(1000, 400);
  }

  @Override
  protected boolean isResizable() {
    return true;
  }

  private TextMergeViewer getDiffViewer(Composite parent) {
    var fileLanguage = language.name().toLowerCase(Locale.getDefault());

    var configurationProviders = SonarLintUiExtensionTracker.getInstance().getSyntaxHighlightingProvider();
    for (var configurationProvider : configurationProviders) {
      var diffViewerNullable = configurationProvider.getTextMergeViewer(fileLanguage, parent, mp);
      if (diffViewerNullable != null) {
        return diffViewerNullable;
      }
    }

    // If no plug-in provides a language-specific diff viewer that is also pre-configured with syntax highlighting, we
    // just provide a "normal" text merge viewer instance!
    return new TextMergeViewer(parent, mp);
  }

  /**
   *  The TextMergeViewer works with distinct nodes for each side, we have to implement it ourself as there is no
   *  common implementation that can be reused. It is basically fancy wrapper for a string -.-
   */
  private static class CodeNode implements ITypedElement, IEncodedStreamContentAccessor {
    private final String content;

    public CodeNode(String content) {
      this.content = content;
    }

    @Override
    public InputStream getContents() throws CoreException {
      return new ByteArrayInputStream(Utilities.getBytes(content, "UTF-8"));
    }

    @Override
    public String getCharset() throws CoreException {
      return "UTF-8";
    }

    @Override
    public String getName() {
      return "no name";
    }

    @Override
    public Image getImage() {
      return null;
    }

    @Override
    public String getType() {
      return "no type";
    }
  }
}
