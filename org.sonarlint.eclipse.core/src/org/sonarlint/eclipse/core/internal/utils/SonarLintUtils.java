/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2021 SonarSource SA
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

import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.resources.IResource;
import org.sonarlint.eclipse.core.analysis.IAnalysisConfigurator;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.extension.SonarLintExtensionTracker;
import org.sonarsource.sonarlint.core.client.api.common.Language;

import static org.sonarlint.eclipse.core.internal.utils.StringUtils.defaultString;

public class SonarLintUtils {

  private static final String PROXY_AUTHORIZATION = "Proxy-Authorization";

  private SonarLintUtils() {
    // utility class, forbidden constructor
  }

  public static boolean isSonarLintFileCandidate(IResource resource) {
    if (!resource.exists() || resource.isDerived(IResource.CHECK_ANCESTORS) || resource.isHidden(IResource.CHECK_ANCESTORS)) {
      return false;
    }
    // Ignore .project, .settings, that are not considered hidden on Windows...
    // Also ignore .class (SLE-65)
    if (resource.getName().startsWith(".") || "class".equals(resource.getFileExtension())) {
      return false;
    }
    return true;
  }

  public static String getPluginVersion() {
    return SonarLintCorePlugin.getInstance().getBundle().getVersion().toString();
  }

  public static OkHttpClient.Builder withProxy(String url, OkHttpClient baseClient) {
    Builder newBuilder = baseClient.newBuilder()
      .connectTimeout(30, TimeUnit.SECONDS)
      .readTimeout(10, TimeUnit.MINUTES);
    IProxyService proxyService = SonarLintCorePlugin.getInstance().getProxyService();
    IProxyData[] proxyDataForHost;
    try {
      proxyDataForHost = proxyService.select(new URL(url).toURI());
    } catch (MalformedURLException | URISyntaxException e) {
      throw new IllegalStateException("Invalid URL for server: " + url, e);
    }
    if (proxyDataForHost.length > 0) {
      IProxyData proxyData = proxyDataForHost[0];
      if (proxyData.getHost() != null) {
        Type proxyType = IProxyData.SOCKS_PROXY_TYPE.equals(proxyData.getType()) ? Proxy.Type.SOCKS : Proxy.Type.HTTP;
        newBuilder.proxy(new Proxy(proxyType, new InetSocketAddress(proxyData.getHost(), proxyData.getPort())));
        if (proxyData.isRequiresAuthentication()) {
          newBuilder.proxyAuthenticator((route, response) -> {
            if (response.request().header(PROXY_AUTHORIZATION) != null) {
              // Give up, we've already attempted to authenticate.
              return null;
            }
            String proxyCrendentials = Credentials.basic(defaultString(proxyData.getUserId(), ""), defaultString(proxyData.getPassword(), ""));
            return response.request().newBuilder()
              .header(PROXY_AUTHORIZATION, proxyCrendentials)
              .build();
          });
        }
      }
    }

    return newBuilder;
  }

  public static Set<Language> getEnabledLanguages() {
    EnumSet<Language> languagesDisabledByDefault = EnumSet.of(Language.TS, Language.JAVA, Language.CPP, Language.C, Language.OBJC, Language.SWIFT);
    EnumSet<Language> enabledLanguages = EnumSet.complementOf(languagesDisabledByDefault);
    Collection<IAnalysisConfigurator> configurators = SonarLintExtensionTracker.getInstance().getAnalysisConfigurators();
    for (IAnalysisConfigurator configurator : configurators) {
      enabledLanguages.addAll(configurator.whitelistedLanguages());
    }
    return enabledLanguages;
  }

  public static Optional<Integer> getPlatformPid() {
    try {
      return Optional.of(getJvmPidForJava9Plus());
    } catch(IllegalStateException e) {
      return Optional.empty();
    }
  }

  private static int getJvmPidForJava9Plus() {
    try {
      Class<?> processHandleClass = Class.forName("java.lang.ProcessHandle");
      Object handle = processHandleClass.getMethod("current").invoke(null);
      return ((Long) processHandleClass.getMethod("pid").invoke(handle)).intValue();
    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException | ClassNotFoundException e) {
      throw new IllegalStateException("Could not get PID through process handle", e);
    }
  }
}
