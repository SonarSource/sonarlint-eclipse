/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2021 SonarSource SA
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
package org.sonarlint.eclipse.core.internal.notifications;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectedEngineFacade;
import org.sonarlint.eclipse.core.internal.engine.connected.ConnectedEngineFacadeManager;
import org.sonarlint.eclipse.core.internal.engine.connected.ResolvedBinding;
import org.sonarlint.eclipse.core.internal.notifications.NotificationsManager.ProjectNotificationTime;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintProjectConfiguration;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.client.api.notifications.ServerNotificationListener;
import org.sonarsource.sonarlint.core.notifications.ServerNotificationsRegistry;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class NotificationsManagerTest {

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  private NotificationsManager underTest;
  private ServerNotificationListener listener;
  private final ConnectedEngineFacadeManager facadeManager = mock(ConnectedEngineFacadeManager.class);
  private final ServerNotificationsRegistry registry = mock(ServerNotificationsRegistry.class);
  private final ConnectedEngineFacade engineFacade = mock(ConnectedEngineFacade.class);

  private static final String PROJECT_KEY_1 = "pkey1";
  private static final String PROJECT_KEY_2 = "pkey2";
  private static final SonarLintProjectConfiguration.EclipseProjectBinding BINDING_1 = new SonarLintProjectConfiguration.EclipseProjectBinding("serverId", PROJECT_KEY_1, "", "");
  private static final SonarLintProjectConfiguration.EclipseProjectBinding BINDING_2 = new SonarLintProjectConfiguration.EclipseProjectBinding("serverId", PROJECT_KEY_2, "", "");

  private final ISonarLintProject project1mod1 = mock(ISonarLintProject.class);
  private final ISonarLintProject project1mod2 = mock(ISonarLintProject.class);
  private final ISonarLintProject project2mod1 = mock(ISonarLintProject.class);
  private final ISonarLintProject project2mod2 = mock(ISonarLintProject.class);

  Map<ISonarLintProject, SonarLintProjectConfiguration> configs = new HashMap<>();
  {
    configs.put(project1mod1, mockConfig(BINDING_1));
    configs.put(project1mod2, mockConfig(BINDING_1));
    configs.put(project2mod1, mockConfig(BINDING_2));
    configs.put(project2mod2, mockConfig(BINDING_2));
  }

  private SonarLintProjectConfiguration mockConfig(SonarLintProjectConfiguration.EclipseProjectBinding binding) {
    SonarLintProjectConfiguration config = mock(SonarLintProjectConfiguration.class);
    when(config.getProjectBinding()).thenReturn(Optional.of(binding));
    return config;
  }

  @Before
  public void setUp() throws IOException {
    underTest = new NotificationsManager(registry, p -> configs.get(p), facadeManager);
    when(project1mod1.getName()).thenReturn("project1-module1");
    when(project1mod1.getWorkingDir()).thenReturn(tmp.newFolder().toPath());
    when(project1mod2.getName()).thenReturn("project1-module2");
    when(project1mod2.getWorkingDir()).thenReturn(tmp.newFolder().toPath());
    when(project2mod1.getName()).thenReturn("project2-module1");
    when(project2mod1.getWorkingDir()).thenReturn(tmp.newFolder().toPath());
    when(project2mod2.getName()).thenReturn("project2-module2");
    when(project2mod2.getWorkingDir()).thenReturn(tmp.newFolder().toPath());

    when(engineFacade.checkNotificationsSupported()).thenReturn(true);

    when(facadeManager.resolveBinding(project1mod1))
      .thenReturn(Optional.of(new ResolvedBinding(BINDING_1, engineFacade)));
    when(facadeManager.resolveBinding(project1mod2))
      .thenReturn(Optional.of(new ResolvedBinding(BINDING_1, engineFacade)));
    when(facadeManager.resolveBinding(project2mod1))
      .thenReturn(Optional.of(new ResolvedBinding(BINDING_2, engineFacade)));
    when(facadeManager.resolveBinding(project2mod2))
      .thenReturn(Optional.of(new ResolvedBinding(BINDING_2, engineFacade)));
  }

  @Test
  public void test_subscribe_and_unsubscribe_one_module_one_project() {
    underTest.subscribeToNotifications(singletonList(project1mod1), f -> listener);
    verify(registry).register(any());

    underTest.unsubscribe(project1mod1);
    verify(registry).remove(any());

    verifyNoMoreInteractions(registry);
  }

  @Test
  public void test_subscribe_and_unsubscribe_two_modules_one_project() {
    underTest.subscribeToNotifications(asList(project1mod1, project1mod2), f -> listener);
    verify(registry, times(1)).register(any());

    underTest.unsubscribe(project1mod1);
    underTest.unsubscribe(project1mod2);
    verify(registry, times(1)).remove(any());

    verifyNoMoreInteractions(registry);
  }

  @Test
  public void test_subscribe_and_unsubscribe_one_module_each_of_two_projects() {
    underTest.subscribeToNotifications(asList(project1mod1, project2mod1), f -> listener);
    verify(registry, times(2)).register(any());

    underTest.unsubscribe(project1mod1);
    underTest.unsubscribe(project2mod1);
    verify(registry, times(2)).remove(any());

    verifyNoMoreInteractions(registry);
  }

  @Test
  public void unsubscribe_non_last_module_should_not_unsubscribe_from_project() {
    underTest.subscribeToNotifications(asList(project1mod1, project1mod2), f -> listener);
    verify(registry, times(1)).register(any());

    underTest.unsubscribe(project1mod1);

    verifyNoMoreInteractions(registry);
  }

  @Test
  public void unsubscribe_non_subscribed_should_do_nothing() {
    underTest.unsubscribe(project1mod1);

    verifyNoMoreInteractions(registry);
  }

  @Test
  public void should_not_subscribe_when_notifications_are_disabled() {
    when(engineFacade.areNotificationsDisabled()).thenReturn(true);

    underTest.subscribeToNotifications(singletonList(project1mod1), f -> listener);
    underTest.unsubscribe(project1mod1);

    verifyNoMoreInteractions(registry);
  }

  @Test
  public void should_not_subscribe_when_notifications_are_unsupported() {
    when(engineFacade.checkNotificationsSupported()).thenReturn(false);

    underTest.subscribeToNotifications(singletonList(project1mod1), f -> listener);
    underTest.unsubscribe(project1mod1);

    verifyNoMoreInteractions(registry);
  }

  @Test
  public void should_not_subscribe_when_project_not_correctly_bound() {
    when(facadeManager.resolveBinding(project1mod1)).thenReturn(Optional.empty());

    underTest.subscribeToNotifications(singletonList(project1mod1), f -> listener);

    verifyNoMoreInteractions(registry);
  }

  @Test
  public void should_not_subscribe_when_project_key_null() {
    ISonarLintProject moduleWithoutProjectKey = mock(ISonarLintProject.class);
    configs.put(moduleWithoutProjectKey, mock(SonarLintProjectConfiguration.class));

    underTest.subscribeToNotifications(singletonList(moduleWithoutProjectKey), f -> listener);

    verifyNoMoreInteractions(registry);
  }

  @Test
  public void should_stop_registry() {
    underTest.stop();

    verify(registry).stop();
  }

  ProjectNotificationTime newProjectNotificationTime() throws IOException {
    return new ProjectNotificationTime(new NotificationsTracker(tmp.newFolder().toPath()));
  }

  @Test
  public void project_notification_time_should_use_previous_timestamp_when_nothing_changed() throws IOException {
    ProjectNotificationTime time = newProjectNotificationTime();
    ZonedDateTime previous = time.get();
    assertThat(previous).isNotNull();
    assertThat(time.get()).isEqualTo(previous);
  }

  @Test
  public void project_notification_time_should_use_latest_timestamp() throws IOException {
    ProjectNotificationTime time = newProjectNotificationTime();

    ZonedDateTime previous = time.get();
    ZonedDateTime next = previous.plus(1, ChronoUnit.MINUTES);
    time.set(next);

    assertThat(time.get()).isEqualTo(next);
  }

  @Test
  public void project_notification_time_should_not_update_to_older_timestamp() throws IOException {
    ProjectNotificationTime time = newProjectNotificationTime();

    ZonedDateTime previous = time.get();
    ZonedDateTime next = previous.minus(1, ChronoUnit.MINUTES);
    time.set(next);

    assertThat(time.get()).isEqualTo(previous);
  }
}
