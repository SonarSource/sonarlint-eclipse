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
import org.sonarlint.eclipse.ui.internal.popup.NoAutomaticBuildWarningPopup;
import org.sonarlint.eclipse.ui.internal.popup.ReleaseNotesPopup;

import static org.assertj.core.api.Assertions.assertThat;

public class PopupUtilsTest {

  @After
  public void tearDown() {
    PopupUtils.removeCurrentlyDisplayedPopup(NoAutomaticBuildWarningPopup.class);
    PopupUtils.removeCurrentlyDisplayedPopup(ReleaseNotesPopup.class);
  }

  @Test
  public void does_not_open_popup_twice_when_scheduled_repeatedly_before_ui_thread_runs() {
    var pending = new ArrayList<Runnable>();
    Consumer<Runnable> fakeAsyncExec = pending::add;
    var shown = new AtomicInteger();

    for (var i = 0; i < 5; i++) {
      PopupUtils.scheduleAsyncDisplay(NoAutomaticBuildWarningPopup.class, () -> true, shown::incrementAndGet, fakeAsyncExec);
    }

    assertThat(pending).hasSize(5);
    pending.forEach(Runnable::run);

    assertThat(shown.get()).isEqualTo(1);
    assertThat(PopupUtils.popupCurrentlyDisplayed(NoAutomaticBuildWarningPopup.class)).isTrue();
  }

  @Test
  public void does_not_schedule_when_popup_already_displayed() {
    var pending = new ArrayList<Runnable>();
    var shown = new AtomicInteger();

    PopupUtils.addCurrentlyDisplayedPopup(NoAutomaticBuildWarningPopup.class);
    PopupUtils.scheduleAsyncDisplay(NoAutomaticBuildWarningPopup.class, () -> true, shown::incrementAndGet, pending::add);

    assertThat(pending).isEmpty();
    assertThat(shown.get()).isZero();
  }

  @Test
  public void does_not_schedule_when_should_show_is_false() {
    var pending = new ArrayList<Runnable>();
    var shown = new AtomicInteger();

    PopupUtils.scheduleAsyncDisplay(NoAutomaticBuildWarningPopup.class, () -> false, shown::incrementAndGet, pending::add);

    assertThat(pending).isEmpty();
    assertThat(shown.get()).isZero();
  }

  @Test
  public void does_not_open_when_should_show_becomes_false_before_ui_thread_runs() {
    var pending = new ArrayList<Runnable>();
    var shown = new AtomicInteger();
    var shouldShow = new AtomicBoolean(true);

    PopupUtils.scheduleAsyncDisplay(NoAutomaticBuildWarningPopup.class, shouldShow::get, shown::incrementAndGet, pending::add);

    shouldShow.set(false);
    pending.forEach(Runnable::run);

    assertThat(shown.get()).isZero();
    assertThat(PopupUtils.popupCurrentlyDisplayed(NoAutomaticBuildWarningPopup.class)).isFalse();
  }

  @Test
  public void opens_popup_when_conditions_still_hold() {
    var pending = new ArrayList<Runnable>();
    var shown = new AtomicInteger();

    PopupUtils.scheduleAsyncDisplay(NoAutomaticBuildWarningPopup.class, () -> true, shown::incrementAndGet, pending::add);
    pending.forEach(Runnable::run);

    assertThat(shown.get()).isEqualTo(1);
    assertThat(PopupUtils.popupCurrentlyDisplayed(NoAutomaticBuildWarningPopup.class)).isTrue();
  }

  @Test
  public void can_show_different_popup_types_independently() {
    var pending = new ArrayList<Runnable>();
    var shownAutobuild = new AtomicInteger();
    var shownReleaseNotes = new AtomicInteger();

    PopupUtils.scheduleAsyncDisplay(NoAutomaticBuildWarningPopup.class, () -> true, shownAutobuild::incrementAndGet, pending::add);
    PopupUtils.scheduleAsyncDisplay(ReleaseNotesPopup.class, () -> true, shownReleaseNotes::incrementAndGet, pending::add);
    pending.forEach(Runnable::run);

    assertThat(shownAutobuild.get()).isEqualTo(1);
    assertThat(shownReleaseNotes.get()).isEqualTo(1);
  }
}
