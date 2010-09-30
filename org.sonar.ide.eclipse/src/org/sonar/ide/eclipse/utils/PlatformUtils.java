package org.sonar.ide.eclipse.utils;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.sonar.ide.eclipse.core.SonarLogger;

@SuppressWarnings("unchecked")
public class PlatformUtils {

  public static <T> T adapt(Object object, Class<T> cls) {
    if (cls.isInstance(object)) {
      return (T) object;
    }
    T result = null;
    if (object instanceof IAdaptable) {
      result = (T) ((IAdaptable) object).getAdapter(cls);
    }
    if (result == null) {
      // From IAdapterManager :
      // this method should be used judiciously, in order to avoid unnecessary plug-in activations
      result = (T) Platform.getAdapterManager().loadAdapter(object, cls.getName());
    }
    return result;
  }

  public static void openEditor(IFile file) {
    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(file.getName());
    try {
      page.openEditor(new FileEditorInput(file), desc.getId());
    } catch (PartInitException e) {
      SonarLogger.log(e);
    }
  }

}
