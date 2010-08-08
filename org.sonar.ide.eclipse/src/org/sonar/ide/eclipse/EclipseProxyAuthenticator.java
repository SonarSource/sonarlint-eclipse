package org.sonar.ide.eclipse;

import java.net.Authenticator;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.net.UnknownHostException;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;

/**
 * {@link Authenticator}, which works via {@link IProxyService}.
 * 
 * @author Evgeny Mandrikov
 */
public class EclipseProxyAuthenticator extends Authenticator {

  private final IProxyService service;

  public EclipseProxyAuthenticator(IProxyService service) {
    this.service = service;
  }

  @Override
  protected PasswordAuthentication getPasswordAuthentication() {
    final IProxyData[] data = service.getProxyData();
    if (data == null) {
      return null;
    }
    for (final IProxyData d : data) {
      if (d.getUserId() == null || d.getHost() == null) {
        continue;
      }
      if (d.getPort() == getRequestingPort() && hostMatches(d)) {
        return auth(d);
      }
    }
    return null;
  }

  private PasswordAuthentication auth(final IProxyData d) {
    final String user = d.getUserId();
    final String pass = d.getPassword();
    final char[] passChar = pass != null ? pass.toCharArray() : new char[0];
    return new PasswordAuthentication(user, passChar);
  }

  private boolean hostMatches(final IProxyData d) {
    try {
      final InetAddress dHost = InetAddress.getByName(d.getHost());
      InetAddress rHost = getRequestingSite();
      if (rHost == null) {
        rHost = InetAddress.getByName(getRequestingHost());
      }
      return dHost.equals(rHost);
    } catch (UnknownHostException err) {
      return false;
    }
  }
}
