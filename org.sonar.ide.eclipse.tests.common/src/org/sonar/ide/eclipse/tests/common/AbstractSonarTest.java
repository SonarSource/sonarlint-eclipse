package org.sonar.ide.eclipse.tests.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.mortbay.jetty.testing.ServletTester;
import org.sonar.ide.eclipse.SonarPlugin;
import org.sonar.ide.shared.AbstractResourceUtils;
import org.sonar.wsclient.Host;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.connectors.HttpClient4Connector;

/**
 * Common test case for sonar-ide/eclipse projects.
 * 
 * @author Jérémie Lagarde
 * 
 */
public abstract class AbstractSonarTest extends TestCase {
  protected static final IProgressMonitor monitor = new NullProgressMonitor();
  protected IWorkspace                    workspace;
  protected SonarPlugin                   plugin;
  private SonarTestServer                 testServer;

  protected void setUp() throws Exception {
    super.setUp();
    workspace = ResourcesPlugin.getWorkspace();
    IWorkspaceDescription description = workspace.getDescription();
    description.setAutoBuilding(false);
    workspace.setDescription(description);

    plugin = SonarPlugin.getDefault();
    cleanWorkspace();
  }

  protected String startTestServer() throws Exception {
    if (testServer == null) {
      synchronized (SonarTestServer.class) {
        if (testServer == null) {
          testServer = new SonarTestServer();
          testServer.start();
        }
      }
    }
    return testServer.getBaseUrl();
  }

  protected String addLocalTestServer() throws Exception {
    String url = startTestServer();
    SonarPlugin.getServerManager().createServer(url);
    return url;
  }

  protected void tearDown() throws Exception {
    super.tearDown();

    cleanWorkspace();

    IWorkspaceDescription description = workspace.getDescription();
    description.setAutoBuilding(true);
    workspace.setDescription(description);

    if (testServer != null) {
      testServer.stop();
      testServer = null;
    }

  }

  private void cleanWorkspace() throws Exception {
//    for (Host host : SonarPlugin.getServerManager().getServers()) {
//      SonarPlugin.getServerManager().removeServer(host.getHost());
//    }
    // TODO : cleanWorkspace()
  }

  /**
   * Import test project into the Eclipse workspace
   * 
   * @return created projects
   */
  protected IProject importEclipseProject(String projectdir) throws IOException, CoreException {
    System.out.println("Importing Eclipse project");
    IWorkspaceRoot root = workspace.getRoot();

    File src = new File(projectdir);
    File dst = new File(root.getLocation().toFile(), src.getName());
    copyDirectory(src, dst);

    final IProject project = workspace.getRoot().getProject(src.getName());
    final List<IProject> addedProjectList = new ArrayList<IProject>();

    workspace.run(new IWorkspaceRunnable() {
      public void run(IProgressMonitor monitor) throws CoreException {
        // create project as java project
        if (!project.exists()) {
          IProjectDescription projectDescription = workspace.newProjectDescription(project.getName());
          projectDescription.setLocation(null);
          project.create(projectDescription, monitor);
          project.open(IResource.NONE, monitor);
        } else {
          project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
        }
        addedProjectList.add(project);
      }
    }, workspace.getRoot(), IWorkspace.AVOID_UPDATE, monitor);
    System.out.println("Eclipse project imported");
    return addedProjectList.get(0);
  }

  private void copyDirectory(File sourceLocation, File targetLocation) throws IOException {
    if (sourceLocation.getName().contains(".svn"))
      return;
    if (sourceLocation.isDirectory()) {
      if (!targetLocation.exists()) {
        targetLocation.mkdir();
      }

      String[] children = sourceLocation.list();
      for (int i = 0; i < children.length; i++) {
        copyDirectory(new File(sourceLocation, children[i]), new File(targetLocation, children[i]));
      }
    } else {

      InputStream in = new FileInputStream(sourceLocation);
      OutputStream out = new FileOutputStream(targetLocation);
      byte[] buf = new byte[1024];
      int len;
      while ((len = in.read(buf)) > 0) {
        out.write(buf, 0, len);
      }
      in.close();
      out.close();
    }
  }

  // =========================================================================
  // == TODO : Use org.sonar.ide.commons.tests ==
  // == Duplicate code from sonar-ide-commons/src/test/java ==

  /**
   * @author Evgeny Mandrikov
   */
  public class SonarTestServer {
    private ServletTester tester;
    private String        baseUrl;

    public void start() throws Exception {
      tester = new ServletTester();
      tester.setContextPath("/");
      tester.addServlet(VersionServlet.class, "/api/server/version");
      tester.addServlet(ViolationServlet.class, "/api/violations");
      tester.addServlet(SourceServlet.class, "/api/sources");

      baseUrl = tester.createSocketConnector(true);
      tester.start();
    }

    public void stop() throws Exception {
      tester.stop();
    }

    public String getBaseUrl() {
      return baseUrl;
    }

    public Sonar getSonar() {
      HttpClient4Connector connector = new HttpClient4Connector(new Host(getBaseUrl()));
      return new Sonar(connector);
    }
  }

  /**
   * @author Evgeny Mandrikov
   */
  public abstract class TestServlet extends GenericServlet {
    private static final long serialVersionUID = 1L;

    protected String getClassKey(ServletRequest request) {
      String resourceKey = request.getParameter("resource");
      String[] parts = resourceKey.split(":");
      // String groupId = parts[0];
      // String artifactId = parts[1];
      String classKey = parts[2];
      if (classKey.startsWith(AbstractResourceUtils.DEFAULT_PACKAGE_NAME)) {
        classKey = StringUtils.substringAfter(classKey, ".");
      }
      return classKey;
    }

    protected abstract String getResource(String classKey);

    @Override
    public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
      PrintWriter out = response.getWriter();
      String json;
      try {
        String classKey = getClassKey(request);
        json = IOUtils.toString(ViolationServlet.class.getResourceAsStream(getResource(classKey)));
      } catch (Exception e) {
        json = "[]";
      }
      out.println(json);
    }
  }

  public class VersionServlet extends GenericServlet {
    private static final long serialVersionUID = 1L;

    @Override
    public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
      PrintWriter out = response.getWriter();
      String json = "{\"id\":\"20100207124430\", \"version\":\"2.0\"}";
      out.println(json);
    }
  }

  public class ViolationServlet extends TestServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected String getResource(String classKey) {
      return "/violations/" + classKey + ".json";
    }
  }

  public class SourceServlet extends TestServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected String getResource(String classKey) {
      return "/sources/" + classKey + ".json";
    }
  }

}
