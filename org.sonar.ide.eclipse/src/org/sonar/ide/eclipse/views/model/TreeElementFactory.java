/*
 * Copyright (C) 2010 Evgeny Mandrikov
 *
 * Sonar-IDE is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar-IDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar-IDE; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse.views.model;

import org.sonar.ide.api.SourceCode;
import org.sonar.wsclient.services.Resource;

/**
 * @author Jérémie Lagarde
 */
public final class TreeElementFactory {

  public enum SonarResourceScope {
    PRJ, // project/module
    DIR, // directory (like Java package)
    FIL
    // file
  }

  public enum SonarResourceQualifier {
    TRK, // project
    BRC, // module
    CLA, // class
    UTS, // unit test
    DIR, // directory
    FIL
    // file
  }

  private TreeElementFactory() {
  }

  public static TreeObject create(SourceCode sourceCode) {
    // TODO
    return new TreeParent(sourceCode) {
    };
  }

  /**
   * @deprecated don't use sonar-ws-client directly
   */
  @Deprecated
  public static TreeObject create(Resource resource) {
    if (resource == null) {
      return null;
    }
    // switch (SonarResourceScope.valueOf(resource.getScope())) {
    // case PRJ:
    // return new TreeProject(resource);
    // case DIR:
    // return new TreeDirectory(resource);
    // case FIL:
    // return new TreeFile(resource);
    // default:
    // break;
    // }
    return null;
  }

}
