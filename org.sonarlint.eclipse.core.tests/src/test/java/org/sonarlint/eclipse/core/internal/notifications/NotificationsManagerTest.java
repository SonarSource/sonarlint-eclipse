/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2017 SonarSource SA
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

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.sonarlint.eclipse.core.internal.notifications.NotificationsManager.ProjectNotificationTime;
import org.sonarlint.eclipse.core.internal.notifications.NotificationsManager.SonarLintProjectConfigurationReader;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProjectConfiguration;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.client.api.notifications.SonarQubeNotificationListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

public class NotificationsManagerTest {

  private NotificationsManager notificationsManager;
  private SonarQubeNotificationListener listener;

  private NotificationsManager.Subscriber subscriber;

  private final String projectKey1 = "pkey1";
  private final String projectKey2 = "pkey2";
  private final String moduleKey1 = "mkey1";
  private final String moduleKey2 = "mkey2";

  private ISonarLintProject project1mod1 = mock(ISonarLintProject.class);
  private ISonarLintProject project1mod2 = mock(ISonarLintProject.class);
  private ISonarLintProject project2mod1 = mock(ISonarLintProject.class);
  private ISonarLintProject project2mod2 = mock(ISonarLintProject.class);

  SonarLintProjectConfigurationReader configReader = new SonarLintProjectConfigurationReader() {
    Map<ISonarLintProject, SonarLintProjectConfiguration> configs = new HashMap<>();
    {
      configs.put(project1mod1, mockConfig(projectKey1, moduleKey1));
      configs.put(project1mod2, mockConfig(projectKey1, moduleKey2));
      configs.put(project2mod1, mockConfig(projectKey2, moduleKey1));
      configs.put(project2mod2, mockConfig(projectKey2, moduleKey2));
    }

    @Override
    public SonarLintProjectConfiguration read(ISonarLintProject project) {
      return configs.get(project);
    };
  };

  @Before
  public void setUp() {
    subscriber = mock(NotificationsManager.Subscriber.class);
    when(subscriber.subscribe(any(), any())).thenReturn(true);

    notificationsManager = new NotificationsManager(subscriber, configReader);
  }

  private SonarLintProjectConfiguration mockConfig(String projectKey, String moduleKey) {
    SonarLintProjectConfiguration config = mock(SonarLintProjectConfiguration.class);
    when(config.getProjectKey()).thenReturn(projectKey);
    when(config.getModuleKey()).thenReturn(moduleKey);
    return config;
  }

  @Test
  public void test_subscribe_and_unsubscribe_one_module_one_project() {
    notificationsManager.subscribe(project1mod1, listener);
    verify(subscriber).subscribe(configReader.read(project1mod1), listener);
    assertThat(notificationsManager.getSubscriberCount()).isEqualTo(1);

    notificationsManager.unsubscribe(project1mod1);
    verify(subscriber).unsubscribe(listener);
    verifyNoMoreInteractions(subscriber);
    assertThat(notificationsManager.getSubscriberCount()).isEqualTo(0);
  }

  @Test
  public void test_subscribe_and_unsubscribe_two_modules_one_project() {
    notificationsManager.subscribe(project1mod1, listener);
    notificationsManager.subscribe(project1mod2, listener);
    verify(subscriber).subscribe(configReader.read(project1mod1), listener);
    assertThat(notificationsManager.getSubscriberCount()).isEqualTo(1);

    notificationsManager.unsubscribe(project1mod1);
    notificationsManager.unsubscribe(project1mod2);
    verify(subscriber).unsubscribe(listener);
    verifyNoMoreInteractions(subscriber);
    assertThat(notificationsManager.getSubscriberCount()).isEqualTo(0);
  }

  @Test
  public void test_subscribe_and_unsubscribe_one_module_each_of_two_projects() {
    notificationsManager.subscribe(project1mod1, listener);
    verify(subscriber).subscribe(configReader.read(project1mod1), listener);
    assertThat(notificationsManager.getSubscriberCount()).isEqualTo(1);

    notificationsManager.subscribe(project2mod1, listener);
    verify(subscriber).subscribe(configReader.read(project2mod1), listener);
    assertThat(notificationsManager.getSubscriberCount()).isEqualTo(2);

    notificationsManager.unsubscribe(project1mod1);
    verify(subscriber).unsubscribe(listener);
    assertThat(notificationsManager.getSubscriberCount()).isEqualTo(1);

    notificationsManager.unsubscribe(project2mod1);
    verify(subscriber, times(2)).unsubscribe(listener);

    verifyNoMoreInteractions(subscriber);
    assertThat(notificationsManager.getSubscriberCount()).isEqualTo(0);
  }

  @Test
  public void second_module_per_project_should_not_trigger_subscription() {
    notificationsManager.subscribe(project1mod1, listener);
    verify(subscriber).subscribe(configReader.read(project1mod1), listener);
    assertThat(notificationsManager.getSubscriberCount()).isEqualTo(1);

    notificationsManager.subscribe(project1mod2, listener);
    verifyNoMoreInteractions(subscriber);
    assertThat(notificationsManager.getSubscriberCount()).isEqualTo(1);
  }

  @Test
  public void unsubscribe_non_last_module_should_not_unsubscribe_from_project() {
    notificationsManager.subscribe(project1mod1, listener);
    verify(subscriber).subscribe(configReader.read(project1mod1), listener);
    assertThat(notificationsManager.getSubscriberCount()).isEqualTo(1);

    notificationsManager.subscribe(project1mod2, listener);
    notificationsManager.unsubscribe(project1mod1);
    verifyNoMoreInteractions(subscriber);
    assertThat(notificationsManager.getSubscriberCount()).isEqualTo(1);
  }

  @Test
  public void unsubscribe_non_subscribed_should_do_nothing() {
    notificationsManager.unsubscribe(project1mod1);
    verifyNoMoreInteractions(subscriber);
    assertThat(notificationsManager.getSubscriberCount()).isEqualTo(0);
  }

  @Test
  public void project_notification_time_should_use_previous_timestamp_when_nothing_changed() {
    ProjectNotificationTime time = new ProjectNotificationTime();
    ZonedDateTime previous = time.get();
    assertThat(previous).isNotNull();
    assertThat(time.get()).isEqualTo(previous);
  }

  @Test
  public void project_notification_time_should_use_latest_timestamp() {
    ProjectNotificationTime time = new ProjectNotificationTime();

    ZonedDateTime previous = time.get();
    ZonedDateTime next = previous.plus(1, ChronoUnit.MINUTES);
    time.set(next);

    assertThat(time.get()).isEqualTo(next);
  }

  @Test
  public void project_notification_time_should_not_update_to_older_timestamp() {
    ProjectNotificationTime time = new ProjectNotificationTime();

    ZonedDateTime previous = time.get();
    ZonedDateTime next = previous.minus(1, ChronoUnit.MINUTES);
    time.set(next);

    assertThat(time.get()).isEqualTo(previous);
  }
}
