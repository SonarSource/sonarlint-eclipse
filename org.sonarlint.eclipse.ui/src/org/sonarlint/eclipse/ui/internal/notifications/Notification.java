package org.sonarlint.eclipse.ui.internal.notifications;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.notifications.NotificationPopup;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.sonarlint.eclipse.core.internal.SonarLintCorePlugin;
import org.sonarlint.eclipse.core.internal.preferences.SonarLintProjectConfiguration;
import org.sonarlint.eclipse.core.resource.ISonarLintProject;
import org.sonarlint.eclipse.ui.internal.SonarLintImages;
import org.sonarlint.eclipse.ui.internal.util.BrowserUtils;

public class Notification {
  public static Notification newNotification() {
    return new Notification();
  }

  private Image icon = SonarLintImages.BALLOON_IMG;
  private String title;
  private Duration autoCloseAfterDuration;
  private String bodyText = "";
  private boolean fadingEnabled;
  private List<Action> actions = new ArrayList<>();

  public Notification setTitle(String title) {
    this.title = title;
    return this;
  }

  public Notification setIcon(Image icon) {
    this.icon = icon;
    return this;
  }

  public Notification setBody(String bodyText) {
    this.bodyText = bodyText;
    return this;
  }

  public Notification addLearnMoreLink(String url) {
    return addLink("Learn more", url);
  }

  public Notification addLink(String text, String url) {
    return addLinks(text, List.of(url));
  }

  public Notification addLink(String text, String url, Runnable onClick) {
    return addLinks(text, List.of(url), onClick);
  }

  public Notification addLinks(String text, List<String> urls) {
    return addLinks(text, urls, () -> {
    });
  }

  public Notification addLinks(String text, List<String> urls, Runnable onClick) {
    return addAction(text, false, s -> {
      for (var url : urls) {
        BrowserUtils.openExternalBrowser(url, s.getDisplay());
      }
    });
  }

  public Notification addDoNotAskAgainAction(Runnable onClick) {
    return addAction("Don't ask again", s -> onClick.run());
  }

  public Notification addDoNotShowAgainAction(Runnable onClick) {
    return addAction("Don't show again", s -> onClick.run());
  }

  public Notification addDoNotAskAgainAction(ISonarLintProject project, Consumer<SonarLintProjectConfiguration> callback) {
    return addDoNotAskAgainAction(List.of(project), callback);
  }

  public Notification addDoNotAskAgainAction(Collection<ISonarLintProject> projects, Consumer<SonarLintProjectConfiguration> callback) {
    return addAction("Don't ask again", s -> {
      projects.forEach(p -> {
        var config = SonarLintCorePlugin.loadConfig(p);
        callback.accept(config);
        SonarLintCorePlugin.saveConfig(p, config);
      });
    });
  }

  public Notification addAction(String text, Consumer<Shell> onClick) {
    return addAction(text, true, onClick);
  }

  public Notification addAction(String text, boolean shouldClosePopup, Consumer<Shell> onClick) {
    return addActionWithTooltip(text, null, shouldClosePopup, onClick);
  }

  public Notification addActionWithTooltip(String text, @Nullable String tooltip, Consumer<Shell> onClick) {
    return addActionWithTooltip(text, tooltip, true, onClick);
  }

  public Notification addActionWithTooltip(String text, @Nullable String tooltip, boolean shouldClosePopup, Consumer<Shell> onClick) {
    this.actions.add(new Action(text, tooltip, shouldClosePopup, onClick));
    return this;
  }

  public Notification setAutoCloseAfter(Duration autoCloseAfterDuration) {
    this.autoCloseAfterDuration = autoCloseAfterDuration;
    return this;
  }

  public Notification setFadingEnabled(boolean fadingEnabled) {
    this.fadingEnabled = fadingEnabled;
    return this;
  }

  public void show() {
    Display.getDefault().asyncExec(() -> {
      showInUiThread();
    });
  }

  private void showInUiThread() {
    var currentShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
    // small hack to be able to close the popup from the action
    var popupRef = new NotificationPopup[1];
    var popup = NotificationPopup.forShell(currentShell)
      .titleImage(icon)
      .title(title, true)
      .delay(autoCloseAfterDuration == null ? 0 : autoCloseAfterDuration.toMillis()).fadeIn(true)
      .fadeIn(fadingEnabled)
      .content(parent -> {
        var contentComposite = new Composite(parent, SWT.NONE);
        var messageLabel = new Label(contentComposite, SWT.WRAP);
        var messageLayoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
        messageLabel.setLayoutData(messageLayoutData);
        messageLabel.setText(bodyText);

        var actionsContainer = new Composite(contentComposite, SWT.NONE);
        var actionsLayoutData = new GridData(SWT.FILL, SWT.FILL, true, false);
        actionsLayoutData.horizontalAlignment = SWT.END;
        actionsLayoutData.verticalAlignment = SWT.BOTTOM;
        this.actions.forEach(action -> {
          var detailsLink = new Link(actionsContainer, SWT.NONE);
          detailsLink.setText("<a>" + action.text + "</a>");
          if (action.tooltip != null) {
            detailsLink.setToolTipText(action.tooltip);
          }
          detailsLink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
              action.onClick.accept(e.display.getActiveShell());
              if (action.shouldClosePopup) {
                popupRef[0].close();
              }
            }
          });
        });

        actionsContainer.setLayoutData(actionsLayoutData);

        var rowLayout = new RowLayout();
        rowLayout.spacing = 20;
        actionsContainer.setLayout(rowLayout);
        return contentComposite;
      }).build();
    popupRef[0] = popup;
    popup.open();
  }

  private static class Action {
    private final String text;
    @Nullable
    private final String tooltip;
    private final Consumer<Shell> onClick;
    private final boolean shouldClosePopup;

    private Action(String text, @Nullable String tooltip, boolean shouldClosePopup, Consumer<Shell> onClick) {
      this.text = text;
      this.tooltip = tooltip;
      this.shouldClosePopup = shouldClosePopup;
      this.onClick = onClick;
    }
  }
}
