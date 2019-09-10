/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2019 SonarSource SA
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
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeNodeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.sonarlint.eclipse.ui.internal.util.SonarLintRuleBrowser;
import org.sonarsource.sonarlint.core.client.api.common.RuleDetails;
import org.sonarsource.sonarlint.core.client.api.common.RuleKey;

// Inspired by: http://www.vogella.com/tutorials/EclipseJFaceTree/article.html
public class RulesConfigurationPart {

  private final Map<String, List<RuleDetailsWrapper>> ruleDetailsWrappersByLanguage;

  private final RuleDetailsWrapperFilter filter;
  private SonarLintRuleBrowser ruleBrowser;
  private CheckBoxFilteredTree tree;
  private Map<String, String> languagesNames;

  public RulesConfigurationPart(Map<String, String> languagesNames, Collection<RuleDetails> allRuleDetails, Collection<RuleKey> excluded, Collection<RuleKey> included) {
    this.languagesNames = languagesNames;
    this.ruleDetailsWrappersByLanguage = allRuleDetails.stream()
      .sorted(Comparator.comparing(RuleDetails::getKey))
      .map(rd -> new RuleDetailsWrapper(rd, excluded, included))
      .collect(Collectors.groupingBy(w -> w.ruleDetails.getLanguageKey(), Collectors.toList()));
    filter = new RuleDetailsWrapperFilter();
    filter.setIncludeLeadingWildcard(true);
  }

  protected void createControls(Composite parent) {
    final SashForm advancedComposite = new SashForm(parent, SWT.VERTICAL);
    GridData sashData = new GridData(SWT.FILL, SWT.FILL, true, true);
    advancedComposite.setLayoutData(sashData);

    Composite filterAndTree = new Composite(advancedComposite, SWT.NONE);
    GridLayout layout = new GridLayout();
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    filterAndTree.setLayout(layout);

    createFilterPart(filterAndTree);

    createTreeViewer(filterAndTree);

    ruleBrowser = new SonarLintRuleBrowser(advancedComposite, false);
    GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
    ruleBrowser.setLayoutData(gridData);
  }

  private void createFilterPart(Composite parent) {
    ComboViewer combo = new ComboViewer(parent, SWT.READ_ONLY);
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
  }

