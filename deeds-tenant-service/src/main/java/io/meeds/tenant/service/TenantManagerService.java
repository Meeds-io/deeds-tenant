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
import org.picocontainer.Startable;

import org.exoplatform.container.xml.InitParams;

import io.meeds.tenant.integration.SpringContext;
import io.meeds.tenant.integration.SpringIntegration;
import io.meeds.tenant.integration.service.TenantServiceFacade;
import io.meeds.tenant.model.DeedTenantHost;

/**
 * A service that allows to detect Deed Tenant Manager address
 */
public class TenantManagerService implements Startable {

  public static final int         MAX_START_TENTATIVES        = 5;

  public static final String      MANAGER_DEFAULT_ROLES_PARAM = "managerDefaultRoles";

  public static final String      NFT_ID_PARAM                = "nftId";

  protected TenantServiceFacade tenantServiceFacade;

  private String                  nftId;

  private List<String>            tenantManagerDefaultRoles   = new ArrayList<>();

  public TenantManagerService(InitParams params) {
    this.tenantManagerDefaultRoles = getParamValues(params, MANAGER_DEFAULT_ROLES_PARAM);
    this.nftId = getParamValue(params, NFT_ID_PARAM);
  }

  @Override
  public void start() {
    if (isTenant()) {
      DeedTenantHost deedTenantHost = retrieveDeedTenant();
      if (deedTenantHost == null) {
        throw unreadyConfigurationException(null);
      } else {
        initMetaverseIntegration();
      }
    }
  }

  @Override
  public void stop() {
    // Nothing to stop for now
  }

  public List<String> getTenantManagerDefaultRoles() {
    return Collections.unmodifiableList(tenantManagerDefaultRoles);
  }

  @SpringIntegration
  public boolean isTenantManager(String address) {
    if (isTenant() && StringUtils.isNotBlank(address)) {
      DeedTenantHost deedTenantHost = DeedTenantHost.getInstance();
      if (deedTenantHost == null || StringUtils.isBlank(deedTenantHost.getManagerAddress())) {
        boolean isTenantManager = getTenantServiceFacade().isTenantManager(address, getNftId());
        if (isTenantManager && deedTenantHost != null) {
          deedTenantHost.setManagerAddress(address);
        }
        return isTenantManager;
      } else {
        return StringUtils.equalsIgnoreCase(address, deedTenantHost.getManagerAddress());
      }
    } else {
      return false;
    }
  }

  public boolean isTenant() {
    return getNftId() > -1;
  }

  public long getNftId() {
    return StringUtils.isBlank(nftId) ? -1 : Long.parseLong(nftId);
  }

  protected TenantServiceFacade getTenantServiceFacade() {
    if (tenantServiceFacade == null) {
      try {
        tenantServiceFacade = SpringContext.getSpringBean(TenantServiceFacade.class);
      } catch (Exception e) {
        throw unreadyConfigurationException(e);
      }
    }
    if (tenantServiceFacade == null) {
      throw unreadyConfigurationException(null);
    }
    return tenantServiceFacade;
  }

  @SpringIntegration
  private DeedTenantHost retrieveDeedTenant() {// NOSONAR
    return getTenantServiceFacade().getDeedTenant(getNftId());
  }

  @SpringIntegration
  private void initMetaverseIntegration() {
    getTenantServiceFacade().initMetaverseIntegration();
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

  private IllegalStateException unreadyConfigurationException(Exception e) {
    return new IllegalStateException("Can't Reach TenantService from Spring context, the Deed Tenant must shutdown until the correct configuration is set. (Deed Tenants ES configuration)", e);
  }

}
