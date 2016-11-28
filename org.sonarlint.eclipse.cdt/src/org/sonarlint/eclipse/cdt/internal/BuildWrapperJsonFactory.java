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

  public String create(Map<String, IScannerInfo> fileInfo, String baseDirPath) {
    StringBuilder builder = new StringBuilder();
    builder.append("{"
      + "\"version\":0,"
      + "\"captures\":[");

    boolean first = true;
    for (Map.Entry<String, IScannerInfo> f : fileInfo.entrySet()) {
      if (first) {
        first = false;
      } else {
        builder.append(",");
      }
      writeFile(builder, baseDirPath, f.getKey(), f.getValue().getIncludePaths(), f.getValue().getDefinedSymbols());
    }

    builder.append("]}");
    return builder.toString();
  }

  private static void writeFile(StringBuilder builder, String baseDirPath, String relativeFilePath, String[] includes, Map<String, String> symbols) {
    String probeStdout = probeStdout(symbols);
    String probeStderr = probeStderr(includes);
    writeCompilerProbe(builder, relativeFilePath, probeStdout, probeStderr);
    builder.append(",");
    writeCompilerProbe(builder, relativeFilePath, probeStdout, probeStderr);
    builder.append(",");
    builder.append("{")
      .append("\"compiler\":\"" + COMPILER + "\",")
      .append("\"cwd\":" + quote(baseDirPath) + ",")
      .append("\"executable\":" + quote(relativeFilePath) + ",")
      .append("\"cmd\":[")
      .append("\"clang\"")
      .append("," + quote(relativeFilePath) + "")
      .append("]}");

  }

  private static String probeStderr(String[] includes) {
    StringBuilder builder = new StringBuilder("#include <...> search starts here:\n");
    for (String include : includes) {
      builder.append(" ").append(include).append("\n");
    }
    builder.append("End of search list.\n");
    return builder.toString();
  }

  private static String probeStdout(Map<String, String> symbols) {
    StringBuilder builder = new StringBuilder();
    for (Map.Entry<String, String> symbol : symbols.entrySet()) {
      builder.append("#define " + symbol.getKey() + " " + symbol.getValue()).append("\n");
    }
    return builder.toString();
  }

  private static void writeCompilerProbe(StringBuilder builder, String compilerKey, String stdout, String stderr) {
    builder
      .append("{")
      .append("\"compiler\":\"").append(COMPILER).append("\",")
      .append("\"executable\":").append(quote(compilerKey)).append(",")
      .append("\"stdout\":").append(quote(stdout)).append(",")
      .append("\"stderr\":").append(quote(stderr))
      .append("}");
  }

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
