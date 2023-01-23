/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2023 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.properties;

import java.nio.file.FileSystems;
import java.util.regex.PatternSyntaxException;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.sonarlint.eclipse.core.internal.resources.ExclusionItem;
import org.sonarlint.eclipse.core.internal.resources.ExclusionItem.Type;
import org.sonarlint.eclipse.core.internal.utils.SonarLintUtils;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintFile;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;

public class EditProjectExclusionDialog extends EditExclusionDialog {
  private final ISonarLintProject project;

  private Button fileRadio;
  private Button directoryRadio;
  private Button patternRadio;

  private Text fileField;
  private Text directoryField;
  private Text patternField;

  private Button fileButton;
  private Button directoryButton;

  private ExclusionItem editItem;
  private boolean initialized = false;

  /**
   * The standard message to be shown when there are no problems being reported.
   */
  private static final String STANDARD_MESSAGE = "Choose a type of exclusion and configure its value. "
    + "Paths should be relative to project base directory and specified with forward slashes. For example: path/to/file.";

  public EditProjectExclusionDialog(ISonarLintProject project, Shell parentShell, @Nullable ExclusionItem editItem) {
    super(parentShell);
    this.project = project;
    this.editing = (editItem != null);
    this.editItem = editItem;
    super.setHelpAvailable(false);
  }

  @Override
  public String standardMessage() {
    return STANDARD_MESSAGE;
  }

  /**
   * Creates and returns the contents of this dialog (except for the button bar).
   *
   * @see org.eclipse.jface.dialogs.TitleAreaDialog#createDialogArea
   */
  @Override
  protected Control createDialogArea(Composite parent) {
    // top level composite
    var parentComposite = (Composite) super.createDialogArea(parent);

    initializeDialogUnits(parentComposite);

    // creates dialog area composite
    var contents = createComposite(parentComposite, 3);

    // creates and lay outs dialog area widgets
    createWidgets(contents);

    if (editing) {
      switch (editItem.type()) {
        case FILE:
          fileRadio.setSelection(true);
          fileField.setText(editItem.item());
          break;
        case DIRECTORY:
          directoryRadio.setSelection(true);
          directoryField.setText(editItem.item());
          break;
        case GLOB:
          patternRadio.setSelection(true);
          patternField.setText(editItem.item());
          break;
      }
      validate();
    } else {
      fileRadio.setSelection(true);

    }
    onSelectType();
    Dialog.applyDialogFont(parentComposite);
    initialized = true;
    return contents;
  }

