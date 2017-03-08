package org.sonarlint.eclipse.core.resource;

import java.util.function.Predicate;
import org.eclipse.core.resources.IFile;

public interface ISonarLintFileFilter extends Predicate<IFile> {

}
