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
package io.meeds.tenant.metamask.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.login.LoginHandler;
import org.exoplatform.web.register.RegisterHandler;

import io.meeds.tenant.metamask.service.MetamaskLoginService;
import io.meeds.tenant.service.TenantManagerService;

@RunWith(MockitoJUnitRunner.class)
public class MetamaskRegisterExtensionTest {

  @Mock
  private MetamaskLoginService      metamaskLoginService;

  @Mock
  private TenantManagerService      tenantManagerService;

  private MetamaskRegisterExtension metamaskRegisterExtension;

  @Before
  public void setUp() {
    reset(metamaskLoginService);
    metamaskRegisterExtension = new MetamaskRegisterExtension(tenantManagerService, metamaskLoginService);
  }

  @Test
  public void testGetExtensionName() {
    assertEquals(Arrays.asList(RegisterHandler.REGISTER_EXTENSION_NAME,
                               LoginHandler.LOGIN_EXTENSION_NAME),
                 metamaskRegisterExtension.getExtensionNames());
  }

  @Test
  public void testExtendParametersForLogin() {
    when(metamaskLoginService.isAllowUserRegistration()).thenReturn(true);
    Map<String, Object> extendParameters = metamaskRegisterExtension.extendParameters(null, LoginHandler.LOGIN_EXTENSION_NAME);
    assertNotNull(extendParameters);
    assertEquals(true, extendParameters.get(RegisterHandler.REGISTER_ENABLED));
    assertEquals(1, extendParameters.size());

    when(metamaskLoginService.isAllowUserRegistration()).thenReturn(false);
    extendParameters = metamaskRegisterExtension.extendParameters(null, LoginHandler.LOGIN_EXTENSION_NAME);
    assertNotNull(extendParameters);
    assertEquals(0, extendParameters.size());
  }

  @Test
  public void testExtendParametersForRegister() {
    String rawMessage = "rawMessage";

    when(metamaskLoginService.isAllowUserRegistration()).thenReturn(true);
    ControllerContext controllerContext = mock(ControllerContext.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpSession session = mock(HttpSession.class);
    when(controllerContext.getRequest()).thenReturn(request);
    when(request.getSession(anyBoolean())).thenReturn(session);
    when(metamaskLoginService.generateLoginMessage(session)).thenReturn(rawMessage);

    Map<String, Object> extendParameters = metamaskRegisterExtension.extendParameters(controllerContext,
                                                                                      RegisterHandler.REGISTER_EXTENSION_NAME);
    assertNotNull(extendParameters);
    assertEquals(2, extendParameters.size());
    assertEquals(true, extendParameters.get(MetamaskRegisterExtension.METAMASK_REGISTRATION_ENABLED));
    assertEquals(rawMessage, extendParameters.get("rawMessage"));

    when(metamaskLoginService.isAllowUserRegistration()).thenReturn(false);
    extendParameters = metamaskRegisterExtension.extendParameters(controllerContext,
                                                                  RegisterHandler.REGISTER_EXTENSION_NAME);
    assertNotNull(extendParameters);
    assertEquals(0, extendParameters.size());
  }

}
