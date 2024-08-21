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
package org.sonarlint.eclipse.ui.internal.dialog;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
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

public class FixSuggestionDialog extends Dialog {
  @Nullable
  private final SonarLintLanguage language;
  private final String explanation;
  private final String textLeft;
  private final String textRight;
  private final CompareConfiguration mp;

  public FixSuggestionDialog(Shell parentShell, @Nullable SonarLintLanguage language, String explanation,
    String textLeft, String textRight) {
    super(parentShell);

    this.language = language;
    this.explanation = explanation;
    this.textLeft = textLeft;
    this.textRight = textRight;
    this.mp = new CompareConfiguration();
    mp.setLeftEditable(false);
    mp.setLeftLabel("Current code");
    mp.setRightEditable(false);
    mp.setRightLabel("Suggested code");
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    var container = (Composite) super.createDialogArea(parent);
    container.setLayout(new GridLayout(1, true));

    var label = new Label(container, SWT.WRAP);
    label.setText(explanation);
    label.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

    TextMergeViewer diffViewer;
    if (language == null) {
      diffViewer = new TextMergeViewer(container, mp);
    } else {
      diffViewer = getDiffViewer(container);
    }
    diffViewer.setInput(new DiffNode(new CodeNode(textLeft), new CodeNode(textRight)));
    var gridData = new GridData();
    gridData.horizontalAlignment = SWT.FILL;
    gridData.verticalAlignment = SWT.FILL;
    gridData.grabExcessHorizontalSpace = true;
    gridData.grabExcessVerticalSpace = true;
    diffViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    return container;
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, IDialogConstants.OK_ID, "Apply Changes", true);
    createButton(parent, IDialogConstants.CANCEL_ID, "Cancel", false);
    createButton(parent, IDialogConstants.SKIP_ID, "Decline Changes", false);
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

    newShell.setText("SonarLint Fix Suggestion");
    newShell.setSize(800, 400);
  }

  @Override
  protected boolean isResizable() {
    return true;
  }

  private TextMergeViewer getDiffViewer(Composite parent) {
    var fileLanguage = language.name().toLowerCase();

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
