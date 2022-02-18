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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package io.meeds.tenant.metamask.web;

import java.util.*;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.login.LoginHandler;
import org.exoplatform.web.login.UIParamsExtension;
import org.exoplatform.web.register.RegisterHandler;

import io.meeds.tenant.metamask.service.MetamaskLoginService;

/**
 * An extension to submit parameters to Register UI
 */
public class MetamaskRegisterExtension implements UIParamsExtension {

  private static final List<String> EXTENSION_NAMES               = Arrays.asList(RegisterHandler.REGISTER_EXTENSION_NAME,
                                                                                  LoginHandler.LOGIN_EXTENSION_NAME);

  private static final String       METAMASK_REGISTRATION_ENABLED = "metamaskRegistrationEnabled";

  private MetamaskLoginService      metamaskLoginService;

  public MetamaskRegisterExtension(MetamaskLoginService metamaskLoginService) {
    this.metamaskLoginService = metamaskLoginService;
  }

  @Override
  public List<String> getExtensionNames() {
    return EXTENSION_NAMES;
  }

  @Override
  public Map<String, Object> extendParameters(ControllerContext controllerContext, String extensionName) {
    Map<String, Object> params = new HashMap<>();
    if (metamaskLoginService.isAllowUserRegistration()) {
      if (StringUtils.equals(LoginHandler.LOGIN_EXTENSION_NAME, extensionName)) {
        params.put(RegisterHandler.REGISTER_ENABLED, true);
      } else if (StringUtils.equals(RegisterHandler.REGISTER_EXTENSION_NAME, extensionName)) {
        params.put(METAMASK_REGISTRATION_ENABLED, true);
        HttpSession httpSession = controllerContext.getRequest().getSession(true);
        params.put("rawMessage", metamaskLoginService.generateLoginMessage(httpSession));
      }
    }
    return params;
  }

}
