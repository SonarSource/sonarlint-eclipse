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
package org.sonarlint.eclipse.core.internal.notifications;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.eclipse.jdt.annotation.Nullable;

public class NotificationsTracker {

  // visible for testing
  public static final String FILENAME = "lastEventPolling.data";

  @Nullable
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
    lastEventPollingPath = basedir.resolve(FILENAME);
  }

  // visible for testing
  public synchronized ZonedDateTime getLastEventPolling() {
    if (lastEventPolling == null) {
      lastEventPolling = readFromFile();
      if (lastEventPolling == null) {
        lastEventPolling = ZonedDateTime.now();
      }
    }
    return lastEventPolling;
  }

  // visible for testing
  public synchronized void setLastEventPolling(ZonedDateTime time) {
    lastEventPolling = time;
    writeToFile(time);
  }

  public synchronized void updateLastEventPolling(ZonedDateTime time) {
    synchronized (this) {
      // this could be false if the settings changed between the read and write
      if (lastEventPolling == null || time.isAfter(lastEventPolling)) {
        setLastEventPolling(time);
      }
    }
  }

  @Nullable
  private ZonedDateTime readFromFile() {
    if (!lastEventPollingPath.toFile().isFile()) {
      return null;
    }
    try {
      var millis = Long.parseLong(new String(Files.readAllBytes(lastEventPollingPath), Charset.defaultCharset()));
      return ZonedDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
    } catch (IOException | NumberFormatException e) {
      // ignore
    }
    return null;
  }

  private void writeToFile(ZonedDateTime time) {
    try {
      Files.write(lastEventPollingPath, Long.toString(time.toInstant().toEpochMilli()).getBytes(Charset.defaultCharset()));
    } catch (IOException e) {
      // ignore
    }
  }
}
