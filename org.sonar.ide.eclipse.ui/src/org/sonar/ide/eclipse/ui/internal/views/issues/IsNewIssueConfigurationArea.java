package org.sonar.ide.eclipse.ui.internal.views.issues;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.markers.FilterConfigurationArea;
import org.eclipse.ui.views.markers.MarkerFieldFilter;

public class IsNewIssueConfigurationArea extends FilterConfigurationArea {

  private int newIssues;
  private Button newIssuesButton;
  private Button otherIssuesButton;

  public IsNewIssueConfigurationArea() {
    super();
  }

  public void apply(MarkerFieldFilter filter) {
    ((IsNewIssueFieldFilter) filter).selectedNewIssues = newIssues;

  }

  public void createContents(Composite parent) {

    parent.setLayout(new GridLayout(2, false));

    newIssuesButton = new Button(parent, SWT.CHECK);
    newIssuesButton.setText("New issues");
    newIssuesButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        updateIssueType(IsNewIssueFieldFilter.SHOW_NEW,
            newIssuesButton.getSelection());
      }
    });

    otherIssuesButton = new Button(parent, SWT.CHECK);
    otherIssuesButton.setText("Other issues");
    otherIssuesButton.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        updateIssueType(IsNewIssueFieldFilter.SHOW_OTHER,
            otherIssuesButton.getSelection());
      }
    });
  }

  void updateIssueType(int constant, boolean enabled) {

    if (enabled)
      newIssues = constant | newIssues;
    else newIssues = constant ^ newIssues;

  }

  public void initialize(MarkerFieldFilter filter) {
    if (filter != null) {
      newIssues = ((IsNewIssueFieldFilter) filter).selectedNewIssues;

      otherIssuesButton
          .setSelection((IsNewIssueFieldFilter.SHOW_OTHER & newIssues) > 0);
      newIssuesButton
          .setSelection((IsNewIssueFieldFilter.SHOW_NEW & newIssues) > 0);
    }
  }

  public String getTitle() {
    return "Issue type";
  }

}
