package org.sonar.ide.eclipse.ui;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;

import java.lang.reflect.Field;

/**
 * A {@link FilteredTree} that uses the new look on Eclipse 3.5 and later.
 * This class can be removed, when time will come to end support for Eclipse 3.4.
 */
@SuppressWarnings("deprecation")
public class EnhancedFilteredTree extends FilteredTree {
  protected boolean useNewLook;

  public EnhancedFilteredTree(Composite parent, int treeStyle, PatternFilter filter, boolean useNewLook) {
    super(parent, treeStyle, filter);
  }

  public EnhancedFilteredTree(Composite parent, int treeStyle, PatternFilter filter) {
    super(parent, treeStyle, filter);
  }

  public EnhancedFilteredTree(Composite parent) {
    super(parent);
  }

  @Override
  protected void createControl(Composite parent, int treeStyle) {
    useNewLook = setNewLook(this);
    super.createControl(parent, treeStyle);
  }

  public static boolean setNewLook(FilteredTree tree) {
    try {
      Field newStyleField = FilteredTree.class.getDeclaredField("useNewLook"); //$NON-NLS-1$
      newStyleField.setAccessible(true);
      newStyleField.setBoolean(tree, true);
      return newStyleField.getBoolean(tree);
    } catch (Exception e) {
      // ignore
    }
    return false;
  }
}
