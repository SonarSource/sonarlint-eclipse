/*
 * Sonar Eclipse
 * Copyright (C) 2010-2013 SonarSource
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
package org.sonar.ide.eclipse.core.internal.servers;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.equinox.security.storage.EncodingUtils;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.slf4j.LoggerFactory;
import org.sonar.ide.eclipse.common.servers.ISonarServer;
import org.sonar.ide.eclipse.core.internal.SonarCorePlugin;

import com.google.common.collect.Lists;

public class ServersManager implements ISonarServersManager {
	// Store node
	static final String PREF_SERVERS = "servers";

	// Default preferences keys
	static final String DEFAULT_PREF_SERVERS_PREFIX = "default.servers.";
	static final String DEFAULT_PREF_SERVERS_SUFFIX_URL = ".url";
	static final String DEFAULT_PREF_SERVERS_SUFFIX_AUTH_TYPE = ".authType";
	static final String DEFAULT_PREF_SERVERS_AUTH_TYPE_PROXY = "proxy";

	@Override
	public Collection<ISonarServer> getServers() {
		IEclipsePreferences rootNode = InstanceScope.INSTANCE.getNode(SonarCorePlugin.PLUGIN_ID);
		List<ISonarServer> servers = Lists.newArrayList();
		try {
			rootNode.sync();
			if (rootNode.nodeExists(PREF_SERVERS)) {
				Preferences serversNode = rootNode.node(PREF_SERVERS);
				for (String encodedUrl : serversNode.childrenNames()) {
					Preferences serverNode = serversNode.node(encodedUrl);
					String url = EncodingUtils.decodeSlashes(encodedUrl);
					boolean auth = serverNode.getBoolean("auth", false);
					servers.add(new SonarServer(url, auth));
				}
			} else {
				// Original thread : http://stackoverflow.com/questions/17975557/sonarqube-eclipse-plugin-change-default-server-configuration
				// Some preferences could be defined in ini file, loaded by -pluginCustomization process
				// The ScopedPreferenceStore service could get keys from this default 'scope', but not iterate on node 'servers'
				// So specific default configuration keys should be defined with an incremental id
				// This default(s) server(s) is/are used to initiate AND store preferences (so addServer is used)
				//
				// Exemple to put in ini file loaded by -pluginCustomization process :
				// # SonarQube plugin configuration
				// org.sonar.ide.eclipse.core/default.servers.0.url=http://sonarqube-auth.mycompany.com/
				// org.sonar.ide.eclipse.core/default.servers.0.authType=proxy
				// org.sonar.ide.eclipse.core/default.servers.1.url=http://sonarqube-anonymous.mycompany.com/
				ScopedPreferenceStore sps = new ScopedPreferenceStore(InstanceScope.INSTANCE, SonarCorePlugin.PLUGIN_ID);
				long index = 0;
				while (true) {
					String url = sps.getDefaultString(DEFAULT_PREF_SERVERS_PREFIX + index + DEFAULT_PREF_SERVERS_SUFFIX_URL);

					// Default preferences not used, break to default server (localhost)
					if (StringUtils.isBlank(url)) {
						break;
					}

					String[] creds = new String[2];
					if (DEFAULT_PREF_SERVERS_AUTH_TYPE_PROXY.equals(sps.getDefaultString(DEFAULT_PREF_SERVERS_PREFIX + index
							+ DEFAULT_PREF_SERVERS_SUFFIX_AUTH_TYPE))) {
						creds = getProxyCreds();
					}
					servers.add(addServer(url, creds[0], creds[1]));
					index++;
				}
			}
		} catch (BackingStoreException e) {
			LoggerFactory.getLogger(SecurityManager.class).error(e.getMessage(), e);
		}
		// Defaults
		if (servers.isEmpty()) {
			return Arrays.asList(getDefault());
		}
		return servers;
	}

	/**
	 * Get first login/password defined in eclipse proxy configuration
	 * 
	 * @return login + pass in array
	 */
	private String[] getProxyCreds() {
		BundleContext bc = SonarCorePlugin.getDefault().getBundle().getBundleContext();
		ServiceReference<?> serviceReference = bc.getServiceReference(IProxyService.class.getName());
		IProxyService proxyService = (IProxyService) bc.getService(serviceReference);
		if (proxyService.getProxyData() != null) {
			for (IProxyData pd : proxyService.getProxyData()) {
				if (StringUtils.isNotBlank(pd.getUserId()) && StringUtils.isNotBlank(pd.getPassword())) {
					return new String[] { pd.getUserId(), pd.getPassword() };
				}
			}
		}
		return null;
	}

	@Override
	public ISonarServer addServer(String url, String username, String password) {
		SonarServer server = new SonarServer(url, username, password);
		String encodedUrl = EncodingUtils.encodeSlashes(server.getUrl());
		IEclipsePreferences rootNode = InstanceScope.INSTANCE.getNode(SonarCorePlugin.PLUGIN_ID);
		try {
			Preferences serversNode = rootNode.node(PREF_SERVERS);
			serversNode.put("initialized", "true");
			serversNode.node(encodedUrl).putBoolean("auth", server.hasCredentials());
			serversNode.flush();
		} catch (BackingStoreException e) {
			LoggerFactory.getLogger(SecurityManager.class).error(e.getMessage(), e);
		}
		return server;
	}

	/**
	 * For tests.
	 */
	public void clean() {
		IEclipsePreferences rootNode = InstanceScope.INSTANCE.getNode(SonarCorePlugin.PLUGIN_ID);
		try {
			rootNode.node(PREF_SERVERS).removeNode();
			rootNode.node(PREF_SERVERS).put("initialized", "true");
			rootNode.flush();
		} catch (BackingStoreException e) {
			LoggerFactory.getLogger(SecurityManager.class).error(e.getMessage(), e);
		}
	}

	@Override
	public void removeServer(String url) {
		String encodedUrl = EncodingUtils.encodeSlashes(url);
		IEclipsePreferences rootNode = InstanceScope.INSTANCE.getNode(SonarCorePlugin.PLUGIN_ID);
		try {
			Preferences serversNode = rootNode.node(PREF_SERVERS);
			serversNode.node(encodedUrl).removeNode();
			serversNode.flush();
		} catch (BackingStoreException e) {
			LoggerFactory.getLogger(SecurityManager.class).error(e.getMessage(), e);
		}
	}

	@Override
	public ISonarServer findServer(String url) {
		for (ISonarServer server : getServers()) {
			if (server.getUrl().equals(url)) {
				return server;
			}
		}
		return null;
	}

	@Override
	public ISonarServer getDefault() {
		return new SonarServer("http://localhost:9000");
	}

	@Override
	public ISonarServer create(String location, String username, String password) {
		return new SonarServer(location, username, password);
	}

}
