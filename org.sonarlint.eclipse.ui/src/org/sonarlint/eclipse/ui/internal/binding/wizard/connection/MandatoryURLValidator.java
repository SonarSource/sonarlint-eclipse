/*
 * SonarLint for Eclipse
 * Copyright (C) 2015-2024 SonarSource SA
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
package org.sonarlint.eclipse.ui.internal.binding.wizard.connection;

import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.sonarlint.eclipse.core.internal.utils.StringUtils;

public class MandatoryURLValidator implements IValidator {

  /**      
  * validates the given string is a URL. Validation passes only if the given value is      
  * not <tt>null</tt> and it is a valid URL      
  */
  public IStatus validate(Object value) {
    if (!((value instanceof String) && ((String) value).length() > 0)) {
      return ValidationStatus.error("You must provide a server URL");
    }
    try {
      var url = new URL((String) value);
      if (StringUtils.isBlank(url.getHost())) {
        return ValidationStatus.error("Please provide a valid URL");
      }
    } catch (MalformedURLException e) {
      return ValidationStatus.error("This is not a valid URL");
    }
    return ValidationStatus.ok();
  }
}
