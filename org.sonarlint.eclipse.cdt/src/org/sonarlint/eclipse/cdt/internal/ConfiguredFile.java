/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2024 SonarSource SA
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

import java.util.Collections;
import java.util.Map;
import org.eclipse.core.resources.IFile;

public class ConfiguredFile {
  private final IFile file;
  private final String[] includes;
  private final Map<String, String> symbols;
  private final String path;

  private ConfiguredFile(IFile file, String[] includes, Map<String, String> symbols, String path) {
    this.file = file;
    this.includes = includes;
    this.symbols = Collections.unmodifiableMap(symbols);
    this.path = path;
  }

  public IFile file() {
    return file;
  }

  public String[] includes() {
    return includes;
  }

  public Map<String, String> symbols() {
    return symbols;
  }

  public String path() {
    return path;
  }

  public static class Builder {
    private String[] includes = new String[0];
    private Map<String, String> symbols = Collections.emptyMap();
    private String path;
    private final IFile file;

    public Builder(IFile file) {
      this.file = file;
    }

    public Builder includes(String[] includes) {
      this.includes = includes;
      return this;
    }

    public Builder symbols(Map<String, String> symbols) {
      this.symbols = symbols;
      return this;
    }

    public Builder path(String path) {
      this.path = path;
      return this;
    }

    public ConfiguredFile build() {
      return new ConfiguredFile(file, includes, symbols, path);
    }
  }

}
