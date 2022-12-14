/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2022 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.notifications.internal;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class AnimationUtil {

  public static final long FADE_RESCHEDULE_DELAY = 80;

  public static final int FADE_IN_INCREMENT = 15;

  public static final int FADE_OUT_INCREMENT = -20;

  public static FadeJob fastFadeIn(Shell shell, IFadeListener listener) {
    return new FadeJob(shell, 2 * FADE_IN_INCREMENT, FADE_RESCHEDULE_DELAY, listener);
  }

  public static FadeJob fadeIn(Shell shell, IFadeListener listener) {
    return new FadeJob(shell, FADE_IN_INCREMENT, FADE_RESCHEDULE_DELAY, listener);
  }

  public static FadeJob fadeOut(Shell shell, IFadeListener listener) {
    return new FadeJob(shell, FADE_OUT_INCREMENT, FADE_RESCHEDULE_DELAY, listener);
  }

  public static class FadeJob extends Job {

    private final Shell shell;

    private final int increment;

    private volatile boolean stopped;

    private volatile int currentAlpha;

    private final long delay;

    private final IFadeListener fadeListener;

    public FadeJob(Shell shell, int increment, long delay, IFadeListener fadeListener) {
      super(Messages.AnimationUtil_FadeJobTitle);
      if (increment < -255 || increment == 0 || increment > 255) {
        throw new IllegalArgumentException("-255 <= increment <= 255 && increment != 0"); //$NON-NLS-1$
      }
      if (delay < 1) {
        throw new IllegalArgumentException("delay must be > 0"); //$NON-NLS-1$
      }
      this.currentAlpha = shell.getAlpha();
      this.shell = shell;
      this.increment = increment;
      this.delay = delay;
      this.fadeListener = fadeListener;

      setSystem(true);
      schedule(delay);
    }

    @Override
    protected void canceling() {
      this.stopped = true;
    }

    private void reschedule() {
      if (this.stopped) {
        return;
      }
      schedule(this.delay);
    }

    public void cancelAndWait(final boolean setAlpha) {
      if (this.stopped) {
        return;
      }
      cancel();
      Display.getDefault().syncExec(() -> {
        if (setAlpha) {
          FadeJob.this.shell.setAlpha(getLastAlpha());
        }
      });
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
      if (this.stopped) {
        return Status.OK_STATUS;
      }

      this.currentAlpha += this.increment;
      if (this.currentAlpha <= 0) {
        this.currentAlpha = 0;
      } else if (this.currentAlpha >= 255) {
        this.currentAlpha = 255;
      }

      Display.getDefault().syncExec(() -> {
        if (FadeJob.this.stopped) {
          return;
        }

        if (FadeJob.this.shell.isDisposed()) {
          FadeJob.this.stopped = true;
          return;
        }

        FadeJob.this.shell.setAlpha(FadeJob.this.currentAlpha);

        if (FadeJob.this.fadeListener != null) {
          FadeJob.this.fadeListener.faded(FadeJob.this.shell, FadeJob.this.currentAlpha);
        }
      });

      if (this.currentAlpha == 0 || this.currentAlpha == 255) {
        this.stopped = true;
      }

      reschedule();
      return Status.OK_STATUS;
    }

    private int getLastAlpha() {
      return (this.increment < 0) ? 0 : 255;
    }

  }

  public static interface IFadeListener {

    public void faded(Shell shell, int alpha);

  }

}
