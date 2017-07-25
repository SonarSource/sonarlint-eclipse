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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;

public class NotificationsTracker {

  private ZonedDateTime lastEventPolling;

  private final Path lastEventPollingPath;

  public NotificationsTracker(Path basedir) {
    if (!basedir.toFile().isDirectory()) {
      try {
        Files.createDirectories(basedir);
      } catch (IOException e) {
        // ignore
      }
    }
    lastEventPollingPath = basedir.resolve("lastEventPolling.obj");
  }

  synchronized ZonedDateTime getLastEventPolling() {
    if (lastEventPolling == null) {
      lastEventPolling = readFromFile();
      if (lastEventPolling == null) {
        lastEventPolling = ZonedDateTime.now();
      }
    }
    return lastEventPolling;
  }

  synchronized void setLastEventPolling(ZonedDateTime time) {
    lastEventPolling = time;
    writeToFile(time);
  }

  private ZonedDateTime readFromFile() {
    if (!lastEventPollingPath.toFile().isFile()) {
      return null;
    }
    try (
      FileInputStream fis = new FileInputStream(lastEventPollingPath.toFile());
      ObjectInputStream ois = new ObjectInputStream(fis)) {
      return (ZonedDateTime) ois.readObject();
    } catch (Exception e) {
      // ignore
    }
    return null;
  }

  private void writeToFile(ZonedDateTime time) {
    try (
      FileOutputStream fos = new FileOutputStream(lastEventPollingPath.toFile());
      ObjectOutputStream oos = new ObjectOutputStream(fos)) {
      oos.writeObject(time);
    } catch (Exception e) {
      // ignore
    }
  }
}
