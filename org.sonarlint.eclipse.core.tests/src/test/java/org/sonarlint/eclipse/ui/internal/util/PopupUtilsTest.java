/*
 * SonarLint for Eclipse
 * Copyright (C) SonarSource Sàrl
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
package org.sonarlint.eclipse.ui.internal.util;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.junit.After;
import org.junit.Test;
import org.sonarlint.eclipse.ui.internal.notifications.AbstractNotificationPopup;

import static org.assertj.core.api.Assertions.assertThat;

public class PopupUtilsTest {

  @After
  public void tearDown() {
    PopupUtils.removeCurrentlyDisplayedPopup(TestPopupA.class);
    PopupUtils.removeCurrentlyDisplayedPopup(TestPopupB.class);
  }

  @Test
  public void does_not_open_popup_twice_when_scheduled_repeatedly_before_ui_thread_runs() {
    var pending = new ArrayList<Runnable>();
    Consumer<Runnable> fakeAsyncExec = pending::add;
    var shown = new AtomicInteger();

    for (var i = 0; i < 5; i++) {
      PopupUtils.scheduleAsyncDisplay(TestPopupA.class, () -> true, shown::incrementAndGet, fakeAsyncExec);
    }

    assertThat(pending).hasSize(5);
    pending.forEach(Runnable::run);

    assertThat(shown.get()).isEqualTo(1);
    assertThat(PopupUtils.popupCurrentlyDisplayed(TestPopupA.class)).isTrue();
  }

  @Test
  public void does_not_schedule_when_popup_already_displayed() {
    var pending = new ArrayList<Runnable>();
    var shown = new AtomicInteger();

    PopupUtils.addCurrentlyDisplayedPopup(TestPopupA.class);
    PopupUtils.scheduleAsyncDisplay(TestPopupA.class, () -> true, shown::incrementAndGet, pending::add);

    assertThat(pending).isEmpty();
    assertThat(shown.get()).isZero();
  }

  @Test
  public void does_not_schedule_when_should_show_is_false() {
    var pending = new ArrayList<Runnable>();
    var shown = new AtomicInteger();

    PopupUtils.scheduleAsyncDisplay(TestPopupA.class, () -> false, shown::incrementAndGet, pending::add);

    assertThat(pending).isEmpty();
    assertThat(shown.get()).isZero();
  }

  @Test
  public void does_not_open_when_should_show_becomes_false_before_ui_thread_runs() {
    var pending = new ArrayList<Runnable>();
    var shown = new AtomicInteger();
    var shouldShow = new AtomicBoolean(true);

    PopupUtils.scheduleAsyncDisplay(TestPopupA.class, shouldShow::get, shown::incrementAndGet, pending::add);

    shouldShow.set(false);
    pending.forEach(Runnable::run);

    assertThat(shown.get()).isZero();
    assertThat(PopupUtils.popupCurrentlyDisplayed(TestPopupA.class)).isFalse();
  }

  @Test
  public void opens_popup_when_conditions_still_hold() {
    var pending = new ArrayList<Runnable>();
    var shown = new AtomicInteger();

    PopupUtils.scheduleAsyncDisplay(TestPopupA.class, () -> true, shown::incrementAndGet, pending::add);
    pending.forEach(Runnable::run);

    assertThat(shown.get()).isEqualTo(1);
    assertThat(PopupUtils.popupCurrentlyDisplayed(TestPopupA.class)).isTrue();
  }

  @Test
  public void can_show_different_popup_types_independently() {
    var pending = new ArrayList<Runnable>();
    var shownA = new AtomicInteger();
    var shownB = new AtomicInteger();

    PopupUtils.scheduleAsyncDisplay(TestPopupA.class, () -> true, shownA::incrementAndGet, pending::add);
    PopupUtils.scheduleAsyncDisplay(TestPopupB.class, () -> true, shownB::incrementAndGet, pending::add);
    pending.forEach(Runnable::run);

    assertThat(shownA.get()).isEqualTo(1);
    assertThat(shownB.get()).isEqualTo(1);
  }

  /** Type tokens only — never instantiated, so they cannot collide with production popup state. */
  private static final class TestPopupA extends AbstractNotificationPopup {
    private TestPopupA() {
      super(null);
    }
  }

  private static final class TestPopupB extends AbstractNotificationPopup {
    private TestPopupB() {
      super(null);
    }
  }
}
