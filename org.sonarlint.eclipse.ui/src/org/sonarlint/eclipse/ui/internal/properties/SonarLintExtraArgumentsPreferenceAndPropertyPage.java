/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2016 SonarSource SA
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
import java.util.Iterator;
import java.util.List;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.dialogs.PropertyPage;
import org.sonarlint.eclipse.core.internal.PreferencesUtils;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProject;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProperty;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;
import org.sonarlint.eclipse.ui.internal.Messages;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;

/**
 * An abstract field editor that manages a list of input values.
 * The editor displays a list containing the values, buttons for
 * adding and removing values, and Up and Down buttons to adjust
 * the order of elements in the list.
 * <p>
 * Subclasses must implement the <code>parseString</code>,
 * <code>createList</code>, and <code>getNewInputObject</code>
 * framework methods.
 * </p>
 */
public class SonarLintExtraArgumentsPreferenceAndPropertyPage extends PropertyPage implements IWorkbenchPreferencePage {

  private static final String VALUE = "Value";

  private static final String PREFERENCE_ID = "org.sonarlint.eclipse.ui.properties.SonarExtraArgumentsPreferenceAndPropertyPage";

  /**
   * Label provider for templates.
   */
  private static class SonarPropertiesLabelProvider extends LabelProvider implements ITableLabelProvider {

    @Override
    public Image getColumnImage(Object element, int columnIndex) {
      return null;
    }

    @Override
    public String getColumnText(Object element, int columnIndex) {
      SonarLintProperty data = (SonarLintProperty) element;

      switch (columnIndex) {
        case 0:
          return data.getName();
        case 1:
          return data.getValue();
        default:
          return ""; //$NON-NLS-1$
      }
    }
  }

  /**
   * A content provider for the template preference page's table viewer.
   *
   * @since 3.0
   */
  private static class SonarPropertiesContentProvider implements IStructuredContentProvider {

    private java.util.List<SonarLintProperty> sonarProperties;

    @Override
    public Object[] getElements(Object input) {
      return sonarProperties.toArray();
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
      sonarProperties = (List<SonarLintProperty>) newInput;
    }

    @Override
    public void dispose() {
      sonarProperties = null;
    }

  }

  private java.util.List<SonarLintProperty> sonarProperties;

  /**
   * The Remove button.
   */
  private Button removeButton;

  /**
   * The Edit button.
   */
  private Button editButton;

  /**
   * The Up button.
   */
  private Button upButton;

  /**
   * The Down button.
   */
  private Button downButton;

  private TableViewer fTableViewer;

  public SonarLintExtraArgumentsPreferenceAndPropertyPage() {
    setTitle(Messages.SonarPreferencePage_label_extra_args);
  }

  @Override
  protected Control createContents(final Composite ancestor) {
    loadProperties();

    Composite parent = new Composite(ancestor, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    parent.setLayout(layout);

    if (!isGlobal()) {
      createLinkToGlobal(ancestor, parent);
    }

    Composite innerParent = new Composite(parent, SWT.NONE);
    GridLayout innerLayout = new GridLayout();
    innerLayout.numColumns = 2;
    innerLayout.marginHeight = 0;
    innerLayout.marginWidth = 0;
    innerParent.setLayout(innerLayout);
    GridData gd = new GridData(GridData.FILL_BOTH);
    gd.horizontalSpan = 2;
    innerParent.setLayoutData(gd);

    Composite tableComposite = new Composite(innerParent, SWT.NONE);
    GridData data = new GridData(GridData.FILL_BOTH);
    data.widthHint = 360;
    data.heightHint = convertHeightInCharsToPixels(10);
    tableComposite.setLayoutData(data);

    TableColumnLayout columnLayout = new TableColumnLayout();
    tableComposite.setLayout(columnLayout);
    Table table = new Table(tableComposite, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);

    table.setHeaderVisible(true);
    table.setLinesVisible(true);

    GC gc = new GC(getShell());
    gc.setFont(JFaceResources.getDialogFont());

    TableColumn propertyNameColumn = new TableColumn(table, SWT.NONE);
    propertyNameColumn.setText("Name");
    int minWidth = computeMinimumColumnWidth(gc, "Name");
    columnLayout.setColumnData(propertyNameColumn, new ColumnWeightData(1, minWidth, true));

    TableColumn propertyValueColumn = new TableColumn(table, SWT.NONE);
    propertyValueColumn.setText(VALUE);
    minWidth = computeMinimumColumnWidth(gc, VALUE);
    columnLayout.setColumnData(propertyValueColumn, new ColumnWeightData(1, minWidth, true));

    gc.dispose();

    fTableViewer = new TableViewer(table);
    fTableViewer.setLabelProvider(new SonarPropertiesLabelProvider());
    fTableViewer.setContentProvider(new SonarPropertiesContentProvider());

    fTableViewer.setComparator(null);

    fTableViewer.addDoubleClickListener(new IDoubleClickListener() {
      @Override
      public void doubleClick(DoubleClickEvent e) {
        edit();
      }
    });

    fTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
      @Override
      public void selectionChanged(SelectionChangedEvent e) {
        updateButtons();
      }
    });

