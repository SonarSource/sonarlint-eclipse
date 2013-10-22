/*
 * SonarQube Eclipse
 * Copyright (C) 2010-2013 SonarSource
 * dev@sonar.codehaus.org
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.ide.eclipse.ui.internal.wizards.associate;

/**
 * This is one possible result that will be displayed as a suggestion.
 * Because content assist can only work with String and not with complex objects
 * we will have to "serialize" this object using {@link #asString()} in
 * {@link SonarSearchEngineProvider#getProposals(String, int)}
 * then deserialize in the {@link RemoteSonarProjectTextContentAdapter}
 * @author julien
 *
 */
public class RemoteSonarProject {

  private static final String SEPARATOR = "|";

  private String url;
  private String name;
  private String key;

  public RemoteSonarProject(String url, String key, String name) {
    this.url = url;
    this.key = key;
    this.name = name;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String asString() {
    return url + SEPARATOR + key + SEPARATOR + name;
  }

  /**
   * The description that will be displayed in Content assist.
   */
  public String getDescription() {
    StringBuffer sb = new StringBuffer();
    sb.append("Project name: ").append(name).append("\n");
    sb.append("Server: ").append(url).append("\n");
    sb.append("Project key: ").append(key);
    return sb.toString();
  }

  public static RemoteSonarProject fromString(String asString) {
    String[] parts = asString.split("\\" + SEPARATOR);
    return new RemoteSonarProject(parts[0], parts[1], parts[2]);
  }

}
