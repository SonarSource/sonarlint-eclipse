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
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.sonarlint.eclipse.ui.internal.views.RuleDescriptionPart;
import org.sonarsource.sonarlint.core.client.api.common.RuleDetails;
import org.sonarsource.sonarlint.core.client.api.common.RuleKey;

// Inspired by: http://www.vogella.com/tutorials/EclipseJFaceTree/article.html
public class RulesConfigurationPart {

  private final Map<String, List<RuleDetailsWrapper>> ruleDetailsWrappersByLanguage;

  private CheckboxTreeViewer viewer;

  private RuleDescriptionPart ruleDescriptionPart;

  public RulesConfigurationPart(Collection<RuleDetails> allRuleDetails, Collection<RuleKey> excluded, Collection<RuleKey> included) {
    this.ruleDetailsWrappersByLanguage = allRuleDetails.stream()
      .sorted(Comparator.comparing(RuleDetails::getKey))
      .map(rd -> new RuleDetailsWrapper(rd, excluded, included))
      .collect(Collectors.groupingBy(w -> w.ruleDetails.getLanguage(), Collectors.toList()));
  }

  @PostConstruct
  protected void createControls(Composite parent) {
    Composite pageComponent = new Composite(parent, SWT.NULL);
    GridLayout layout = new GridLayout(1, false);
    layout.marginWidth = 0;
    layout.marginHeight = 0;
    pageComponent.setLayout(layout);

    Composite treeComposite = new Composite(pageComponent, SWT.NONE);
    GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
    treeComposite.setLayout(new FillLayout());
    treeComposite.setLayoutData(gridData);
    createTreeViewer(treeComposite);
    createContextMenu();

    Composite descriptionComposite = new Composite(pageComponent, SWT.NONE);
    descriptionComposite.setLayoutData(gridData);
    descriptionComposite.setLayout(new FillLayout());
    createRuleDescriptionPart(descriptionComposite);
  }

  private void createTreeViewer(Composite parent) {
    viewer = new CheckboxTreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
    viewer.setContentProvider(new ViewContentProvider());
    viewer.getTree().setHeaderVisible(true);

    TreeViewerColumn languageColumn = new TreeViewerColumn(viewer, SWT.NONE);
    languageColumn.getColumn().setText("Language");
    languageColumn.getColumn().setWidth(100);
    languageColumn.setLabelProvider(new DelegatingStyledCellLabelProvider(new LanguageLabelProvider()));

    TreeViewerColumn ruleNameColumn = new TreeViewerColumn(viewer, SWT.NONE);
    ruleNameColumn.getColumn().setText("Rule name");
    ruleNameColumn.getColumn().setWidth(300);
    ruleNameColumn.setLabelProvider(new DelegatingStyledCellLabelProvider(new RuleNameLabelProvider()));

    TreeViewerColumn ruleKeyColumn = new TreeViewerColumn(viewer, SWT.NONE);
    ruleKeyColumn.getColumn().setText("Rule key");
    ruleKeyColumn.getColumn().setWidth(100);
    ruleKeyColumn.setLabelProvider(new DelegatingStyledCellLabelProvider(new RuleKeyLabelProvider()));

    viewer.setInput(ruleDetailsWrappersByLanguage.keySet().toArray(new String[ruleDetailsWrappersByLanguage.size()]));

    ISelectionChangedListener selectionChangedListener = event -> {
      IStructuredSelection thisSelection = (IStructuredSelection) event.getSelection();
      Object selectedNode = thisSelection.getFirstElement();
      if (selectedNode instanceof RuleDetailsWrapper) {
        RuleDetailsWrapper wrapper = (RuleDetailsWrapper) selectedNode;
        if (ruleDescriptionPart != null) {
          ruleDescriptionPart.updateView(wrapper.ruleDetails);
        }
      }
    };
    viewer.addSelectionChangedListener(selectionChangedListener);

    ICheckStateListener checkStateListener = event -> {
      Object element = event.getElement();
      if (element instanceof RuleDetailsWrapper) {
        RuleDetailsWrapper wrapper = (RuleDetailsWrapper) element;
        wrapper.isActive = event.getChecked();
        viewer.refresh(element);
      } else if (element instanceof String) {
        viewer.setSubtreeChecked(element, event.getChecked());
        String language = (String) element;
        ruleDetailsWrappersByLanguage.get(language).forEach(w -> w.isActive = event.getChecked());
      }
    };
    viewer.addCheckStateListener(checkStateListener);

    ICheckStateProvider checkStateProvider = new ICheckStateProvider() {
      @Override
      public boolean isGrayed(Object element) {
        return false;
      }

      @Override
      public boolean isChecked(Object element) {
        if (element instanceof RuleDetailsWrapper) {
          RuleDetailsWrapper wrapper = (RuleDetailsWrapper) element;
          return wrapper.isActive;
        } else if (element instanceof String) {
          String language = (String) element;
          return ruleDetailsWrappersByLanguage.get(language).stream().allMatch(w -> w.isActive);
        }
        return false;
      }
    };
    viewer.setCheckStateProvider(checkStateProvider);
  }

