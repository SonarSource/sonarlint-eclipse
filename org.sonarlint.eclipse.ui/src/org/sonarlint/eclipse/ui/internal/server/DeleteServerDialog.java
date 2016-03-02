package org.sonarlint.eclipse.ui.internal.server;

import java.util.List;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.sonarlint.eclipse.core.internal.server.IServer;
import org.sonarlint.eclipse.ui.internal.Messages;
import org.sonarlint.eclipse.ui.internal.SonarLintUiPlugin;

/**
 * Dialog that prompts a user to delete server(s).
 */
public class DeleteServerDialog extends MessageDialog {
  protected List<IServer> servers;

  /**
   * DeleteServerDialog constructor comment.
   * 
   * @param parentShell a shell
   * @param servers an array of servers
   * @param configs an array of server configurations
   */
  public DeleteServerDialog(Shell parentShell, List<IServer> servers) {
    super(parentShell, Messages.deleteServerDialogTitle, null, null, QUESTION, new String[] {IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL}, 0);

    if (servers == null) {
      throw new IllegalArgumentException();
    }

    this.servers = servers;

    if (servers.size() == 1) {
      message = NLS.bind(Messages.deleteServerDialogMessage, servers.get(0).getName());
    } else {
      message = NLS.bind(Messages.deleteServerDialogMessageMany, Integer.toString(servers.size()));
    }
  }

  @Override
  protected void buttonPressed(int buttonId) {
    if (buttonId == OK) {

      Job job = new Job(Messages.deleteServerTask) {
        @Override
        protected IStatus run(IProgressMonitor monitor) {
          if (servers.isEmpty()) {
            // all servers have been deleted from list
            return Status.OK_STATUS;
          }
          try {
            if (monitor.isCanceled()) {
              return Status.CANCEL_STATUS;
            }

            for (IServer server : servers) {
              server.delete();
            }

            if (monitor.isCanceled()) {
              return Status.CANCEL_STATUS;
            }
          } catch (Exception e) {
            return new Status(IStatus.ERROR, SonarLintUiPlugin.PLUGIN_ID, 0, e.getMessage(), e);
          }

          return Status.OK_STATUS;
        }
      };

      job.setPriority(Job.BUILD);

      job.schedule();
    }
    super.buttonPressed(buttonId);
  }
}
