/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2025 SonarSource SA
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

import java.util.Collection;
import java.util.Map;
import org.eclipse.jdt.annotation.Nullable;

public class BuildWrapperJsonFactory {
  private static final String COMPILER = "clang";

  public String create(Collection<ConfiguredFile> files, String baseDirPath) {
    var builder = new StringBuilder();
    builder.append("{"
      + "\"version\":0,"
      + "\"captures\":[");

    var first = true;
    for (var file : files) {
      if (first) {
        first = false;
      } else {
        builder.append(",");
      }
      writeFile(builder, baseDirPath, file.path(), file.includes(), file.symbols());
    }

    builder.append("]}");
    return builder.toString();
  }

  private static void writeFile(StringBuilder builder, String baseDirPath, String filePath, String[] includes, Map<String, String> symbols) {
    var probeStdout = probeStdout(symbols);
    var probeStderr = probeStderr(includes);
    writeCompilerProbe(builder, filePath, probeStdout, probeStderr);
    builder.append(",");
    writeCompilerProbe(builder, filePath, probeStdout, probeStderr);
    builder.append(",");
    builder.append("{")
      .append("\"compiler\":\"" + COMPILER + "\",")
      .append("\"cwd\":" + quote(baseDirPath) + ",")
      .append("\"executable\":" + quote(filePath) + ",")
      .append("\"cmd\":[")
      .append("\"clang\"")
      .append("," + quote(filePath) + "")
      .append("]}");

  }

  private static String probeStderr(String[] includes) {
    var builder = new StringBuilder("#include <...> search starts here:\n");
    for (var include : includes) {
      builder.append(" ").append(include).append("\n");
    }
    builder.append("End of search list.\n");
    return builder.toString();
  }

  private static String probeStdout(Map<String, String> symbols) {
    var builder = new StringBuilder();
    for (var symbol : symbols.entrySet()) {
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

  private static String quote(@Nullable String string) {
    if (string == null || string.length() == 0) {
      return "\"\"";
    }

    char c;
    int i;
    var len = string.length();
    var sb = new StringBuilder(len + 4);
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