  /**
   * Creates widgets for this dialog.
   *
   * @param contents
   *            the parent composite where to create widgets
   */
  private void createWidgets(Composite contents) {
    fileRadio = new Button(contents, SWT.RADIO);
    fileRadio.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
    fileRadio.setText("File");
    fileField = new Text(contents, SWT.SINGLE | SWT.BORDER);
    fileField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    fileButton = new Button(contents, SWT.PUSH);
    fileButton.setText("Select..");
    setButtonLayoutData(fileButton);
    fileButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        selectFile();
      }
    });
    directoryRadio = new Button(contents, SWT.RADIO);
    directoryRadio.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
    directoryRadio.setText("Directory");
    directoryField = new Text(contents, SWT.SINGLE | SWT.BORDER);
    directoryField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    directoryButton = new Button(contents, SWT.PUSH);
    directoryButton.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
    directoryButton.setText("Select..");
    setButtonLayoutData(directoryButton);
    directoryButton.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        selectFolder();
      }
    });

    patternRadio = new Button(contents, SWT.RADIO);
    patternRadio.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
    patternRadio.setText("GLOB pattern");
    patternField = new Text(contents, SWT.SINGLE | SWT.BORDER);
    patternField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

    // callbacks
    var radioListener = new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        onSelectType();
      }
    };
    directoryRadio.addSelectionListener(radioListener);
    fileRadio.addSelectionListener(radioListener);
    patternRadio.addSelectionListener(radioListener);

    ModifyListener fieldListener = e -> onFieldChanged();
    directoryField.addModifyListener(fieldListener);
    fileField.addModifyListener(fieldListener);
    patternField.addModifyListener(fieldListener);
  }

  private void selectType(ExclusionItem.Type type) {
    fileField.setEnabled(false);
    fileButton.setEnabled(false);
    directoryField.setEnabled(false);
    directoryButton.setEnabled(false);
    patternField.setEnabled(false);

    switch (type) {
      case FILE:
        fileField.setEnabled(true);
        fileButton.setEnabled(true);
        break;
      case DIRECTORY:
        directoryField.setEnabled(true);
        directoryButton.setEnabled(true);
        break;
      case GLOB:
        patternField.setEnabled(true);
        break;
    }
  }

  private void onSelectType() {
    selectType(getCurrentItem().type());
  }

  private void onFieldChanged() {
    editItem = getCurrentItem();
    validate();
  }

  private ExclusionItem getCurrentItem() {
    if (directoryRadio.getSelection()) {
      return new ExclusionItem(Type.DIRECTORY, directoryField.getText());
    } else if (fileRadio.getSelection()) {
      return new ExclusionItem(Type.FILE, fileField.getText());
    } else {
      return new ExclusionItem(Type.GLOB, patternField.getText());
    }
  }

  private void validate() {
    if (!initialized) {
      return;
    }
    var validationMessage = STANDARD_MESSAGE;
    var validationStatus = IMessageProvider.NONE;

    if (StringUtils.isEmpty(editItem.item())) {
      validationMessage = "The field is empty";
      validationStatus = IMessageProvider.ERROR;
    } else if (editItem.type() != Type.GLOB) {
      if (!Path.EMPTY.isValidPath(editItem.item())) {
        validationMessage = "The field is empty";
        validationStatus = IMessageProvider.ERROR;
      } else if (new Path(editItem.item()).isAbsolute()) {
        validationMessage = "The path must be relative";
        validationStatus = IMessageProvider.ERROR;
      }
    } else {
      
      try {
        var fs = FileSystems.getDefault();
        fs.getPathMatcher("glob:" + editItem);
      } catch (PatternSyntaxException e) {
        validationMessage = "The pattern has an invalid syntax";
        validationStatus = IMessageProvider.ERROR;
      }
    }

    setMessage(validationMessage, validationStatus);
    okButton.setEnabled(validationStatus != IMessageProvider.ERROR);
  }

  /**
   * Opens a dialog where the user can select a folder path.
   */
  private void selectFolder() {
    ISelectionStatusValidator validator = arr -> {
      if (arr.length > 1) {
        return ValidationStatus.error("Only one folder can be selected");
      }
      if (arr.length == 0) {
        return ValidationStatus.ok();
      }
      var obj = arr[0];
      return (obj instanceof IFolder) ? ValidationStatus.ok() : ValidationStatus.error("Select a folder");
    };

    var viewFilter = new ViewerFilter() {
      @Override
      public boolean select(Viewer viewer, Object parentElement, Object element) {
        if (element instanceof IFolder) {
          IFolder folder = (IFolder) element;
          return SonarLintUtils.isSonarLintFileCandidate(folder);
        }
        return false;
      }
    };

    var lp = new WorkbenchLabelProvider();
    var cp = new WorkbenchContentProvider();
    var dialog = new ElementTreeSelectionDialog(getShell(), lp, cp);
    dialog.setTitle("Select Folder");
    dialog.setInput(project.getResource());
    dialog.setAllowMultiple(false);
    dialog.addFilter(viewFilter);
    dialog.setValidator(validator);
    dialog.setMessage("Select a project folder to be excluded from SonarLint analysis");

    if (editItem != null) {
      dialog.setInitialSelection(editItem.item());
    }

    if (dialog.open() == Window.OK) {
      var obj = dialog.getFirstResult();
      var folder = (IFolder) obj;
      if (folder != null) {
        editItem = new ExclusionItem(Type.DIRECTORY, folder.getProjectRelativePath().toString());
        directoryField.setText(editItem.item());
      }
    }
  }

  /**
   * Opens a dialog where the user can select a file path.
   */
  private void selectFile() {
    ISelectionStatusValidator validator = arr -> {
      if (arr.length > 1) {
        return ValidationStatus.error("Only one file can be selected");
      }
      if (arr.length == 0) {
        return ValidationStatus.ok();
      }
      Object obj = arr[0];
      ISonarLintFile file = Adapters.adapt(obj, ISonarLintFile.class);
      return file != null ? ValidationStatus.ok() : ValidationStatus.error("Select a file");
    };

    var viewFilter = new ViewerFilter() {
      @Override
      public boolean select(Viewer viewer, Object parentElement, Object element) {
        if (element instanceof IFolder) {
          var folder = (IFolder) element;
          return SonarLintUtils.isSonarLintFileCandidate(folder);
        }

        if (element instanceof IFile) {
          var file = Adapters.adapt(element, ISonarLintFile.class);
          return file != null;
        }
        return false;
      }
    };

    var lp = new WorkbenchLabelProvider();
    var cp = new WorkbenchContentProvider();
    var dialog = new ElementTreeSelectionDialog(getShell(), lp, cp);
    dialog.setTitle("Select File");
    dialog.setInput(project.getResource());
    dialog.setAllowMultiple(false);
    dialog.addFilter(viewFilter);
    dialog.setValidator(validator);
    dialog.setMessage("Select a project file to be excluded from SonarLint analysis");
    if (editItem != null) {
      dialog.setInitialSelection(editItem.item());
    }

    if (dialog.open() == Window.OK) {
      var obj = dialog.getFirstResult();
      var file = Adapters.adapt(obj, ISonarLintFile.class);
      if (file != null) {
        editItem = new ExclusionItem(Type.FILE, file.getProjectRelativePath());
        fileField.setText(editItem.item());
      }
    }
  }

  @Override
  public ExclusionItem get() {
    return editItem;
  }
}
