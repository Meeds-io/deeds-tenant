/**
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2022 Meeds Association
 * contact@meeds.io
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
package io.meeds.tenant.metamask;

import java.io.PrintStream;
import java.io.PrintWriter;

public class FakeTestException extends RuntimeException {

  private static final long serialVersionUID = 5983944082820821629L;

  public FakeTestException() {
    super("Fake Test Exception !");
  }

  @Override
  public synchronized Throwable fillInStackTrace() {
    return this;
  }

  @Override
  public StackTraceElement[] getStackTrace() {
    return new StackTraceElement[0];
  }

  @Override
  public void printStackTrace() {
    // Nothing to print
  }

  @Override
  public void printStackTrace(PrintStream s) {
    // Nothing to print
  }

  @Override
  public void printStackTrace(PrintWriter s) {
    // Nothing to print
  }

  @Override
  public void setStackTrace(StackTraceElement[] stackTrace) {
    // Nothing to set
  }

  @Override
  public synchronized Throwable getCause() {
    return null;
  }

  @Override
  public synchronized Throwable initCause(Throwable cause) {
    // Nothing to set
    return this;
  }

}
