/*
 * Copyright (C) 2021 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package io.meeds.tenant;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

@Aspect
public class WebAppClassLoaderContextAspect {

  private static final Log LOG = ExoLogger.getLogger(WebAppClassLoaderContextAspect.class);

  @Around("execution(* *(..)) && @annotation(io.meeds.tenant.WebAppClassLoaderContext)")
  public Object around(ProceedingJoinPoint point) throws Throwable {
    ClassLoader springClassLoader = SpringIntegration.getClassLoader();
    if (springClassLoader != null) {
      ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
      try {
        Thread.currentThread().setContextClassLoader(springClassLoader);
        return point.proceed();
      } finally {
        Thread.currentThread().setContextClassLoader(originalContextClassLoader);
      }
    } else {
      LOG.warn("Can't find spring context, class loader is null");
      return point.proceed();
    }
  }

}
