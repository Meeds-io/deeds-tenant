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

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.login.LoginHandler;

import io.meeds.tenant.hub.service.HubService;
import io.meeds.tenant.metamask.service.MetamaskLoginService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@SpringBootTest(classes = {
  MetamaskLoginExtension.class,
})
class MetamaskLoginExtensionTest {

  @MockBean
  private MetamaskLoginService   metamaskLoginService;

  @MockBean
  private HubService             hubService;

  @Autowired
  private MetamaskLoginExtension metamaskLoginExtension;

  @Test
  void testGetExtensionName() {
    assertEquals(Collections.singletonList(LoginHandler.LOGIN_EXTENSION_NAME),
                 metamaskLoginExtension.getExtensionNames());
  }

  @Test
  void testExtendParameters() {
    String rawMessage = "rawMessage";

    ControllerContext controllerContext = mock(ControllerContext.class);
    HttpServletRequest request = mock(HttpServletRequest.class);
    HttpSession session = mock(HttpSession.class);
    when(controllerContext.getRequest()).thenReturn(request);
    when(request.getSession(anyBoolean())).thenReturn(session);
    when(metamaskLoginService.generateLoginMessage(session)).thenReturn(rawMessage);

    Map<String, Object> extendParameters = metamaskLoginExtension.extendParameters(controllerContext, null);
    assertNotNull(extendParameters);
    assertEquals(true, extendParameters.get("metamaskEnabled"));
    assertEquals(rawMessage, extendParameters.get("rawMessage"));
  }

}
