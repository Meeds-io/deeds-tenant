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
package io.meeds.tenant.metamask.service;

import java.util.*;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.meeds.tenant.metamask.storage.TenantManagerStorage;

/**
 * A service that allows to detect Deed Tenant Manager address
 */
public class TenantManagerService {

  public static final String   MANAGER_DEFAULT_ROLES_PARAM = "managerDefaultRoles";

  public static final String   NFT_ID_PARAM                = "nftId";

  protected static final Log   LOG                         = ExoLogger.getLogger(TenantManagerService.class);

  private TenantManagerStorage tenantManagerStorage;

  private String               nftId;

  private List<String>         tenantManagerDefaultRoles   = new ArrayList<>();

  public TenantManagerService(TenantManagerStorage tenantManagerStorage,
                              InitParams params) {
    this.tenantManagerStorage = tenantManagerStorage;
    this.tenantManagerDefaultRoles = getParamValues(params, MANAGER_DEFAULT_ROLES_PARAM);
    this.nftId = getParamValue(params, NFT_ID_PARAM);
  }

  public boolean isTenantManager(String address) {
    if (StringUtils.isNotBlank(this.nftId)) {
      return this.tenantManagerStorage.isManagerAddress(this.nftId, address);
    } else {
      return false;
    }
  }

  public List<String> getTenantManagerDefaultRoles() {
    return Collections.unmodifiableList(tenantManagerDefaultRoles);
  }

  public String getNftId() {
    return nftId;
  }

  private String getParamValue(InitParams params, String paramName) {
    if (params != null && params.containsKey(paramName)) {
      return params.getValueParam(paramName).getValue();
    }
    return null;
  }

  private List<String> getParamValues(InitParams params, String paramName) {
    if (params != null && params.containsKey(paramName)) {
      return params.getValuesParam(paramName).getValues();
    }
    return Collections.emptyList();
  }

}
