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
package org.sonarlint.eclipse.core.http;

import java.nio.file.Paths;
import java.time.Duration;
import org.junit.Test;
import org.sonarlint.eclipse.core.internal.http.HttpClient;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.initialize.HttpConfigurationDto;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.initialize.SslConfigurationDto;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpClientTest {
  private static final String GOOGLE = "https://www.google.com";

  @Test
  public void test_404_request() {
    var client = noConfigHttpClient();
    var response = client.getWebsiteContent("https://abc.def.ghi");
    assertThat(client.getContext()).isNull();
    assertThat(response).isNull();
  }

  @Test
  public void test_200_request() {
    var client = noConfigHttpClient();
    var response = client.getWebsiteContent(GOOGLE);
    assertThat(client.getContext()).isNull();
    assertThat(response).isNotNull();
  }

  @Test
  public void test_too_short_timeouts() {
    var sonicClient = new HttpClient(
      new HttpConfigurationDto(
        new SslConfigurationDto(null, null, null, null, null, null),
        Duration.ofNanos(1), null, Duration.ofNanos(1), null));
    var response = sonicClient.getWebsiteContent(GOOGLE);
    assertThat(sonicClient.getContext()).isNull();
    assertThat(response).isNull();
  }

  @Test
  public void test_incorrect_trustStoreConfig() {
    var client1 = new HttpClient(
      new HttpConfigurationDto(
        new SslConfigurationDto(Paths.get("/not/available"), "password", "jks", null, null, null),
        null, null, null, null));
    assertThat(client1.getContext()).isNull();

    var client2 = new HttpClient(
      new HttpConfigurationDto(
        new SslConfigurationDto(Paths.get("/not/available"), "password", null, null, null, null),
        null, null, null, null));
    assertThat(client2.getContext()).isNull();
  }

  @Test
  public void test_incorrect_keyStoreConfig() {
    var client1 = new HttpClient(
      new HttpConfigurationDto(
        new SslConfigurationDto(null, null, null, Paths.get("/not/available"), "password", "jks"),
        null, null, null, null));
    assertThat(client1.getContext()).isNull();

    var client2 = new HttpClient(
      new HttpConfigurationDto(
        new SslConfigurationDto(null, null, null, Paths.get("/not/available"), "password", null),
        null, null, null, null));
    assertThat(client2.getContext()).isNull();
  }

  private static HttpClient noConfigHttpClient() {
    return new HttpClient(
      new HttpConfigurationDto(
        new SslConfigurationDto(null, null, null, null, null, null),
        null, null, null, null));
  }
}
