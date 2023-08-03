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

import static io.meeds.tenant.metamask.web.filter.MetamaskSignInFilter.USERNAME_REQUEST_PARAM;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.login.LoginHandler;
import org.exoplatform.web.login.UIParamsExtension;

import io.meeds.tenant.metamask.service.MetamaskLoginService;
import io.meeds.tenant.service.HubService;

/**
 * A Login extension to submit Login parameters to UI
 */
public class MetamaskLoginExtension implements UIParamsExtension {

  private static final List<String> EXTENSION_NAMES = Collections.singletonList(LoginHandler.LOGIN_EXTENSION_NAME);

  protected MetamaskLoginService    metamaskLoginService;

  protected HubService              hubService;

  public MetamaskLoginExtension(HubService hubService,
                                MetamaskLoginService metamaskLoginService) {
    this.metamaskLoginService = metamaskLoginService;
    this.hubService = hubService;
  }

  @Override
  public List<String> getExtensionNames() {
    return EXTENSION_NAMES;
  }

  @Override
  public Map<String, Object> extendParameters(ControllerContext controllerContext, String extensionName) {
    Map<String, Object> params = new HashMap<>();
    params.put("metamaskEnabled", true);

    HttpSession httpSession = controllerContext.getRequest().getSession(true);
    params.put("rawMessage", metamaskLoginService.generateLoginMessage(httpSession));

    addDeedTenantParameters(httpSession, params);
    return params;
  }

  protected void addDeedTenantParameters(HttpSession httpSession, Map<String, Object> params) {
    if (metamaskLoginService.isDeedHub()) {
      long deedId = metamaskLoginService.getDeedId();
      params.put("nftId", deedId);
      params.put("isDeedTenant", true);

      if (deedId > -1) {
        params.put("cityIndex", hubService.getDeedCity());
        params.put("cardTypeIndex", hubService.getDeedType());
        String walletAddress = (String) httpSession.getAttribute(USERNAME_REQUEST_PARAM);
        if (StringUtils.isNotBlank(walletAddress)
            && metamaskLoginService.isTenantManager(walletAddress)) {
          params.put("isTenantManager", true);
        }
      }
    }
  }

}
