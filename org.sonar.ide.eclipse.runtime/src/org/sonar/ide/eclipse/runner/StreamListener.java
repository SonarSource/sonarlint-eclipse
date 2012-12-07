/*
 * Sonar Eclipse
 * Copyright (C) 2010-2012 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.ide.eclipse.runner;

import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IFlushableStreamMonitor;
import org.eclipse.debug.core.model.IStreamMonitor;

/**
 * This class listens to a specified IO stream
 */
public abstract class StreamListener implements IStreamListener {

  private IStreamMonitor fStreamMonitor;

  private boolean fFlushed = false;

  private boolean fListenerRemoved = false;

  public StreamListener(IStreamMonitor monitor) {
    this.fStreamMonitor = monitor;
    fStreamMonitor.addListener(this);
    streamAppended(null, monitor);
  }

  protected abstract void write(String text);

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.debug.core.IStreamListener#streamAppended(java.lang.String,
   * org.eclipse.debug.core.model.IStreamMonitor)
   */
  public final void streamAppended(String text, IStreamMonitor monitor) {
    if (fFlushed) {
      write(text);
    } else {
      String contents = null;
      synchronized (fStreamMonitor) {
        fFlushed = true;
        contents = fStreamMonitor.getContents();
        if (fStreamMonitor instanceof IFlushableStreamMonitor) {
          IFlushableStreamMonitor m = (IFlushableStreamMonitor) fStreamMonitor;
          m.flushContents();
          m.setBuffered(false);
        }
      }
      if (contents != null && contents.length() > 0) {
        write(contents);
      }
    }
  }

  public void closeStream() {
    if (fStreamMonitor == null) {
      return;
    }
    synchronized (fStreamMonitor) {
      fStreamMonitor.removeListener(this);
      if (!fFlushed) {
        String contents = fStreamMonitor.getContents();
        streamAppended(contents, fStreamMonitor);
      }
      fListenerRemoved = true;
    }
  }

  public void dispose() {
    if (!fListenerRemoved) {
      closeStream();
    }
    fStreamMonitor = null;
  }
}
