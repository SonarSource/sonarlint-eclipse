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

import org.apache.commons.lang.ArrayUtils;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.formatter.DefaultCodeFormatterConstants;
import org.sonar.ide.eclipse.internal.jdt.profiles.ProfileConfiguration;
import org.sonar.wsclient.services.Rule;

/**
 * @author Jérémie Lagarde
 * @since 1.1.0
 * 
 * Class to convert Checkstyle WhitesapceAfterCheck rule to eclipse profile. 
 * 
 *        ex : {"title":"Whitespace After",
 *        "key":"checkstyle:com.puppycrawl.tools.checkstyle.checks.whitespace.WhitespaceAfterCheck",
 *        "plugin":"checkstyle",
 *        "description":"Checks that a token is followed by whitespace, with the exception that it does not check for whitespace after the semicolon of an empty for iterator.",*
 *        "priority":"MINOR", 
 *        "status":"INACTIVE", 
 *        "params":[{"name":"tokens","description":"tokens to check","value":"TK_SEMI,TK_COMMA"}]}
 */
public class WhitespaceAfterCheckstyleConverter extends AbstractCheckstyleConverter {

  protected final static String KEY = "checkstyle:com.puppycrawl.tools.checkstyle.checks.whitespace.WhitespaceAfterCheck";
  
  private final static String TK_COMMA = "COMMA";
  private final static String TK_SEMI = "SEMI";
  private final static String TK_TYPECAST = "TYPECAST";
  private final static String[] TK_DEFAULT = new String[] { TK_COMMA, TK_SEMI, TK_TYPECAST };

  public WhitespaceAfterCheckstyleConverter() {
    super(KEY);
  }

  public void convert(ProfileConfiguration config, Rule rule) {
    String[] tokens = getParams(rule, "tokens", TK_DEFAULT);

    setComma(config, ArrayUtils.contains(tokens, TK_COMMA));
    setSemi(config, ArrayUtils.contains(tokens, TK_SEMI));
    setType(config, ArrayUtils.contains(tokens, TK_TYPECAST));
  }

  private void setComma(ProfileConfiguration config, boolean insert) {
    String value = (insert)?JavaCore.INSERT:JavaCore.DO_NOT_INSERT;
    config.add(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_ALLOCATION_EXPRESSION, value);
    config.add(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_ANNOTATION, value);
    config.add(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_ARRAY_INITIALIZER, value);
    config.add(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_CONSTRUCTOR_DECLARATION_PARAMETERS, value);
    config.add(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_CONSTRUCTOR_DECLARATION_THROWS, value);
    config.add(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_ENUM_CONSTANT_ARGUMENTS, value);
    config.add(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_ENUM_DECLARATIONS, value);
    config.add(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_EXPLICIT_CONSTRUCTOR_CALL_ARGUMENTS, value);
    config.add(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_FOR_INCREMENTS, value);
    config.add(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_FOR_INITS, value);
    config.add(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_DECLARATION_PARAMETERS, value);
    config.add(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_DECLARATION_THROWS, value);
    config.add(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_METHOD_INVOCATION_ARGUMENTS, value);
    config.add(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_MULTIPLE_FIELD_DECLARATIONS, value);
    config.add(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_MULTIPLE_LOCAL_DECLARATIONS, value);
    config.add(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_PARAMETERIZED_TYPE_REFERENCE, value);
    config.add(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_SUPERINTERFACES, value);
    config.add(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_TYPE_ARGUMENTS, value);
    config.add(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COMMA_IN_TYPE_PARAMETERS, value);
  }

  private void setSemi(ProfileConfiguration config, boolean insert) {
    String value = (insert)?JavaCore.INSERT:JavaCore.DO_NOT_INSERT;
    config.add(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_SEMICOLON_IN_FOR, value);
  }

  private void setType(ProfileConfiguration config, boolean insert) {
    String value = (insert)?JavaCore.INSERT:JavaCore.DO_NOT_INSERT;
    config.add(DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_CLOSING_PAREN_IN_CAST, value);
  }

}
