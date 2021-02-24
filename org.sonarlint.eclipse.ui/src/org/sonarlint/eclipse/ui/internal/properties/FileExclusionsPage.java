/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2021 SonarSource SA
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

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.adapter.Adapters;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintGlobalConfiguration;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintProjectConfiguration;
import org.sonarlint.eclipse.core.internal.resources.ExclusionItem;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;
import org.sonarlint.eclipse.ui.internal.binding.actions.JobUtils;

public class FileExclusionsPage extends AbstractListPropertyPage implements IWorkbenchPreferencePage {
  private static final String PREFERENCE_ID = "org.sonarlint.eclipse.ui.properties.FileExclusionsPage";
  private static final Image FILE_IMG = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE);
  private static final Image FOLDER_IMG = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
  private static final Image PATTERN_IMG = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ELEMENT);

  private List<ExclusionItem> exclusions = new ArrayList<>();
  private TableViewer table;
  private Shell shell;

  public FileExclusionsPage() {
    setTitle("File Exclusions");
  }

  @Override
  public void init(IWorkbench workbench) {
    setDescription("Configure files to be excluded from SonarLint analysis");
    setPreferenceStore(SonarLintUiPlugin.getDefault().getPreferenceStore());
  }

  @Override
  protected Control createContents(Composite parent) {
    this.exclusions = loadExclusions();
    this.shell = parent.getShell();

    // define container & its layout
    Font font = parent.getFont();
    Composite pageComponent = new Composite(parent, SWT.NULL);
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    pageComponent.setLayout(layout);

    if (!isGlobal()) {
      createLinkToGlobal(parent, pageComponent);
    }

    GridData data = new GridData(GridData.FILL_BOTH);
    pageComponent.setLayoutData(data);
    pageComponent.setFont(font);

    // layout the table & its buttons
    int tableStyle = SWT.BORDER | SWT.FULL_SELECTION;

    Composite tableComposite = new Composite(pageComponent, SWT.NONE);
    data = new GridData(SWT.FILL, SWT.FILL, true, true);
    data.grabExcessHorizontalSpace = true;
    data.grabExcessVerticalSpace = true;
    tableComposite.setLayoutData(data);

    table = new TableViewer(tableComposite, tableStyle);
    table.getTable().setFont(font);
    ColumnViewerToolTipSupport.enableFor(table, ToolTip.NO_RECREATE);

    TableViewerColumn typeColumn = new TableViewerColumn(table, SWT.NONE);
    typeColumn.setLabelProvider(new TypeLabelProvider());
    typeColumn.getColumn().setText("Type");

    TableViewerColumn valueColumn = new TableViewerColumn(table, SWT.NONE);
    valueColumn.setLabelProvider(new ValueLabelProvider());
    valueColumn.getColumn().setText("Value");

    TableColumnLayout tableLayout = new TableColumnLayout();
    tableComposite.setLayout(tableLayout);

    tableLayout.setColumnData(typeColumn.getColumn(), new ColumnWeightData(150));
    tableLayout.setColumnData(valueColumn.getColumn(), new ColumnWeightData(280));

    table.getTable().setHeaderVisible(true);
    data = new GridData(GridData.FILL_BOTH);
    data.heightHint = table.getTable().getItemHeight() * 7;
    table.getTable().setLayoutData(data);
    table.getTable().setFont(font);

    table.getTable().setToolTipText(null);
    table.setContentProvider(new ContentProvider());
    table.setInput(exclusions);

    createButtons(pageComponent);
    updateButtons();

    // CALLBACKS
    table.getTable().addMouseListener(new MouseAdapter() {
      @Override
      public void mouseDoubleClick(MouseEvent e) {
        int itemsSelectedCount = table.getTable().getSelectionCount();
        if (itemsSelectedCount == 1) {
          edit();
        }
      }
    });

    table.addSelectionChangedListener(e -> updateButtons());
    return pageComponent;
  }

  @Override
  protected TableViewer getTableViewer() {
    return table;
  }

  @Override
  protected void add() {
    EditExclusionDialog dialog;

    if (isGlobal()) {
      dialog = new EditGlobalExclusionDialog(shell, null);
    } else {
      dialog = new EditProjectExclusionDialog(getProject(), shell, null);
    }
    // opens the dialog - just returns if the user cancels it
    if (dialog.open() == Window.CANCEL) {
      return;
    }

    ExclusionItem newExclusion = dialog.get();
    if (newExclusion != null) {
      exclusions.add(newExclusion);
    }
    table.refresh();
  }

  @Override
  protected void edit() {
    ExclusionItem exclusion = (ExclusionItem) table.getStructuredSelection().getFirstElement();
    EditExclusionDialog dialog;

    if (isGlobal()) {
      dialog = new EditGlobalExclusionDialog(shell, exclusion.item());
    } else {
      dialog = new EditProjectExclusionDialog(getProject(), shell, exclusion);
    }

    // opens the dialog - just returns if the user cancels it
    if (dialog.open() == Window.CANCEL) {
      return;
    }

    ExclusionItem newExclusion = dialog.get();
    if (newExclusion != null) {
      int index = exclusions.indexOf(exclusion);
      exclusions.set(index, newExclusion);
      table.setSelection(new StructuredSelection(newExclusion));
      table.refresh();
    }
  }

  @Override
  protected void remove(Object item) {
    exclusions.remove(item);
  }

  @Override
  protected void removeSelection() {
    super.removeSelection();
    updateButtons();
  }

  protected void updateButtons() {
    IStructuredSelection selection = (IStructuredSelection) table.getSelection();
    int selectionCount = selection.size();
    int index = exclusions.indexOf(selection.getFirstElement());

    editButton.setEnabled(selectionCount == 1);
    removeButton.setEnabled(index >= 0);
  }

  private List<ExclusionItem> loadExclusions() {
    if (isGlobal()) {
      String props = getPreferenceStore().getString(SonarLintGlobalConfiguration.PREF_FILE_EXCLUSIONS);
      return SonarLintGlobalConfiguration.deserializeFileExclusions(props);
    } else {
      SonarLintProjectConfiguration sonarProject = getProjectConfig();
      if (sonarProject != null) {
        return new ArrayList<>(sonarProject.getFileExclusions());
      }
    }

    return new ArrayList<>();
  }

  private static void createLinkToGlobal(final Composite ancestor, Composite parent) {
    Link fLink = new Link(parent, SWT.NONE);
    fLink.setText("<A>Configure Workspace Settings...</A>");
    GridData gridData = new GridData();
    gridData.horizontalSpan = 2;
    fLink.setLayoutData(gridData);
    SelectionAdapter sl = new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        PreferencesUtil.createPreferenceDialogOn(ancestor.getShell(), PREFERENCE_ID, null, null).open();
      }
    };
    fLink.addSelectionListener(sl);
  }

  @Override
  public boolean performOk() {
    if (isGlobal()) {
      String serialized = SonarLintGlobalConfiguration.serializeFileExclusions(this.exclusions);
      getPreferenceStore().setValue(SonarLintGlobalConfiguration.PREF_FILE_EXCLUSIONS, serialized);
      JobUtils.scheduleAnalysisOfOpenFiles((ISonarLintProject) null, TriggerType.STANDALONE_CONFIG_CHANGE);
    } else {
      SonarLintProjectConfiguration projectConfig = getProjectConfig();
      projectConfig.getFileExclusions().clear();
      projectConfig.getFileExclusions().addAll(exclusions);
      SonarLintCorePlugin.saveConfig(getProject(), projectConfig);
      JobUtils.scheduleAnalysisOfOpenFiles(getProject(), TriggerType.STANDALONE_CONFIG_CHANGE);
    }

    return true;
  }

  @Nullable
  private SonarLintProjectConfiguration getProjectConfig() {
    ISonarLintProject project = getProject();
    if (project != null) {
      return SonarLintCorePlugin.loadConfig(project);
    }
    return null;
  }

  @Nullable
  private ISonarLintProject getProject() {
    return Adapters.adapt(getElement(), ISonarLintProject.class);
  }

  @Override
  protected void performDefaults() {
    this.exclusions = new ArrayList<>();
    table.refresh();
  }

  private boolean isGlobal() {
    return getElement() == null;
  }

  private class ContentProvider implements IStructuredContentProvider {
    @Override
    public Object[] getElements(Object inputElement) {
      return exclusions.toArray();
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      // nothing to do; seems to be required by e45 and older
    }

    @Override
    public void dispose() {
      // nothing to do; seems to be required by e45 and older
    }
  }

  private static class TypeLabelProvider extends CellLabelProvider {
    @Override
    public void update(ViewerCell cell) {
      ExclusionItem exclusion = (ExclusionItem) cell.getElement();
      cell.setText(exclusion.type().toString());
      if (exclusion.type().equals(ExclusionItem.Type.FILE)) {
        cell.setImage(FILE_IMG);
      } else if (exclusion.type().equals(ExclusionItem.Type.DIRECTORY)) {
        cell.setImage(FOLDER_IMG);
      } else {
        cell.setImage(PATTERN_IMG);
      }
    }
  }

  private static class ValueLabelProvider extends CellLabelProvider {
    @Override
    public String getToolTipText(Object element) {
      ExclusionItem exclusion = (ExclusionItem) element;
      return exclusion.item();
    }

    @Override
    public void update(ViewerCell cell) {
      ExclusionItem exclusion = (ExclusionItem) cell.getElement();
      cell.setText(exclusion.item());
    }
  }
}
