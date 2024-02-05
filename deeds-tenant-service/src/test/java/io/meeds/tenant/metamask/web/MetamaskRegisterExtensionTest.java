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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.login.LoginHandler;
import org.exoplatform.web.register.RegisterHandler;

import io.meeds.tenant.hub.service.HubService;
import io.meeds.tenant.metamask.service.MetamaskLoginService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@SpringBootTest(classes = {
  MetamaskRegisterExtension.class,
})
class MetamaskRegisterExtensionTest {

  @MockBean
  private MetamaskLoginService      metamaskLoginService;

  @MockBean
  private HubService                hubService;

  @Autowired
  private MetamaskRegisterExtension metamaskRegisterExtension;

  @Test
  void testGetExtensionName() {
    assertEquals(Arrays.asList(RegisterHandler.REGISTER_EXTENSION_NAME,
                               LoginHandler.LOGIN_EXTENSION_NAME),
                 metamaskRegisterExtension.getExtensionNames());
  }

  @Test
  void testExtendParametersForLogin() {
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
  void testExtendParametersForRegister() {
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
