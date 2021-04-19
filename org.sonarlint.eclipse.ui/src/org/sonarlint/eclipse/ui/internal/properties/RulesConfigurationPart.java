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
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeNodeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.sonarlint.eclipse.core.internal.preferences.RuleConfig;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.util.PlatformUtils;
import org.sonarlint.eclipse.ui.internal.util.SonarLintRuleBrowser;
import org.sonarsource.sonarlint.core.client.api.common.Language;
import org.sonarsource.sonarlint.core.client.api.common.RuleDetails;
import org.sonarsource.sonarlint.core.client.api.standalone.StandaloneRuleDetails;

// Inspired by: http://www.vogella.com/tutorials/EclipseJFaceTree/article.html
public class RulesConfigurationPart {

  private final Map<Language, List<RuleDetailsWrapper>> ruleDetailsWrappersByLanguage;
  private final Map<String, RuleConfig> initialRuleConfigs;

  private final RuleDetailsWrapperFilter filter;
  private SonarLintRuleBrowser ruleBrowser;
  private CheckBoxFilteredTree tree;
  private Composite paramPanelParent;
  private Composite paramPanel;
  SashForm horizontalSplitter;

  public RulesConfigurationPart(Collection<StandaloneRuleDetails> allRuleDetails, Collection<RuleConfig> initialConfig) {
    this.initialRuleConfigs = initialConfig.stream()
      .collect(Collectors.toMap(RuleConfig::getKey, it -> it));
    this.ruleDetailsWrappersByLanguage = allRuleDetails.stream()
      .sorted(Comparator.comparing(RuleDetails::getKey))
      .map(rd -> new RuleDetailsWrapper(rd, initialRuleConfigs.getOrDefault(rd.getKey(), new RuleConfig(rd.getKey(), rd.isActiveByDefault()))))
      .collect(Collectors.groupingBy(w -> w.ruleDetails.getLanguage(), Collectors.toList()));
    filter = new RuleDetailsWrapperFilter();
    filter.setIncludeLeadingWildcard(true);
  }

