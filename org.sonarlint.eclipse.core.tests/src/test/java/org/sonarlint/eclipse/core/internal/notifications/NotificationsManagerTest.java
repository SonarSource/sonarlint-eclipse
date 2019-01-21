/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2019 SonarSource SA
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
import org.sonarlint.eclipse.core.internal.notifications.NotificationsManager.ProjectNotificationTime;
import org.sonarlint.eclipse.core.internal.notifications.NotificationsManager.SonarLintProjectConfigurationReader;
import org.sonarlint.eclipse.core.internal.resources.SonarLintProjectConfiguration;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarsource.sonarlint.core.client.api.notifications.SonarQubeNotificationListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NotificationsManagerTest {

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  private NotificationsManager notificationsManager;
  private SonarQubeNotificationListener listener;

  private final FakeSubscriber subscriber = new FakeSubscriber();

  private final String projectKey1 = "pkey1";
  private final String projectKey2 = "pkey2";

  private ISonarLintProject project1mod1 = mock(ISonarLintProject.class);
  private ISonarLintProject project1mod2 = mock(ISonarLintProject.class);
  private ISonarLintProject project2mod1 = mock(ISonarLintProject.class);
  private ISonarLintProject project2mod2 = mock(ISonarLintProject.class);

  Map<ISonarLintProject, SonarLintProjectConfiguration> configs = new HashMap<>();
  {
    configs.put(project1mod1, mockConfig(projectKey1));
    configs.put(project1mod2, mockConfig(projectKey1));
    configs.put(project2mod1, mockConfig(projectKey2));
    configs.put(project2mod2, mockConfig(projectKey2));
  }

  SonarLintProjectConfigurationReader configReader = p -> configs.get(p);

  private SonarLintProjectConfiguration mockConfig(String projectKey) {
    SonarLintProjectConfiguration config = mock(SonarLintProjectConfiguration.class);
    when(config.getProjectBinding()).thenReturn(Optional.of(new SonarLintProjectConfiguration.EclipseProjectBinding("serverId", projectKey, "", "")));
    return config;
  }

  static class FakeSubscriber extends NotificationsManager.Subscriber {
    int count;

    @Override
    public boolean subscribe(ISonarLintProject project, SonarLintProjectConfiguration config, SonarQubeNotificationListener listener) {
      count++;
      return true;
    }

    public void unsubscribe(SonarQubeNotificationListener listener) {
      count--;
    }
  }

  @Before
  public void setUp() {
    notificationsManager = new NotificationsManager(subscriber, configReader);
    when(project1mod1.getName()).thenReturn("project1-module1");
    when(project1mod2.getName()).thenReturn("project1-module2");
    when(project2mod1.getName()).thenReturn("project2-module1");
    when(project2mod2.getName()).thenReturn("project2-module2");
  }

  @Test
  public void test_subscribe_and_unsubscribe_one_module_one_project() {
    notificationsManager.subscribe(project1mod1, listener);
    assertThat(subscriber.count).isEqualTo(1);

    notificationsManager.unsubscribe(project1mod1);
    assertThat(subscriber.count).isEqualTo(0);
  }

  @Test
  public void test_subscribe_and_unsubscribe_two_modules_one_project() {
    notificationsManager.subscribe(project1mod1, listener);
    notificationsManager.subscribe(project1mod2, listener);
    assertThat(subscriber.count).isEqualTo(1);

    notificationsManager.unsubscribe(project1mod1);
    notificationsManager.unsubscribe(project1mod2);
    assertThat(subscriber.count).isEqualTo(0);
  }

  @Test
  public void test_subscribe_and_unsubscribe_one_module_each_of_two_projects() {
    notificationsManager.subscribe(project1mod1, listener);
    assertThat(subscriber.count).isEqualTo(1);

    notificationsManager.subscribe(project2mod1, listener);
    assertThat(subscriber.count).isEqualTo(2);

    notificationsManager.unsubscribe(project1mod1);
    assertThat(subscriber.count).isEqualTo(1);

    notificationsManager.unsubscribe(project2mod1);
    assertThat(subscriber.count).isEqualTo(0);
  }

  @Test
  public void second_module_per_project_should_not_trigger_subscription() {
    notificationsManager.subscribe(project1mod1, listener);
    assertThat(subscriber.count).isEqualTo(1);

    notificationsManager.subscribe(project1mod2, listener);
    assertThat(subscriber.count).isEqualTo(1);
  }

  @Test
  public void unsubscribe_non_last_module_should_not_unsubscribe_from_project() {
    notificationsManager.subscribe(project1mod1, listener);
    assertThat(subscriber.count).isEqualTo(1);

    notificationsManager.subscribe(project1mod2, listener);
    notificationsManager.unsubscribe(project1mod1);
    assertThat(subscriber.count).isEqualTo(1);
  }

  @Test
  public void unsubscribe_non_subscribed_should_do_nothing() {
    notificationsManager.unsubscribe(project1mod1);
    assertThat(subscriber.count).isEqualTo(0);
  }

  @Test
  public void should_not_subscribe_when_notifications_are_disabled() {
    FakeSubscriber disabled = new FakeSubscriber() {
      @Override
      public boolean subscribe(ISonarLintProject project, SonarLintProjectConfiguration config, SonarQubeNotificationListener listener) {
        return false;
      }
    };

    NotificationsManager notificationsManager = new NotificationsManager(disabled, configReader);
    notificationsManager.subscribe(project1mod1, listener);
    notificationsManager.unsubscribe(project1mod1);

    // should be 0 if unsubscribe was correctly ignored, otherwise it would be -1
    assertThat(disabled.count).isEqualTo(0);
  }

  @Test
  public void should_not_subscribe_when_project_key_null() {
    ISonarLintProject moduleWithoutProjectKey = mock(ISonarLintProject.class);
    configs.put(moduleWithoutProjectKey, mock(SonarLintProjectConfiguration.class));
    notificationsManager.subscribe(moduleWithoutProjectKey, listener);
    assertThat(subscriber.count).isEqualTo(0);
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
