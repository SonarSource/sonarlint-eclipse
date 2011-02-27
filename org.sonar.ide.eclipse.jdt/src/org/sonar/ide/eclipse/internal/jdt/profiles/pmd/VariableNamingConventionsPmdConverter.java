/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2010-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.ide.eclipse.internal.jdt.profiles.pmd;

import org.eclipse.jdt.core.JavaCore;
import org.sonar.ide.eclipse.internal.jdt.profiles.ProfileConfiguration;
import org.sonar.wsclient.services.Rule;

/**
 * @author Jérémie Lagarde
 * @since 1.1.0
 * 
 *        Class to convert Pmd VariableNamingConventions rule to eclipse preferences.
 * 
 *        ex : {"title":"Naming - Variable naming conventions","key":"pmd:VariableNamingConventions","plugin":"pmd",
 *        "description":"A variable naming conventions rule - customize this to your liking. Currently, it checks for final variables that should be fully capitalized and non-final variables that should not include underscores.",
 *        "priority":"MAJOR","status":"INACTIVE","params":[
 *          {"name":"staticPrefix","description":"A prefix for static variables"},
 *          {"name":"staticSuffix","description":"A suffix for static variables"},
 *          {"name":"memberPrefix","description":"A prefix for member variables"},
 *          {"name":"memberSuffix","description":"A suffix for member variables"}]}
 */
public class VariableNamingConventionsPmdConverter extends AbstractPmdConverter {

  protected final static String KEY = "pmd:VariableNamingConventions";

  public VariableNamingConventionsPmdConverter() {
    super(KEY);
  }

  public void convert(ProfileConfiguration config, Rule rule) {
    String staticPrefix = getParam(rule, "staticPrefix", "");
    String staticSuffix = getParam(rule, "staticSuffix", "");
    String memberPrefix = getParam(rule, "memberPrefix", "");
    String memberSuffix = getParam(rule, "memberSuffix", "");
    config.addOption(JavaCore.CODEASSIST_STATIC_FIELD_PREFIXES, staticPrefix);
    config.addOption(JavaCore.CODEASSIST_STATIC_FIELD_SUFFIXES, staticSuffix);
    config.addOption(JavaCore.CODEASSIST_FIELD_PREFIXES, memberPrefix);
    config.addOption(JavaCore.CODEASSIST_FIELD_SUFFIXES, memberSuffix);
  }
  
}
