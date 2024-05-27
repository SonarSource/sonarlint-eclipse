/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2024 SonarSource SA
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
package org.sonarlint.eclipse.core.internal.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class DigestUtils {

  private static final char[] DIGITS = "0123456789abcdef".toCharArray();

  private static final MessageDigest MD5_DIGEST = DigestUtils.getMd5Digest();

  private DigestUtils() {
    // utility class, forbidden constructor
  }

  public static String digest(String content) {
    return encodeHexString(MD5_DIGEST.digest(content.replaceAll("[\\s]", "").getBytes(UTF_8)));
  }

  private static MessageDigest getMd5Digest() {
    return getDigest("MD5");
  }

  private static MessageDigest getDigest(String algorithm) {
    try {
      return MessageDigest.getInstance(algorithm);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalArgumentException(e);
    }
  }

  private static String encodeHexString(byte[] data) {
    var length = data.length;
    var out = new char[length << 1];

    for (int i = 0, j = 0; i < length; ++i, j += 2) {
      out[j] = DIGITS[(240 & data[i]) >>> 4];
      out[j + 1] = DIGITS[15 & data[i]];
    }

    return new String(out);
  }
}
