/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2024 SonarSource SA
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
package org.sonarlint.eclipse.core.internal.resources;

import java.util.Locale;
import java.util.Objects;
import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;

public class ExclusionItem {
  private final Type type;
  private final String item;

  public enum Type {
    FILE, DIRECTORY, GLOB
  }

  public ExclusionItem(Type type, String item) {
    this.type = type;
    this.item = item;
  }

  @Nullable
  public static ExclusionItem parse(String text) {
    var i = text.indexOf(':');
    if (i < 0) {
      return null;
    }
    var item = text.substring(i + 1);
    if (StringUtils.trimToNull(item) == null) {
      return null;
    }
    switch (text.substring(0, i).toUpperCase(Locale.US)) {
      case "FILE":
        return new ExclusionItem(Type.FILE, item);
      case "DIRECTORY":
        return new ExclusionItem(Type.DIRECTORY, item);
      case "GLOB":
        return new ExclusionItem(Type.GLOB, item);
      default:
        return null;
    }
  }

  public String item() {
    return item;
  }

  public Type type() {
    return type;
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, item);
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof ExclusionItem)) {
      return false;
    }
    var o = (ExclusionItem) other;

    return Objects.equals(type, o.type) && Objects.equals(item, o.item);
  }

  public String toStringWithType() {
    return type.name() + ":" + item;
  }
}
