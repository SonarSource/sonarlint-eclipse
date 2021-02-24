/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2021 SonarSource SA
 * sonarlint@sonarsource.com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarlint.eclipse.core.resource;

import java.util.Collection;
import java.util.function.Supplier;

/**
 * Interface for the extension point <code>org.sonarlint.eclipse.core.projectsProvider</code>.
 * Returned projects will be used in addition of defaults (ie IProject) when SonarLint need to collect
 * the list of all {@link ISonarLintProject}.
 * @since 3.0
 */
public interface ISonarLintProjectsProvider extends Supplier<Collection<ISonarLintProject>> {

}
