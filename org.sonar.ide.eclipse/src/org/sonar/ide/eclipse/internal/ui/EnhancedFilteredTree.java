/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse.internal.ui;

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
