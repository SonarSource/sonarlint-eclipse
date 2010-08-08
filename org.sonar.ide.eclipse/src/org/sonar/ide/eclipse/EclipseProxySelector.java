package org.sonar.ide.eclipse;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;

/**
 * {@link ProxySelector}, which selects the proxy server to use via {@link IProxyService}.
 * 
 * @author Evgeny Mandrikov
 */
public class EclipseProxySelector extends ProxySelector {

  private final IProxyService service;

  public EclipseProxySelector(IProxyService service) {
    this.service = service;
  }

  @Override
  public List<Proxy> select(final URI uri) {
    final ArrayList<Proxy> result = new ArrayList<Proxy>();

    for (IProxyData data : service.select(uri)) {
      if (IProxyData.HTTP_PROXY_TYPE.equals(data.getType())) {
        addProxy(result, Proxy.Type.HTTP, data);
      } else if (IProxyData.HTTPS_PROXY_TYPE.equals(data.getType())) {
        addProxy(result, Proxy.Type.HTTP, data);
      } else if (IProxyData.SOCKS_PROXY_TYPE.equals(data.getType())) {
        addProxy(result, Proxy.Type.SOCKS, data);
      }
    }

    if (result.isEmpty()) {
      result.add(Proxy.NO_PROXY);
    }
    return result;
  }

  private void addProxy(final ArrayList<Proxy> list, final Proxy.Type type, final IProxyData d) {
    try {
      list.add(new Proxy(type, new InetSocketAddress(InetAddress.getByName(d.getHost()), d.getPort())));
    } catch (UnknownHostException uhe) {
      // Oh well.
    }
  }

  @Override
  public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
    // Don't tell Eclipse.
  }

}