  protected void createControls(Composite parent) {
    final SashForm verticalSplitter = new SashForm(parent, SWT.HORIZONTAL);
    GridData fillGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
    verticalSplitter.setLayoutData(fillGridData);

    Composite filterAndTree = new Composite(verticalSplitter, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    filterAndTree.setLayout(layout);

    createFilterPart(filterAndTree);

    createTreeViewer(filterAndTree);

    horizontalSplitter = new SashForm(verticalSplitter, SWT.VERTICAL);
    ruleBrowser = new SonarLintRuleBrowser(horizontalSplitter, false);
    paramPanelParent = new Composite(horizontalSplitter, SWT.NONE);
    paramPanelParent.setLayout(new GridLayout());
    paramPanel = emptyRuleParam();
    horizontalSplitter.setWeights(new int[] {70, 30});
  }

  private void createFilterPart(Composite parent) {
    Composite composite = new Composite(parent, SWT.NONE);
    composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    GridLayout layout = new GridLayout(2, false);
    layout.marginHeight = 0;
    layout.marginWidth = 0;
    layout.horizontalSpacing = 40;
    composite.setLayout(layout);
    ComboViewer combo = new ComboViewer(composite, SWT.READ_ONLY);
    combo.setContentProvider(ArrayContentProvider.getInstance());
    combo.setLabelProvider(new LabelProvider() {
      @Override
      public String getText(Object element) {
        if (element instanceof Type) {
          Type type = (Type) element;
          return type.label;
        }
        return super.getText(element);
      }
    });
    combo.setInput(Type.values());
    combo.setSelection(new StructuredSelection(Type.ALL));
    ISelectionChangedListener selectionChangedListener = event -> {
      IStructuredSelection selection = (IStructuredSelection) event.getSelection();
      if (selection.size() > 0) {
        filter.setType((Type) selection.getFirstElement());
      }
    };
    combo.addSelectionChangedListener(selectionChangedListener);

    ToolBar toolbar = new ToolBar(composite, SWT.FLAT);
    toolbar.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
    createExpansionItem(toolbar, true, SonarLintImages.IMG_EXPAND_ALL, "Expand all");
    createExpansionItem(toolbar, false, SonarLintImages.IMG_COLLAPSE_ALL, "Collapse all");
  }

  private ToolItem createExpansionItem(ToolBar toolBar, final boolean expand, ImageDescriptor image, String tooltip) {
    ToolItem item = new ToolItem(toolBar, SWT.PUSH);
    final Image createdImage = image.createImage();
    item.setImage(createdImage);
    item.setToolTipText(tooltip);
    item.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        if (expand) {
          tree.getViewer().expandAll();
        } else {
          tree.getViewer().collapseAll();
        }
      }
    });
    item.addDisposeListener(e -> createdImage.dispose());
    return item;
  }

  private void createTreeViewer(Composite parent) {
    tree = new CheckBoxFilteredTree(parent);
    GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
    tree.setLayoutData(data);

    tree.getViewer().setContentProvider(new ViewContentProvider());
    tree.getViewer().setInput(ruleDetailsWrappersByLanguage.keySet().toArray(new Language[ruleDetailsWrappersByLanguage.size()]));
    tree.getViewer().setLabelProvider(new LanguageAndRuleLabelProvider());
    tree.getViewer().setComparator(new ViewerComparator() {
      @Override
      public int compare(Viewer viewer, Object e1, Object e2) {
        if (!(e1 instanceof RuleDetailsWrapper && e2 instanceof RuleDetailsWrapper)) {
          return super.compare(viewer, e1, e2);
        }
        RuleDetailsWrapper w1 = (RuleDetailsWrapper) e1;
        RuleDetailsWrapper w2 = (RuleDetailsWrapper) e2;
        return w1.ruleDetails.getName().compareTo(w2.ruleDetails.getName());
      }
    });
    tree.getViewer().getTree().setSortDirection(SWT.DOWN);

    ISelectionChangedListener selectionChangedListener = event -> {
      IStructuredSelection thisSelection = (IStructuredSelection) event.getSelection();
      Object selectedNode = thisSelection.getFirstElement();
      refreshUiForRuleSelection(selectedNode);
    };
    tree.getViewer().addSelectionChangedListener(selectionChangedListener);
  }

  private void refreshUiForRuleSelection(Object selectedNode) {
    paramPanel.dispose();
    if (selectedNode instanceof RuleDetailsWrapper) {
      RuleDetailsWrapper wrapper = (RuleDetailsWrapper) selectedNode;
      ruleBrowser.updateRule(wrapper.ruleDetails);
      if (wrapper.ruleDetails.paramDetails().isEmpty()) {
        paramPanel = emptyRuleParam();
      } else {
        paramPanel = new RuleParameterPanel(paramPanelParent, SWT.NONE, wrapper.ruleDetails, wrapper.ruleConfig);
        paramPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
      }
    } else {
      ruleBrowser.updateRule(null);
      paramPanel = emptyRuleParam();
    }
    PlatformUtils.requestLayout(paramPanel);
  }

  private Composite emptyRuleParam() {
    Composite composite = new Composite(paramPanelParent, SWT.BORDER);
    composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    composite.setLayout(new GridLayout());
    Label label = new Label(composite, SWT.NONE);
    label.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
    label.setText("No parameters");
    return composite;
  }

  enum Type {
    ALL("All rules", w -> true),
    ACTIVE("Active rules", w -> w.ruleConfig.isActive()),
    INACTIVE("Inactive rules", w -> !w.ruleConfig.isActive()),
    CHANGED("Changed rules", RuleDetailsWrapper::isNonDefault);

    final String label;
    final Predicate<RuleDetailsWrapper> predicate;

    Type(String label, Predicate<RuleDetailsWrapper> predicate) {
      this.label = label;
      this.predicate = predicate;
    }
  }

  private class RuleDetailsWrapperFilter extends PatternFilter {
    private Type type = Type.ALL;

    private void setType(Type type) {
      this.type = type;
      tree.refresh();
    }

    @Override
    protected boolean isLeafMatch(Viewer viewer, Object element) {
      if (element instanceof RuleDetailsWrapper) {
        return isRuleMatch((RuleDetailsWrapper) element);
      }
      return false;
    }

    public boolean isRuleMatch(RuleDetailsWrapper element) {
      return type.predicate.test(element) && (wordMatches(element.getName()) || wordMatches(element.ruleDetails.getKey()));
    }

  }

  private class RuleCheckStateListener implements ICheckStateListener {
    @Override
    public void checkStateChanged(CheckStateChangedEvent event) {
      Object element = event.getElement();
      if (element instanceof RuleDetailsWrapper) {
        RuleDetailsWrapper wrapper = (RuleDetailsWrapper) element;
        wrapper.ruleConfig.setActive(event.getChecked());
        tree.getViewer().refresh(wrapper);
        // Refresh the parent to update the check state
        tree.getViewer().refresh(wrapper.ruleDetails.getLanguage());
      } else if (element instanceof Language) {
        Language language = (Language) element;
        tree.getViewer().setExpandedState(element, true);
        ruleDetailsWrappersByLanguage.get(language).stream()
          .filter(filter::isRuleMatch)
          .forEach(w -> w.ruleConfig.setActive(event.getChecked()));
        tree.getViewer().refresh();
      }
      Object currentSelection = tree.getViewer().getStructuredSelection().getFirstElement();
      refreshUiForRuleSelection(currentSelection);
    }
  }

  private class RuleCheckStateProvider implements ICheckStateProvider {
    @Override
    public boolean isGrayed(Object element) {
      if (element instanceof RuleDetailsWrapper) {
        return false;
      }
      if (element instanceof Language) {
        Language language = (Language) element;
        boolean foundActive = false;
        boolean foundInactive = false;
        for (RuleDetailsWrapper wrapper : ruleDetailsWrappersByLanguage.get(language)) {
          if (!filter.isRuleMatch(wrapper)) {
            continue;
          }
          if (wrapper.ruleConfig.isActive()) {
            foundActive = true;
          } else {
            foundInactive = true;
          }

          // stop scanning after found both kinds
          if (foundActive && foundInactive) {
            return true;
          }
        }
        return foundActive == foundInactive;
      }
      return false;
    }

    @Override
    public boolean isChecked(Object element) {
      if (element instanceof RuleDetailsWrapper) {
        RuleDetailsWrapper wrapper = (RuleDetailsWrapper) element;
        return wrapper.ruleConfig.isActive();
      }
      if (element instanceof Language) {
        Language language = (Language) element;
        return ruleDetailsWrappersByLanguage.get(language).stream()
          .filter(filter::isRuleMatch)
          .anyMatch(w -> w.ruleConfig.isActive());
      }
      return false;
    }
  }

  private class ViewContentProvider extends TreeNodeContentProvider {
    @Override
    public Object[] getElements(Object inputElement) {
      return (Language[]) inputElement;
    }

    @Override
    public Object[] getChildren(Object parentElement) {
      if (!(parentElement instanceof Language)) {
        return new Object[0];
      }
      Language language = (Language) parentElement;
      List<RuleDetailsWrapper> list = ruleDetailsWrappersByLanguage.get(language);
      return list.toArray(new RuleDetailsWrapper[list.size()]);
    }

    @Override
    public Object getParent(Object element) {
      if (element instanceof RuleDetailsWrapper) {
        RuleDetailsWrapper wrapper = (RuleDetailsWrapper) element;
        return wrapper.ruleDetails.getLanguage();
      }
      return null;
    }

    @Override
    public boolean hasChildren(Object element) {
      return element instanceof Language;
    }
  }

  private static class LanguageAndRuleLabelProvider extends LabelProvider {
    @Override
    public String getText(Object element) {
      if (element instanceof Language) {
        return ((Language) element).getLabel();
      }
      if (element instanceof RuleDetailsWrapper) {
        RuleDetailsWrapper wrapper = (RuleDetailsWrapper) element;
        return wrapper.ruleDetails.getName();
      }
      return null;
    }
  }

  private static class RuleDetailsWrapper {
    private final StandaloneRuleDetails ruleDetails;
    private RuleConfig ruleConfig;

    RuleDetailsWrapper(StandaloneRuleDetails ruleDetails, RuleConfig ruleConfig) {
      this.ruleDetails = ruleDetails;
      this.ruleConfig = ruleConfig;
    }

    String getName() {
      return ruleDetails.getName();
    }

    boolean isNonDefault() {
      return ruleDetails.isActiveByDefault() != ruleConfig.isActive() || (ruleConfig.isActive() && !ruleConfig.getParams().isEmpty());
    }
  }

  // visible for testing
  public Collection<RuleConfig> computeRulesConfig() {
    Collection<RuleConfig> rules = new ArrayList<>();
    ruleDetailsWrappersByLanguage.entrySet().stream()
      .flatMap(e -> e.getValue().stream())
      .filter(RuleDetailsWrapper::isNonDefault)
      .forEach(w -> rules.add(w.ruleConfig));

    return rules;
  }

  // visible for testing
  public void resetToDefaults() {
    ruleDetailsWrappersByLanguage.entrySet().stream()
      .flatMap(e -> e.getValue().stream())
      .forEach(w -> {
        w.ruleConfig = new RuleConfig(w.ruleDetails.getKey(), w.ruleDetails.isActiveByDefault());
        w.ruleConfig.getParams().clear();
      });
    if (tree != null) {
      tree.getViewer().refresh();
      Object currentSelection = tree.getViewer().getStructuredSelection().getFirstElement();
      refreshUiForRuleSelection(currentSelection);
    }
  }

  private class CheckBoxFilteredTree extends FilteredTree {

    public CheckBoxFilteredTree(Composite parent) {
      super(parent, SWT.CHECK | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER, filter, true);
      setInitialText("type filter text or rule key");
    }

    @Override
    protected TreeViewer doCreateTreeViewer(Composite parent, int style) {
      CheckboxTreeViewer cbTreeViewer = new CheckboxTreeViewer(parent, style);
      cbTreeViewer.addCheckStateListener(new RuleCheckStateListener());
      cbTreeViewer.setCheckStateProvider(new RuleCheckStateProvider());
      createContextMenu(cbTreeViewer);
      return cbTreeViewer;
    }

    private void createContextMenu(TreeViewer viewer) {
      MenuManager contextMenu = new MenuManager("#ViewerMenu"); //$NON-NLS-1$
      contextMenu.setRemoveAllWhenShown(true);
      contextMenu.addMenuListener(this::fillContextMenu);
      Menu menu = contextMenu.createContextMenu(viewer.getControl());
      viewer.getControl().setMenu(menu);
    }

    private void fillContextMenu(IMenuManager contextMenu) {
      contextMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

      contextMenu.add(new Action("Activate") {
        @Override
        public void run() {
          setActiveForSelection(true);
        }
      });

      contextMenu.add(new Action("Deactivate") {
        @Override
        public void run() {
          setActiveForSelection(false);
        }
      });
    }

    private void setActiveForSelection(boolean isActive) {
      ITreeSelection selection = tree.getViewer().getStructuredSelection();
      Iterator<?> iterator = selection.iterator();
      while (iterator.hasNext()) {
        setActiveForElement(iterator.next(), isActive);
      }
      tree.getViewer().refresh();
      Object currentSelection = selection.getFirstElement();
      refreshUiForRuleSelection(currentSelection);
    }

    private void setActiveForElement(Object element, boolean isActive) {
      if (element instanceof RuleDetailsWrapper) {
        RuleDetailsWrapper wrapper = (RuleDetailsWrapper) element;
        wrapper.ruleConfig.setActive(isActive);
      } else if (element instanceof Language) {
        Language language = (Language) element;
        ruleDetailsWrappersByLanguage.get(language).stream().forEach(w -> w.ruleConfig.setActive(isActive));
      }
    }

    public void refresh() {
      super.textChanged();
    }

    @Override
    protected String getFilterString() {
      String filterString = super.getFilterString();
      // Hack to trigger the filtering even if the search string is empty, to filter also on the combobox
      return filterString != null && filterString.length() == 0 ? "*" : filterString;
    }
  }
}
