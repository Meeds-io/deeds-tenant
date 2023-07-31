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
package io.meeds.tenant.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.wallet.service.WalletAccountService;

import io.meeds.tenant.model.DeedTenantConfiguration;
import io.meeds.tenant.model.DeedTenantHub;
import io.meeds.tenant.rest.client.TenantServiceConsumer;

/**
 * A service that allows to detect Deed Tenant Manager address
 */
public class TenantManagerService {

  public static final int       MAX_START_TENTATIVES        = 5;

  public static final String    MANAGER_DEFAULT_ROLES_PARAM = "managerDefaultRoles";

  public static final String    NFT_ID_PARAM                = "nftId";

  private WalletAccountService  walletAccountService;

  private TenantServiceConsumer tenantServiceConsumer;

  private String                nftId;

  private List<String>          tenantManagerDefaultRoles   = new ArrayList<>();

  private DeedTenantHub         currentDeedTenantHost;

  public TenantManagerService(WalletAccountService walletAccountService,
                              TenantServiceConsumer tenantServiceConsumer,
                              InitParams params) {
    this.tenantServiceConsumer = tenantServiceConsumer;
    this.walletAccountService = walletAccountService;
    this.tenantManagerDefaultRoles = getParamValues(params, MANAGER_DEFAULT_ROLES_PARAM);
    this.nftId = getParamValue(params, NFT_ID_PARAM);
  }

  public List<String> getTenantManagerDefaultRoles() {
    return Collections.unmodifiableList(tenantManagerDefaultRoles);
  }

  public boolean isTenantManager(String address) {
    if (isTenant() && StringUtils.isNotBlank(address)) {
      if (currentDeedTenantHost == null) {
        currentDeedTenantHost = tenantServiceConsumer.getDeedTenant(getNftId());
        if (currentDeedTenantHost == null) {
          return false;
        }
      }
      if (StringUtils.isBlank(currentDeedTenantHost.getManagerAddress())) {
        boolean isTenantManager = tenantServiceConsumer.isDeedManager(address, getNftId());
        if (isTenantManager) {
          currentDeedTenantHost.setManagerAddress(address);
        }
        return isTenantManager;
      } else {
        return StringUtils.equalsIgnoreCase(address, currentDeedTenantHost.getManagerAddress());
      }
    } else {
      return false;
    }
  }

  public boolean isTenantManager(String address, long nftId) {
    if (StringUtils.isNotBlank(address)) {
      return tenantServiceConsumer.isDeedManager(address, nftId);
    } else {
      return false;
    }
  }

  public DeedTenantHub getDeedTenant(long nftId) {
    return tenantServiceConsumer.getDeedTenant(nftId);
  }

  public DeedTenantHub getDeedTenantHub() {
    if (currentDeedTenantHost != null) {
      return currentDeedTenantHost;
    } else if (isTenant()) {
      currentDeedTenantHost = tenantServiceConsumer.getDeedTenant(getNftId());
      return currentDeedTenantHost;
    } else {
      return null;
    }
  }

  public boolean isTenant() {
    return getNftId() > -1;
  }

  public long getNftId() {
    return StringUtils.isBlank(nftId) ? -1 : Long.parseLong(nftId);
  }

  public DeedTenantConfiguration getDeedTenantConfiguration() {
    DeedTenantConfiguration deedTenantConfiguration = new DeedTenantConfiguration();
    deedTenantConfiguration.setToken(tenantServiceConsumer.generateToken());
    deedTenantConfiguration.setAdminWallet(walletAccountService.getAdminWallet().getAddress());
    return deedTenantConfiguration;
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
