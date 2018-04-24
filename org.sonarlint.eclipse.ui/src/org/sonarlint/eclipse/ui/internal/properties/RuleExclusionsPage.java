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
package org.sonarlint.eclipse.ui.internal.properties;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.PropertyPage;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.TriggerType;
import org.sonarlint.eclipse.core.internal.jobs.StandaloneSonarLintClientFacade;
import org.sonarlint.eclipse.core.internal.utils.PreferencesUtils;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;
import org.sonarlint.eclipse.ui.internal.server.actions.JobUtils;
import org.sonarsource.sonarlint.core.client.api.common.RuleDetails;
import org.sonarsource.sonarlint.core.client.api.common.RuleKey;

public class RuleExclusionsPage extends PropertyPage implements IWorkbenchPreferencePage {

  private Button removeButton;

  private final List<RuleExclusionItem> excludedRules = new ArrayList<>();

  private TableViewer table;

  public RuleExclusionsPage() {
    setTitle("Rules Configuration");
  }

  @Override
  public void init(IWorkbench workbench) {
    setDescription("Configure rules to be excluded from SonarLint analysis. When a project is connected to a SonarQube server, configuration from the server apply.");
    setPreferenceStore(SonarLintUiPlugin.getDefault().getPreferenceStore());
  }

  @Override
  protected Control createContents(Composite parent) {
    loadExcludedRules();

    // define container & its layout
    Font font = parent.getFont();
    Composite pageComponent = new Composite(parent, SWT.NULL);
    GridLayout layout = new GridLayout();
    layout.numColumns = 2;
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    pageComponent.setLayout(layout);

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

    TableViewerColumn ruleKeyColumn = new TableViewerColumn(table, SWT.NONE);
    ruleKeyColumn.setLabelProvider(new RuleKeyLabelProvider());
    ruleKeyColumn.getColumn().setText("Rule key");

    TableViewerColumn ruleNameColumn = new TableViewerColumn(table, SWT.NONE);
    ruleNameColumn.setLabelProvider(new RuleNameLabelProvider());
    ruleNameColumn.getColumn().setText("Rule name");

    TableColumnLayout tableLayout = new TableColumnLayout();
    tableComposite.setLayout(tableLayout);

    tableLayout.setColumnData(ruleKeyColumn.getColumn(), new ColumnWeightData(150));
    tableLayout.setColumnData(ruleNameColumn.getColumn(), new ColumnWeightData(280));

    table.getTable().setHeaderVisible(true);
    data = new GridData(GridData.FILL_BOTH);
    data.heightHint = table.getTable().getItemHeight() * 7;
    table.getTable().setLayoutData(data);
    table.getTable().setFont(font);

    table.getTable().setToolTipText(null);
    table.setContentProvider(new ContentProvider());
    table.setInput(excludedRules);

    createButtons(pageComponent);
    updateButtons();

    table.addSelectionChangedListener(e -> updateButtons());
    return pageComponent;
  }

  private void remove() {
    AbstractListPropertyPage.removeSelection(table, excludedRules::remove);
    updateButtons();
  }

  protected void createButtons(Composite innerParent) {
    Composite buttons = new Composite(innerParent, SWT.NONE);
    buttons.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
    GridLayout layout = new GridLayout();
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    buttons.setLayout(layout);

    removeButton = new Button(buttons, SWT.PUSH);
    removeButton.setText("Remove");
    removeButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    removeButton.addListener(SWT.Selection, e -> remove());
  }

  protected void updateButtons() {
    IStructuredSelection selection = (IStructuredSelection) table.getSelection();
    int index = excludedRules.indexOf(selection.getFirstElement());

    removeButton.setEnabled(index >= 0);
  }

  private void loadExcludedRules() {
    StandaloneSonarLintClientFacade standaloneEngine = SonarLintCorePlugin.getInstance().getDefaultSonarLintClientFacade();
    this.excludedRules.clear();
    PreferencesUtils.getExcludedRules()
      .stream()
      .sorted(Comparator.comparing(RuleKey::repository).thenComparing(RuleKey::rule))
      .forEach(ruleKey -> {
        String ruleName;
        try {
          RuleDetails ruleDetails = standaloneEngine.getRuleDescription(ruleKey.toString());
          ruleName = ruleDetails != null ? ruleDetails.getName() : "(unknown)";
        } catch (IllegalArgumentException e) {
          // no such rule: probably it was removed in new version of the plugin
          ruleName = "(unavailable)";
        }
        excludedRules.add(new RuleExclusionItem(ruleKey, ruleName));
      });
  }

  @Override
  public boolean performOk() {
    PreferencesUtils.setExcludedRules(this.excludedRules.stream().map(item -> item.ruleKey).collect(Collectors.toList()));
    JobUtils.scheduleAnalysisOfOpenFiles((ISonarLintProject) null, TriggerType.EXCLUSION_CHANGE);
    return true;
  }

  @Override
  protected void performDefaults() {
    this.excludedRules.clear();
    this.table.refresh();
  }

  private static class RuleExclusionItem {
    private final RuleKey ruleKey;
    private final String ruleName;

    private RuleExclusionItem(RuleKey ruleKey, String ruleName) {
      this.ruleKey = ruleKey;
      this.ruleName = ruleName;
    }
  }

  private class ContentProvider implements IStructuredContentProvider {
    @Override
    public Object[] getElements(Object inputElement) {
      return excludedRules.toArray();
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

  private static class RuleKeyLabelProvider extends CellLabelProvider {
    @Override
    public void update(ViewerCell cell) {
      RuleExclusionItem item = (RuleExclusionItem) cell.getElement();
      cell.setText(item.ruleKey.toString());
    }
  }

  private static class RuleNameLabelProvider extends CellLabelProvider {
    @Override
    public void update(ViewerCell cell) {
      RuleExclusionItem item = (RuleExclusionItem) cell.getElement();
      cell.setText(item.ruleName);
    }
  }
}
