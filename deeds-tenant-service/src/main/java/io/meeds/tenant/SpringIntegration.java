/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 - 2022 Meeds Association contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.tenant;

import javax.servlet.ServletContext;

public class SpringIntegration {

  private static ServletContext servletContext; // NOSONAR

  private static ClassLoader    classLoader;    // NOSONAR

  private SpringIntegration() {
    // Static methods class
  }

  public static void setServletContext(ServletContext servletContext) {
    SpringIntegration.servletContext = servletContext;
  }

  public static ServletContext getServletContext() {
    return servletContext;
  }

  public static void setClassLoader(ClassLoader classLoader) {
    SpringIntegration.classLoader = classLoader;
  }

  public static ClassLoader getClassLoader() {
    return classLoader;
  }

  @SuppressWarnings("unchecked")
  @WebAppClassLoaderContext
  public static <T> T getSpringBean(Class<T> clazz) {
    if (servletContext != null) {
      try {
        Object webApplicationContext = classLoader.loadClass("org.springframework.web.context.support.WebApplicationContextUtils")
                                                  .getMethod("getRequiredWebApplicationContext", ServletContext.class)
                                                  .invoke(null, servletContext);
        return (T) webApplicationContext.getClass().getMethod("getBean", Class.class).invoke(webApplicationContext, clazz);
      } catch (Exception e) {
        throw new IllegalStateException("Error loading Bean with name " + clazz, e);
      }
    } else {
      return null;
    }
  }

}
