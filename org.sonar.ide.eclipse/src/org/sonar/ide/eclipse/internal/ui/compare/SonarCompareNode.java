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

package org.sonar.ide.eclipse.internal.ui.compare;

import org.eclipse.compare.IModificationDate;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * @author Jérémie Lagarde
 */
public class SonarCompareNode implements IStreamContentAccessor, ITypedElement, IModificationDate {
  private final String contents;
  private final String name;

  SonarCompareNode(final String name, final String contents) {
    this.name = name;
    this.contents = contents;
  }

  public InputStream getContents() throws CoreException {
    return new ByteArrayInputStream(contents.getBytes());
  }

  public Image getImage() {
    return null;
  }

  public long getModificationDate() {
    return System.currentTimeMillis();
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return ITypedElement.TEXT_TYPE;
  }
}