  private void createRuleDescriptionPart(Composite parent) {
    try {
      Browser browser = new Browser(parent, SWT.FILL);
      browser.setText("(No rule selected.)");
      ruleDescriptionPart = new RuleDescriptionPart(browser);
      ruleDescriptionPart.setExtraCss("body { background-color: white; }\n");
    } catch (SWTError e) {
      // Browser is probably not available but it will be partially initialized in parent
      for (Control c : parent.getChildren()) {
        c.dispose();
      }
      new Label(parent, SWT.WRAP).setText("Unable to create SWT Browser:\n " + e.getMessage());
    }
  }

  private void createContextMenu() {
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
    Iterator<?> iterator = ((IStructuredSelection) viewer.getSelection()).iterator();
    while (iterator.hasNext()) {
      setActiveForElement(iterator.next(), isActive);
    }
  }

  private void setActiveForElement(Object element, boolean isActive) {
    if (element instanceof RuleDetailsWrapper) {
      RuleDetailsWrapper wrapper = (RuleDetailsWrapper) element;
      wrapper.isActive = isActive;
      viewer.refresh(element);
    } else if (element instanceof String) {
      String language = (String) element;
      ruleDetailsWrappersByLanguage.get(language).stream().forEach(w -> w.isActive = isActive);
      viewer.refresh();
    }
  }

  private class ViewContentProvider implements ITreeContentProvider {
    @Override
    public void inputChanged(Viewer v, Object oldInput, Object newInput) {
      // ignore, we never switch elements
    }

    @Override
    public void dispose() {
      // nothing to dispose
    }

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
      RuleDetailsWrapper wrapper = (RuleDetailsWrapper) element;
      return wrapper.ruleDetails.getLanguage();
    }

    @Override
    public boolean hasChildren(Object element) {
      return element instanceof String;
    }
  }

  private class LanguageLabelProvider extends LabelProvider implements IStyledLabelProvider {
    @Override
    public StyledString getStyledText(Object element) {
      if (element instanceof String) {
        String language = (String) element;
        StyledString styledString = new StyledString(language);
        styledString.append(" ( " + ruleDetailsWrappersByLanguage.get(language).size() + " ) ", StyledString.COUNTER_STYLER);
        return styledString;
      }
      if (element instanceof RuleDetailsWrapper) {
        RuleDetailsWrapper wrapper = (RuleDetailsWrapper) element;
        return new StyledString(wrapper.ruleDetails.getLanguage());
      }
      return new StyledString();
    }
  }

  private static class RuleKeyLabelProvider extends LabelProvider implements IStyledLabelProvider {
    @Override
    public StyledString getStyledText(Object element) {
      if (element instanceof RuleDetailsWrapper) {
        RuleDetailsWrapper wrapper = (RuleDetailsWrapper) element;
        return new StyledString(wrapper.ruleDetails.getKey());
      }
      return new StyledString();
    }
  }

  private static class RuleNameLabelProvider extends LabelProvider implements IStyledLabelProvider {
    @Override
    public StyledString getStyledText(Object element) {
      if (element instanceof RuleDetailsWrapper) {
        RuleDetailsWrapper wrapper = (RuleDetailsWrapper) element;
        return new StyledString(wrapper.ruleDetails.getName());
      }
      return new StyledString();
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
  }

  void refresh() {
    viewer.refresh();
  }
}
