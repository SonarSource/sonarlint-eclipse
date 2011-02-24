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
 * @see http://checkstyle.sourceforge.net/config_whitespace.html#WhitespaceAround
 * 
 *      Class to convert Checkstyle WhitespaceAroundCheck rule to eclipse profile.
 * 
 *      ex : {"title":"Whitespace Around", "key":"checkstyle:com.puppycrawl.tools.checkstyle.checks.whitespace.WhitespaceAroundCheck",
 *      "plugin":"checkstyle","description":"Checks that a token is surrounded by whitespace.",
 *      "priority":"MINOR","status":"INACTIVE","params":[ {"name":"tokens","description":"tokens to check"},
 *      {"name":"allowEmptyConstructors","description":"allow empty constructor bodies. Default is false."},
 *      {"name":"allowEmptyMethods","description":"allow empty method bodies. Default is false."}]}
 * 
 */
public class WhitespaceAroundCheckstyleConverter extends AbstractCheckstyleConverter {

  protected final static String KEY = "checkstyle:com.puppycrawl.tools.checkstyle.checks.whitespace.WhitespaceAroundCheck";

  private static enum WAToken {
    ASSIGN, BAND, BAND_ASSIGN, BOR, BOR_ASSIGN, BSR, BSR_ASSIGN, BXOR, BXOR_ASSIGN, COLON, DIV, DIV_ASSIGN, EQUAL, GE, GT, LAND, LCURLY, LE, LITERAL_ASSERT, LITERAL_CATCH, LITERAL_DO, LITERAL_ELSE, LITERAL_FINALLY, LITERAL_FOR, LITERAL_IF, LITERAL_RETURN, LITERAL_SYNCHRONIZED, LITERAL_TRY, LITERAL_WHILE, LOR, LT, MINUS, MINUS_ASSIGN, MOD, MOD_ASSIGN, NOT_EQUAL, PLUS, PLUS_ASSIGN, QUESTION, RCURLY, SL, SLIST, SL_ASSIGN, SR, SR_ASSIGN, STAR
  }

  public WhitespaceAroundCheckstyleConverter() {
    super(KEY);
  }

  public void convert(ProfileConfiguration config, Rule rule) {
    String[] tokens = getParams(rule, "tokens", null);

    // Assignment Operators like =, &=, +=, ...
    configure(
        config,
        hasToken(tokens, WAToken.ASSIGN, WAToken.BAND_ASSIGN, WAToken.BOR_ASSIGN, WAToken.BSR_ASSIGN, WAToken.BXOR_ASSIGN,
            WAToken.DIV_ASSIGN, WAToken.MINUS_ASSIGN, WAToken.MOD_ASSIGN, WAToken.PLUS_ASSIGN, WAToken.SL_ASSIGN, WAToken.SR_ASSIGN),
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_ASSIGNMENT_OPERATOR,
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_ASSIGNMENT_OPERATOR);

    // Bitwise Operators like &, >>, ...
    configure(
        config,
        hasToken(tokens, WAToken.BAND, WAToken.BOR, WAToken.BSR, WAToken.BXOR, WAToken.DIV, WAToken.EQUAL, WAToken.GE, WAToken.GT,
            WAToken.LAND, WAToken.LE, WAToken.LOR, WAToken.LT, WAToken.MINUS, WAToken.MOD, WAToken.NOT_EQUAL, WAToken.PLUS, WAToken.SL,
            WAToken.SR, WAToken.STAR),
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_BINARY_OPERATOR,
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_BINARY_OPERATOR);

    // The : (colon) operator.
    configure(config, hasToken(tokens, WAToken.COLON), DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_ASSERT,
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COLON_IN_ASSERT,
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_CASE,
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COLON_IN_CASE,
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_CONDITIONAL,
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COLON_IN_CONDITIONAL,
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_DEFAULT,
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_COLON_IN_FOR,
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_COLON_IN_FOR);

    // The ? (conditional) operator.
    configure(config, hasToken(tokens, WAToken.QUESTION),
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_QUESTION_IN_CONDITIONAL,
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_QUESTION_IN_CONDITIONAL);

    // The assert keyword.
    configure(config, hasToken(tokens, WAToken.LITERAL_ASSERT),
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_PARENTHESIZED_EXPRESSION);

    // The catch keyword.
    configure(config, hasToken(tokens, WAToken.LITERAL_CATCH),
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_CATCH);

    // Do, else, finally and try keywords.
    configure(config, hasToken(tokens, WAToken.LITERAL_DO, WAToken.LITERAL_ELSE, WAToken.LITERAL_FINALLY, WAToken.LITERAL_TRY),
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_BLOCK);

    // The for keyword.
    configure(config, hasToken(tokens, WAToken.LITERAL_FOR),
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_FOR);

    // The if keyword.
    configure(config, hasToken(tokens, WAToken.LITERAL_IF), DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_IF);

    // The return keyword.
    configure(config, hasToken(tokens, WAToken.LITERAL_RETURN),
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_PARENTHESIZED_EXPRESSION_IN_RETURN);

    // The synchronized keyword.
    configure(config, hasToken(tokens, WAToken.LITERAL_SYNCHRONIZED),
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_SYNCHRONIZED);

    // The while keyword.
    configure(config, hasToken(tokens, WAToken.LITERAL_WHILE),
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_PAREN_IN_WHILE);

    // A left (curly) brace ({).
    configure(config, hasToken(tokens, WAToken.LCURLY),
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ANNOTATION_TYPE_DECLARATION,
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ANONYMOUS_TYPE_DECLARATION,
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ARRAY_INITIALIZER,
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ARRAY_INITIALIZER,
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_BLOCK,
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_CONSTRUCTOR_DECLARATION,
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ENUM_CONSTANT,
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_ENUM_DECLARATION,
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_METHOD_DECLARATION,
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_SWITCH,
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_TYPE_DECLARATION,
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_OPENING_BRACE_IN_ARRAY_INITIALIZER);

    // A right (curly) brace (}).
    configure(config, hasToken(tokens, WAToken.RCURLY),
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_CLOSING_BRACE_IN_ARRAY_INITIALIZER,
        DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_AFTER_CLOSING_BRACE_IN_BLOCK);

    // A list of statements.
    configure(config, hasToken(tokens, WAToken.SLIST), DefaultCodeFormatterConstants.FORMATTER_INSERT_SPACE_BEFORE_OPENING_BRACE_IN_BLOCK);

  }

  private boolean hasToken(String[] tokens, WAToken... values) {
    if(tokens == null)
      return true;
    for(WAToken value : values) {
      return ArrayUtils.contains(tokens, value.name());
    }
    return false;
  }

  private void configure(ProfileConfiguration config, boolean insert, String... formats) {
    String value = (insert) ? JavaCore.INSERT : JavaCore.DO_NOT_INSERT;
    for(String format : formats) {
      config.addFormat(format, value);
    }
  }
}
