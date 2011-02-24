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

package org.sonar.ide.eclipse.internal.jdt.profiles.checkstyle;

import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.sonar.ide.eclipse.internal.jdt.profiles.ProfileConfiguration;
import org.sonar.wsclient.services.Rule;

/**
 * @author Jérémie Lagarde
 * @since 1.1.0
 * 
 * Class to convert Checkstyle LineLengthCheck rule to eclipse profile. 
 * 
 *        ex : {"title":"Line Length", "key":"checkstyle:com.puppycrawl.tools.checkstyle.checks.sizes.LineLengthCheck",
 *        "plugin":"checkstyle", "description":"Checks for long lines.","priority":"MAJOR","status":"ACTIVE", "params":[
 *        {"name":"ignorePattern","description":"pattern for lines to ignore"},
 *        {"name":"max","description":"maximum allowable line length. Default is 80.","value":"180"},
 *        {"name":"tabWidth","description":"number of expanded spaces for a tab character. Default is 8.","value":"4"}]},
 */
public class LineLengthCheckstyleConverter extends AbstractCheckstyleConverter {

  protected final static String KEY = "checkstyle:com.puppycrawl.tools.checkstyle.checks.sizes.LineLengthCheck";

  public LineLengthCheckstyleConverter() {
    super(KEY);
  }

  public void convert(ProfileConfiguration config, Rule rule) {
    String max = getParam(rule, "max", "80");
    String tabWidth = getParam(rule, "tabWidth", "8");
    config.addFormat(DefaultCodeFormatterConstants.FORMATTER_LINE_SPLIT, max);
    config.addFormat(DefaultCodeFormatterConstants.FORMATTER_COMMENT_LINE_LENGTH, max);
    config.addFormat(DefaultCodeFormatterConstants.FORMATTER_TAB_SIZE, tabWidth);
  }

}
