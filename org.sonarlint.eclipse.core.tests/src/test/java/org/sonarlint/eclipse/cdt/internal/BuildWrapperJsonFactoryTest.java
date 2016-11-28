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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.cdt.core.parser.IScannerInfo;
import org.junit.Before;
import org.junit.Test;

public class BuildWrapperJsonFactoryTest {
  private BuildWrapperJsonFactory writer;

  @Before
  public void setUp() {
    writer = new BuildWrapperJsonFactory();
  }

  @Test
  public void test() throws IOException, URISyntaxException {
    Map<String, IScannerInfo> info = new HashMap<>();
    Map<String, String> defines1 = new LinkedHashMap<>();
    defines1.put("MACRO1", "V1");
    defines1.put("MACRO2", "V2");

    info.put(
      "path/to/file1",
      new ScannerInfo(
        new String[] {"/path/to/include1", "/path/to/include2"},
        defines1));

    Map<String, String> defines2 = new LinkedHashMap<>();
    defines2.put("MACRO1", "V1");
    defines2.put("MACRO2", "V2");
    defines2.put("MACRO3", "V3");
    info.put("\\path\\to\\file2",
      new ScannerInfo(
        new String[] {"path\\to\\include1", "\\path\\to\\include2", "\\path\\to\\include3"},
        defines2));

    String json = writer.create(info, "/path/to/projectBaseDir");
    assertThat(json).isEqualTo(loadExpected());

  }

  private String loadExpected() throws IOException, URISyntaxException {
    String str = new String(Files.readAllBytes(Paths.get("src", "test", "resources", "expected.json")), StandardCharsets.UTF_8);
    return str.replace("\n", "").replace("\r", "");
  }

  private static class ScannerInfo implements IScannerInfo {

    private final String[] includes;
    private final Map<String, String> symbols;

    public ScannerInfo(String[] includes, Map<String, String> symbols) {
      this.includes = includes;
      this.symbols = symbols;
    }

    @Override
    public String[] getIncludePaths() {
      return includes;
    }

    @Override
    public Map<String, String> getDefinedSymbols() {
      return symbols;
    }
  }

}
