package org.sonarlint.eclipse.ui.internal.server;

import org.eclipse.swt.widgets.Composite;
import org.sonarlint.eclipse.core.internal.server.IServer;

/**
 * Provides extra information to the hover over mouse action of a server
 * <p>
 * <b>Provisional API:</b> This class/interface is part of an interim API that is still under development and expected to 
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback 
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken 
 * (repeatedly) as the API evolves.
 * </p>
 */
public interface IServerToolTip {
  /**
   * Allows adopters to add widgets to the tooltip.
   * 
   * @param parent the parent
   * @param server the server
   */
  public void createContent(Composite parent, IServer server);
}