    createButtons(innerParent);

    fTableViewer.setInput(sonarProperties);

    updateButtons();
    Dialog.applyDialogFont(parent);
    innerParent.layout();

    return parent;
  }

  private void createButtons(Composite innerParent) {
    GridLayout layout;
    Composite buttons = new Composite(innerParent, SWT.NONE);
    buttons.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
    layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    buttons.setLayout(layout);

    Button addButton = new Button(buttons, SWT.PUSH);
    addButton.setText("New...");
    addButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    addButton.addListener(SWT.Selection, new Listener() {
      @Override
      public void handleEvent(Event e) {
        add();
      }
    });

    editButton = new Button(buttons, SWT.PUSH);
    editButton.setText("Edit...");
    editButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    editButton.addListener(SWT.Selection, new Listener() {
      @Override
      public void handleEvent(Event e) {
        edit();
      }
    });

    removeButton = new Button(buttons, SWT.PUSH);
    removeButton.setText("Remove");
    removeButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    removeButton.addListener(SWT.Selection, new Listener() {
      @Override
      public void handleEvent(Event e) {
        remove();
      }
    });

    upButton = new Button(buttons, SWT.PUSH);
    upButton.setText("Up");
    upButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    upButton.addListener(SWT.Selection, new Listener() {
      @Override
      public void handleEvent(Event e) {
        upPressed();
      }
    });

    downButton = new Button(buttons, SWT.PUSH);
    downButton.setText("Down");
    downButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    downButton.addListener(SWT.Selection, new Listener() {
      @Override
      public void handleEvent(Event e) {
        downPressed();
      }
    });
  }

  private static void createLinkToGlobal(final Composite ancestor, Composite parent) {
    Link fLink = new Link(parent, SWT.NONE);
    fLink.setText("<A>Configure Workspace Settings...</A>");
    fLink.setLayoutData(new GridData());
    SelectionAdapter sl = new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        PreferencesUtil.createPreferenceDialogOn(ancestor.getShell(), PREFERENCE_ID, null, null).open();
      }
    };
    fLink.addSelectionListener(sl);
  }

  private void edit() {
    IStructuredSelection selection = (IStructuredSelection) fTableViewer.getSelection();

    Object[] objects = selection.toArray();
    if ((objects == null) || (objects.length != 1)) {
      return;
    }

    SonarLintProperty data = (SonarLintProperty) selection.getFirstElement();
    edit(data);
  }

  private void add() {
    SonarLintProperty newProperty = editSonarProperty(new SonarLintProperty("", ""), false, true);
    if (newProperty != null) {
      sonarProperties.add(newProperty);
      fTableViewer.refresh();
      fTableViewer.setSelection(new StructuredSelection(newProperty));
    }
  }

  private void remove() {
    IStructuredSelection selection = (IStructuredSelection) fTableViewer.getSelection();

    Iterator elements = selection.iterator();
    while (elements.hasNext()) {
      SonarLintProperty data = (SonarLintProperty) elements.next();
      sonarProperties.remove(data);
    }

    fTableViewer.refresh();
  }

  private void edit(SonarLintProperty data) {
    SonarLintProperty oldProp = data;
    SonarLintProperty newProp = editSonarProperty(new SonarLintProperty(oldProp), true, false);
    if (newProp != null) {
      data.setValue(newProp.getValue());
      fTableViewer.refresh(data);
      updateButtons();
      fTableViewer.setSelection(new StructuredSelection(data));
    }
  }

  /**
   * Updates the buttons.
   */
  protected void updateButtons() {
    IStructuredSelection selection = (IStructuredSelection) fTableViewer.getSelection();
    int selectionCount = selection.size();
    int itemCount = fTableViewer.getTable().getItemCount();

    editButton.setEnabled(selectionCount == 1);
    removeButton.setEnabled(selectionCount > 0 && selectionCount <= itemCount);

    int index = sonarProperties.indexOf(selection.getFirstElement());

    removeButton.setEnabled(index >= 0);
    upButton.setEnabled(itemCount > 1 && index > 0);
    downButton.setEnabled(itemCount > 1 && index >= 0 && index < itemCount - 1);
  }

  /**
   * Creates the edit dialog. Subclasses may override this method to provide a
   * custom dialog.
   *
   * @param property the property being edited
   * @param edit whether this is a new property or an existing being edited
   * @param isNameModifiable whether the property name may be modified
   * @return the created or modified property, or <code>null</code> if the edition failed
   */
  protected SonarLintProperty editSonarProperty(SonarLintProperty property, boolean edit, boolean isNameModifiable) {
    EditSonarPropertyDialog dialog = new EditSonarPropertyDialog(getShell(), property, edit, isNameModifiable);
    if (dialog.open() == Window.OK) {
      return dialog.getSonarProperty();
    }
    return null;
  }

  private static int computeMinimumColumnWidth(GC gc, String string) {
    // pad 10 to accommodate table header trimmings
    return gc.stringExtent(string).x + 10;
  }

  /**
   * Notifies that the Down button has been pressed.
   */
  private void downPressed() {
    swap(false);
  }

  /**
   * Moves the currently selected item up or down.
   *
   * @param up <code>true</code> if the item should move up,
   *  and <code>false</code> if it should move down
   */
  private void swap(boolean up) {
    IStructuredSelection selection = (IStructuredSelection) fTableViewer.getSelection();

    Object[] objects = selection.toArray();
    if ((objects == null) || (objects.length != 1)) {
      return;
    }

    SonarLintProperty data = (SonarLintProperty) selection.getFirstElement();

    int index = sonarProperties.indexOf(data);
    int target = up ? (index - 1) : (index + 1);

    if (index >= 0) {
      sonarProperties.remove(index);
      sonarProperties.add(target, data);
      fTableViewer.setSelection(new StructuredSelection(data));
      fTableViewer.refresh();
      updateButtons();
    }
  }

  /**
   * Notifies that the Up button has been pressed.
   */
  private void upPressed() {
    swap(true);
  }

  @Override
  public void init(IWorkbench workbench) {
    setDescription("Additional properties passed to SonarLint analyzers");
    setPreferenceStore(SonarLintUiPlugin.getDefault().getPreferenceStore());
  }

  private void loadProperties() {
    sonarProperties = new ArrayList<>();
    if (isGlobal()) {
      String props = getPreferenceStore().getString(PreferencesUtils.PREF_EXTRA_ARGS);
      try {
        String[] keyValuePairs = StringUtils.split(props, "\r\n");
        for (String keyValuePair : keyValuePairs) {
          String[] keyValue = StringUtils.split(keyValuePair, "=");
          sonarProperties.add(new SonarLintProperty(keyValue[0], keyValue[1]));
        }
      } catch (Exception e) {
        SonarLintCorePlugin.getDefault().error("Error while loading SonarLint analyzer properties" + props, e);
      }
    } else {
      sonarProperties.addAll(getSonarProject().getExtraProperties());
    }
  }

  @Override
  public boolean performOk() {
    List<String> keyValuePairs = new ArrayList<>(sonarProperties.size());
    for (SonarLintProperty prop : sonarProperties) {
      keyValuePairs.add(prop.getName() + "=" + prop.getValue());
    }
    String props = StringUtils.joinSkipNull(keyValuePairs, "\r\n");
    if (isGlobal()) {
      getPreferenceStore().setValue(PreferencesUtils.PREF_EXTRA_ARGS, props);
    } else {
      SonarLintProject sonarProject = getSonarProject();
      sonarProject.setExtraProperties(sonarProperties);
      sonarProject.save();
    }
    return true;
  }

  @Override
  protected void performDefaults() {
    sonarProperties.clear();
    fTableViewer.refresh();
  }

  private IProject getProject() {
    return (IProject) getElement().getAdapter(IProject.class);
  }

  private SonarLintProject getSonarProject() {
    IProject project = getProject();
    if (project != null) {
      return SonarLintProject.getInstance(project);
    }
    return null;
  }

  private boolean isGlobal() {
    return getElement() == null;
  }

}
