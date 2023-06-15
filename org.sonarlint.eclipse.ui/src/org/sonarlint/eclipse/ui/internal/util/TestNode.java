/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2023 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.internal.Utilities;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;

public final class TestNode implements ITypedElement, IEncodedStreamContentAccessor {
  private String content;

  public TestNode(String content) {
    this.content = content;
  }

  @Override
  public String getName() {
    return "no name";
  }

  @Override
  public Image getImage() {
    return null;
  }

  @Override
  public String getType() {
    return "no type";
  }

  @Override
  public InputStream getContents() throws CoreException {
    // TODO Auto-generated method stub
    return new ByteArrayInputStream(Utilities.getBytes(content, "UTF-8"));
  }

  @Override
  public String getCharset() {
    return "UTF-8";
  }
}
