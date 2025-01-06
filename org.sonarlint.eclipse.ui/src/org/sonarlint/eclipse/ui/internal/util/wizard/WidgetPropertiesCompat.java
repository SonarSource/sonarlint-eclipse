/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2025 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.util.wizard;

import org.eclipse.jface.databinding.swt.IWidgetValueProperty;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Widget;

public class WidgetPropertiesCompat {
  public static IWidgetValueProperty<Button, Boolean> buttonSelection() {
    if (JFaceUtils.IS_TYPED_API_SUPPORTED) {
      return WidgetProperties.buttonSelection();
    }
    try {
      var widgetPropertiesClass = Class.forName("org.eclipse.jface.databinding.swt.WidgetProperties");
      var selectionMethod = widgetPropertiesClass.getMethod("selection");
      return (IWidgetValueProperty<Button, Boolean>) selectionMethod.invoke(null);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to call deprecated method", e);
    }
  }

  public static <S extends Widget> IWidgetValueProperty<S, String> text(final int event) {
    if (JFaceUtils.IS_TYPED_API_SUPPORTED) {
      return WidgetProperties.text(event);
    }
    try {
      var widgetPropertiesClass = Class.forName("org.eclipse.jface.databinding.swt.WidgetProperties");
      var textMethod = widgetPropertiesClass.getMethod("text", int.class);
      return (IWidgetValueProperty<S, String>) textMethod.invoke(null, event);
    } catch (Exception e) {
      throw new IllegalStateException("Unable to call deprecated method", e);
    }
  }

  private WidgetPropertiesCompat() {
    // utility class
  }
}
