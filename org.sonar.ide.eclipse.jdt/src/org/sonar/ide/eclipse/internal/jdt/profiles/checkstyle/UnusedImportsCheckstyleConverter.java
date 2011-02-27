/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010-2011 SonarSource
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
package org.sonar.ide.eclipse.internal.jdt.profiles.checkstyle;

import org.eclipse.jdt.internal.corext.fix.CleanUpConstants;
import org.eclipse.jdt.ui.cleanup.CleanUpOptions;
import org.sonar.ide.eclipse.internal.jdt.profiles.ProfileConfiguration;
import org.sonar.wsclient.services.Rule;

/**
 * @author Jérémie Lagarde
 * @since 1.1.0
 * 
 *        Class to convert Checkstyle LineLengthCheck rule to eclipse profile.
 * 
 *        ex : {"title":"Unused Imports","key":"checkstyle:com.puppycrawl.tools.checkstyle.checks.imports.UnusedImportsCheck","plugin":
 *        "checkstyle","description":"Checks for unused import statements.","priority":"INFO","status":"ACTIVE"},
 */
@SuppressWarnings("restriction")
public class UnusedImportsCheckstyleConverter extends AbstractCheckstyleConverter {

  protected final static String KEY = "checkstyle:com.puppycrawl.tools.checkstyle.checks.imports.UnusedImportsCheck";

  public UnusedImportsCheckstyleConverter() {
    super(KEY);
  }
  public void convert(ProfileConfiguration config, Rule rule) {
    config.addFormat(CleanUpConstants.REMOVE_UNUSED_CODE_IMPORTS,CleanUpOptions.TRUE);
  }

}
