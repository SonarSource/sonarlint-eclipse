/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2020 SonarSource SA
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
package org.sonarlint.eclipse.core.internal.telemetry;

import java.nio.file.Path;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sonarsource.sonarlint.core.telemetry.TelemetryClient;
import org.sonarsource.sonarlint.core.telemetry.TelemetryManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class SonarLintTelemetryTest {
  private SonarLintTelemetry telemetry;
  private TelemetryManager engine = mock(TelemetryManager.class);

  @Before
  public void start() throws Exception {
    // Clear property that is set in the surefire arguments (see pom.xml)
    System.clearProperty(SonarLintTelemetry.DISABLE_PROPERTY_KEY);
    this.telemetry = createTelemetry();
  }

  @After
  public void after() {
    System.clearProperty(SonarLintTelemetry.DISABLE_PROPERTY_KEY);
  }

  private SonarLintTelemetry createTelemetry() {
    when(engine.isEnabled()).thenReturn(true);

    SonarLintTelemetry telemetry = new SonarLintTelemetry() {
      public TelemetryManager newTelemetryManager(Path path, TelemetryClient client) {
        return engine;
      }
    };
    telemetry.init();
    return telemetry;
  }

  @Test
  public void disable_property_should_disable_telemetry() throws Exception {
    assertThat(createTelemetry().enabled()).isTrue();

    System.setProperty(SonarLintTelemetry.DISABLE_PROPERTY_KEY, "true");
    assertThat(createTelemetry().enabled()).isFalse();
  }

  @Test
  public void stop_should_trigger_stop_telemetry() {
    when(engine.isEnabled()).thenReturn(true);
    telemetry.stop();
    verify(engine).isEnabled();
    verify(engine).stop();
  }

  @Test
  public void test_scheduler() {
    assertThat(telemetry.getScheduledJob()).isNotNull();
    telemetry.stop();
    assertThat(telemetry.getScheduledJob()).isNull();
  }

  @Test
  public void optOut_should_trigger_disable_telemetry() {
    when(engine.isEnabled()).thenReturn(true);
    telemetry.optOut(true);
    verify(engine).disable();
    telemetry.stop();
  }

  @Test
  public void should_not_opt_out_twice() {
    when(engine.isEnabled()).thenReturn(false);
    telemetry.optOut(true);
    verify(engine).isEnabled();
    verifyNoMoreInteractions(engine);
  }

  @Test
  public void optIn_should_trigger_enable_telemetry() {
    when(engine.isEnabled()).thenReturn(false);
    telemetry.optOut(false);
    verify(engine).enable();
  }

  @Test
  public void upload_should_trigger_upload_when_enabled() {
    when(engine.isEnabled()).thenReturn(true);
    telemetry.upload();
    verify(engine).isEnabled();
    verify(engine).uploadLazily();
  }

  @Test
  public void upload_should_not_trigger_upload_when_disabled() {
    when(engine.isEnabled()).thenReturn(false);
    telemetry.upload();
    verify(engine).isEnabled();
    verifyNoMoreInteractions(engine);
  }

  @Test
  public void analysisDoneOnMultipleFiles_should_trigger_analysisDoneOnMultipleFiles_when_enabled() {
    when(engine.isEnabled()).thenReturn(true);
    telemetry.analysisDoneOnMultipleFiles();
    verify(engine).isEnabled();
    verify(engine).analysisDoneOnMultipleFiles();
  }

  @Test
  public void analysisDoneOnMultipleFiles_should_not_trigger_analysisDoneOnMultipleFiles_when_disabled() {
    when(engine.isEnabled()).thenReturn(false);
    telemetry.analysisDoneOnMultipleFiles();
    verify(engine).isEnabled();
    verifyNoMoreInteractions(engine);
  }

  @Test
  public void analysisDoneOnSingleFile_should_trigger_analysisDoneOnSingleFile_when_enabled() {
    when(engine.isEnabled()).thenReturn(true);
    String language = "java";
    int time = 123;
    telemetry.analysisDoneOnSingleFile(language, time);
    verify(engine).isEnabled();
    verify(engine).analysisDoneOnSingleLanguage(language, time);
  }

  @Test
  public void analysisDoneOnSingleFile_should_not_trigger_analysisDoneOnSingleFile_when_disabled() {
    when(engine.isEnabled()).thenReturn(false);
    String language = "java";
    int time = 123;
    telemetry.analysisDoneOnSingleFile(language, time);
    verify(engine).isEnabled();
    verifyNoMoreInteractions(engine);
  }
}
