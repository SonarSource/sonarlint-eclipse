/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2016 SonarSource SA
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
package org.sonarlint.eclipse.cdt.internal;

import java.util.Map;
import org.eclipse.cdt.core.parser.IScannerInfo;

public class BuildWrapperJsonFactory {
  private static final String COMPILER = "clang";
  private static final String EXE_ID = "uniqueid";

  public String create(Map<String, IScannerInfo> fileInfo, String baseDirPath) {
    StringBuilder builder = new StringBuilder();
    builder.append("{"
      + "\"version\":0,"
      + "\"captures\":[");

    writeCompiler(builder);
    builder.append(",");
    writeCompiler(builder);

    for (Map.Entry<String, IScannerInfo> f : fileInfo.entrySet()) {
      builder.append(",");
      writeFile(builder, baseDirPath, f.getKey(), f.getValue().getIncludePaths(), f.getValue().getDefinedSymbols());
    }

    builder.append("]}");
    return builder.toString();
  }

  private static void writeFile(StringBuilder builder, String baseDirPath, String relativeFilePath, String[] includes,
    Map<String, String> symbols) {
    builder.append("{"
      + "\"compiler\":\"" + COMPILER + "\","
      + "\"cwd\":" + quote(baseDirPath) + ","
      + "\"executable\":\"" + EXE_ID + "\","
      + "\"cmd\":["
      + "\"clang\"");

    writeSymbols(builder, symbols);
    writeIncludes(builder, includes);
    builder
      .append(",\"" + relativeFilePath + "\"")
      .append("]}");

  }

  private static void writeIncludes(StringBuilder builder, String[] includes) {
    for (String include : includes) {
      builder.append(",\"-I\"," + quote(include) + "");
    }
  }

  private static void writeSymbols(StringBuilder builder, Map<String, String> symbols) {
    for (Map.Entry<String, String> symbol : symbols.entrySet()) {
      builder.append(",");
      builder.append(quote("-D" + symbol.getKey() + "=" + symbol.getValue()));
    }
  }

  private static void writeCompiler(StringBuilder builder) {
    builder.append("{"
      + "\"compiler\":\"" + COMPILER + "\","
      + "\"executable\":\"" + EXE_ID + "\","
      + "\"stdout\":\"\","
      + "\"stderr\":\"\""
      + "}");
  }

  /**
   * Copied from Jettison
   */
  private static String quote(String string) {
    if (string == null || string.length() == 0) {
      return "\"\"";
    }

    char c;
    int i;
    int len = string.length();
    StringBuilder sb = new StringBuilder(len + 4);
    String t;

    sb.append('"');
    for (i = 0; i < len; i += 1) {
      c = string.charAt(i);
      switch (c) {
        case '\\':
        case '"':
        case '/':
          sb.append('\\');
          sb.append(c);
          break;
        case '\b':
          sb.append("\\b");
          break;
        case '\t':
          sb.append("\\t");
          break;
        case '\n':
          sb.append("\\n");
          break;
        case '\f':
          sb.append("\\f");
          break;
        case '\r':
          sb.append("\\r");
          break;
        default:
          if (c < ' ') {
            t = "000" + Integer.toHexString(c);
            sb.append("\\u" + t.substring(t.length() - 4));
          } else {
            sb.append(c);
          }
      }
    }
    sb.append('"');
    return sb.toString();
  }

}
