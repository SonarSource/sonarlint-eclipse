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
package org.sonarlint.eclipse.core.internal.http;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import org.eclipse.jdt.annotation.Nullable;
import org.sonarlint.eclipse.core.SonarLintLogger;
import org.sonarsource.sonarlint.core.rpc.protocol.backend.initialize.HttpConfigurationDto;

/**
 *  Very bare bones implementation of a HTTP client that can work in the context of SonarLint both with a configurable
 *  key and trust store. Additionally, timeouts can be provided that will used the same way as SLCORE would use them.
 */
public class HttpClient {
  @Nullable
  private SSLContext context;

  @Nullable
  private final Duration connectTimeout;
  @Nullable
  private final Duration connectRequestTimeout;

  public HttpClient(HttpConfigurationDto config) {
    var sslConfig = config.getSslConfiguration();
    connectTimeout = config.getConnectTimeout();
    connectRequestTimeout = config.getConnectionRequestTimeout();

    try {
      var keyManagers = getKeyManagers(sslConfig.getKeyStorePath(), sslConfig.getKeyStorePassword(), sslConfig.getKeyStoreType());
      var trustManagers = getTrustManagers(sslConfig.getTrustStorePath(), sslConfig.getTrustStorePassword(), sslConfig.getTrustStoreType());
      if (keyManagers.length > 0 || trustManagers.length > 0) {
        context = SSLContext.getInstance("TLS");
        context.init(
          getKeyManagers(sslConfig.getKeyStorePath(), sslConfig.getKeyStorePassword(), sslConfig.getKeyStoreType()),
          getTrustManagers(sslConfig.getTrustStorePath(), sslConfig.getTrustStorePassword(), sslConfig.getTrustStoreType()),
          null);
      } else {
        SonarLintLogger.get().debug("No SSL context was initialized due no key or trust manager provided");
      }
    } catch (KeyManagementException | NoSuchAlgorithmException err) {
      SonarLintLogger.get().error("Cannot create SSL context for TLS and initialize it with configuration provided!",
        err);
      context = null;
    }
  }

  @Nullable
  public SSLContext getContext() {
    return context;
  }

  /**
   *  This makes a HTTP GET request to the specified website.
   *
   *  @param uri of website to make a HTTP GET request to
   *  @return site body when the request was successful, null otherwise
   */
  @Nullable
  public String getWebsiteContent(String uri) {
    try {
      var clientBuilder = java.net.http.HttpClient.newBuilder();
      if (context != null) {
        clientBuilder = clientBuilder.sslContext(context);
      }
      if (connectTimeout != null) {
        clientBuilder = clientBuilder.connectTimeout(connectTimeout);
      }
      var client = clientBuilder.build();

      var requestBuilder = HttpRequest.newBuilder().uri(new URI(uri));
      if (connectRequestTimeout != null) {
        requestBuilder = requestBuilder.timeout(connectRequestTimeout);
      }
      var request = requestBuilder.build();

      var response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        SonarLintLogger.get().debug("Accessing '" + uri + "' returned the following status code: "
          + response.statusCode());
        return null;
      }
      return response.body();
    } catch (Exception err) {
      SonarLintLogger.get().error("Unable to make HTTP request to '" + uri + "'", err);
    }

    return null;
  }

  /**
   *  Based on the {@link HttpConfigurationDto} provided, get all trust managers.
   *
   *  @param path (optional) path to the key store
   *  @param password (optional) password to the key store
   *  @param type (optional) type of the key store
   *  @return all trust managers
   */
  private static TrustManager[] getTrustManagers(@Nullable Path path, @Nullable String password,
    @Nullable String type) {
    try {
      var factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

      if (path != null && password != null) {
        var store = KeyStore.getInstance(type != null ? type : KeyStore.getDefaultType());
        try (var is = Files.newInputStream(path, StandardOpenOption.READ)) {
          store.load(is, password.toCharArray());
          factory.init(store);
        }

        return factory.getTrustManagers();
      }
    } catch (Exception err) {
      // INFO: Never display the password!
      SonarLintLogger.get().error("Cannot access and enhance the trust managers based on the configuration provided: "
        + path + ", "
        + (password == null ? null : "<Password hidden>") + ", "
        + type,
        err);
    }

    return new TrustManager[] {};
  }

  /**
   *  Based on the {@link HttpConfigurationDto} provided, get all key managers.
   *
   *  @param path (optional) path to the key store
   *  @param password (optional) password to the key store
   *  @param type (optional) type of the key store
   *  @return all key managers
   */
  private static KeyManager[] getKeyManagers(@Nullable Path path, @Nullable String password, @Nullable String type) {
    try {
      var factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

      if (path != null && password != null) {
        var store = KeyStore.getInstance(type != null ? type : KeyStore.getDefaultType());
        try (var is = Files.newInputStream(path, StandardOpenOption.READ)) {
          store.load(is, password.toCharArray());
          factory.init(store, password.toCharArray());
        }

        return factory.getKeyManagers();
      }
    } catch (Exception err) {
      // INFO: Never display the password!
      SonarLintLogger.get().error("Cannot access and enhance the key managers based on the configuration provided: "
        + path + ", "
        + (password == null ? null : "<Password hidden>") + ", "
        + type,
        err);
    }

    return new KeyManager[] {};
  }
}