  private void createTreeViewer(Composite parent) {
    tree = new CheckBoxFilteredTree(parent);
    GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
    tree.setLayoutData(data);

    tree.getViewer().setContentProvider(new ViewContentProvider());
    tree.getViewer().setInput(ruleDetailsWrappersByLanguage.keySet().toArray(new String[ruleDetailsWrappersByLanguage.size()]));
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
      if (selectedNode instanceof RuleDetailsWrapper) {
        RuleDetailsWrapper wrapper = (RuleDetailsWrapper) selectedNode;
        ruleBrowser.updateRule(wrapper.ruleDetails);
      }
    };
    tree.getViewer().addSelectionChangedListener(selectionChangedListener);
  }

  enum Type {
    ALL("All rules", w -> true),
    ACTIVE("Active rules", w -> w.isActive),
    INACTIVE("Inactive rules", w -> !w.isActive),
    CHANGED("Changed rules", w -> w.isActive != w.ruleDetails.isActiveByDefault());

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
        wrapper.isActive = event.getChecked();
        tree.getViewer().refresh(wrapper);
        // Refresh the parent to update the check state
        tree.getViewer().refresh(wrapper.ruleDetails.getLanguageKey());
      } else if (element instanceof String) {
        String language = (String) element;
        tree.getViewer().setExpandedState(element, true);
        ruleDetailsWrappersByLanguage.get(language).stream()
          .filter(filter::isRuleMatch)
          .forEach(w -> w.isActive = event.getChecked());
        tree.getViewer().refresh();
      }
    }
  }

  private class RuleCheckStateProvider implements ICheckStateProvider {
    @Override
    public boolean isGrayed(Object element) {
      if (element instanceof RuleDetailsWrapper) {
        return false;
      }
      if (element instanceof String) {
        String language = (String) element;
        boolean foundActive = false;
        boolean foundInActive = false;
        for (RuleDetailsWrapper wrapper : ruleDetailsWrappersByLanguage.get(language)) {
          if (!filter.isRuleMatch(wrapper)) {
            continue;
          }
          if (wrapper.isActive) {
            foundActive = true;
          } else {
            foundInActive = true;
          }

          // stop scanning after found both kinds
          if (foundActive && foundInActive) {
            break;
          }
        }
        return foundActive == foundInActive;
      }
      return false;
    }

    @Override
    public boolean isChecked(Object element) {
      if (element instanceof RuleDetailsWrapper) {
        RuleDetailsWrapper wrapper = (RuleDetailsWrapper) element;
        return wrapper.isActive;
      }
      if (element instanceof String) {
        String language = (String) element;
        return ruleDetailsWrappersByLanguage.get(language).stream()
          .filter(filter::isRuleMatch)
          .anyMatch(w -> w.isActive);
      }
      return false;
    }
  }

  private class ViewContentProvider extends TreeNodeContentProvider {
    @Override
    public Object[] getElements(Object inputElement) {
      return (String[]) inputElement;
    }

    @Override
    public Object[] getChildren(Object parentElement) {
      if (!(parentElement instanceof String)) {
        return new Object[0];
      }
      String language = (String) parentElement;
      List<RuleDetailsWrapper> list = ruleDetailsWrappersByLanguage.get(language);
      return list.toArray(new RuleDetailsWrapper[list.size()]);
    }

    @Override
    public Object getParent(Object element) {
      if (element instanceof RuleDetailsWrapper) {
        RuleDetailsWrapper wrapper = (RuleDetailsWrapper) element;
        return wrapper.ruleDetails.getLanguageKey();
      }
      return null;
    }

    @Override
    public boolean hasChildren(Object element) {
      return element instanceof String;
    }
  }

  private class LanguageAndRuleLabelProvider extends LabelProvider {
    @Override
    public String getText(Object element) {
      if (element instanceof String) {
        return languagesNames.get((String) element);
      }
      if (element instanceof RuleDetailsWrapper) {
        RuleDetailsWrapper wrapper = (RuleDetailsWrapper) element;
        return wrapper.ruleDetails.getName();
      }
      return null;
    }
  }

  private static class RuleDetailsWrapper {
    private final RuleDetails ruleDetails;
    private boolean isActive;

    RuleDetailsWrapper(RuleDetails ruleDetails, Collection<RuleKey> excluded, Collection<RuleKey> included) {
      this.ruleDetails = ruleDetails;
      this.isActive = computeIsActive(ruleDetails.getKey(), ruleDetails.isActiveByDefault(), excluded, included);
    }

    private static boolean computeIsActive(String key, boolean activeByDefault, Collection<RuleKey> excluded, Collection<RuleKey> included) {
      RuleKey ruleKey = RuleKey.parse(key);
      return !excluded.contains(ruleKey) && (activeByDefault || included.contains(ruleKey));
    }

    String getName() {
      return ruleDetails.getName();
    }
  }

  // visible for testing
  public static class ExclusionsAndInclusions {
    private final Collection<RuleKey> excluded;
    private final Collection<RuleKey> included;

    public ExclusionsAndInclusions(Collection<RuleKey> excluded, Collection<RuleKey> included) {
      this.excluded = excluded;
      this.included = included;
    }

    public Collection<RuleKey> excluded() {
      return excluded;
    }

    public Collection<RuleKey> included() {
      return included;
    }
  }

  // visible for testing
  public ExclusionsAndInclusions computeExclusionsAndInclusions() {
    Collection<RuleKey> excluded = new ArrayList<>();
    Collection<RuleKey> included = new ArrayList<>();
    ruleDetailsWrappersByLanguage.entrySet().stream()
      .flatMap(e -> e.getValue().stream())
      .filter(w -> w.isActive != w.ruleDetails.isActiveByDefault())
      .forEach(w -> {
        RuleKey ruleKey = RuleKey.parse(w.ruleDetails.getKey());
        if (w.isActive) {
          included.add(ruleKey);
        } else {
          excluded.add(ruleKey);
        }
      });

    return new ExclusionsAndInclusions(excluded, included);
  }

  // visible for testing
  public void resetToDefaults() {
    ruleDetailsWrappersByLanguage.entrySet().stream()
      .flatMap(e -> e.getValue().stream())
      .forEach(w -> w.isActive = w.ruleDetails.isActiveByDefault());
    if (tree != null) {
      tree.getViewer().refresh();
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
      Iterator<?> iterator = ((IStructuredSelection) tree.getViewer().getSelection()).iterator();
      while (iterator.hasNext()) {
        setActiveForElement(iterator.next(), isActive);
      }
    }

    private void setActiveForElement(Object element, boolean isActive) {
      if (element instanceof RuleDetailsWrapper) {
        RuleDetailsWrapper wrapper = (RuleDetailsWrapper) element;
        wrapper.isActive = isActive;
        tree.getViewer().refresh(element);
      } else if (element instanceof String) {
        String language = (String) element;
        ruleDetailsWrappersByLanguage.get(language).stream().forEach(w -> w.isActive = isActive);
        tree.getViewer().refresh();
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
