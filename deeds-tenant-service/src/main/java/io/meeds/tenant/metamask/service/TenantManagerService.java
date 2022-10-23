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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;
import org.picocontainer.Startable;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.meeds.tenant.SpringIntegration;
import io.meeds.tenant.WebAppClassLoaderContext;
import io.meeds.tenant.model.DeedTenantHost;

/**
 * A service that allows to detect Deed Tenant Manager address
 */
public class TenantManagerService implements Startable {

  public static final int    MAX_START_TENTATIVES        = 5;

  public static final String MANAGER_DEFAULT_ROLES_PARAM = "managerDefaultRoles";

  public static final String NFT_ID_PARAM                = "nftId";

  protected static final Log LOG                         = ExoLogger.getLogger(TenantManagerService.class);

  private String             nftId;

  private List<String>       tenantManagerDefaultRoles   = new ArrayList<>();

  public TenantManagerService(InitParams params) {
    this.tenantManagerDefaultRoles = getParamValues(params, MANAGER_DEFAULT_ROLES_PARAM);
    this.nftId = getParamValue(params, NFT_ID_PARAM);
  }

  @Override
  public void start() {
    CompletableFuture.runAsync(() -> {
      if (hasConfiguredDeedId()) {
        try {
          retrieveDeedTenant(nftId);
        } catch (Exception e) {
          LOG.error("Error retrieving DeedTenant information", e);
        }
      }
    });
  }

  @Override
  public void stop() {
    // Nothing to stop for now
  }

  public boolean isTenantManager(String address) {
    if (hasConfiguredDeedId()) {
      DeedTenantHost deedTenantHost = DeedTenantHost.getInstance();
      return deedTenantHost != null && StringUtils.equalsIgnoreCase(address, deedTenantHost.getManagerAddress());
    } else {
      return false;
    }
  }

  public List<String> getTenantManagerDefaultRoles() {
    return Collections.unmodifiableList(tenantManagerDefaultRoles);
  }

  public boolean isDeedTenant() {
    return hasConfiguredDeedId();
  }

  public long getNftId() {
    return StringUtils.isBlank(nftId) ? -1 : Long.parseLong(nftId);
  }

  @WebAppClassLoaderContext
  public DeedTenantHost retrieveDeedTenant(String nftId) throws Exception {// NOSONAR
    if (hasConfiguredDeedId() && DeedTenantHost.getInstance() == null) {
      String serviceName = "io.meeds.deeds.service.TenantService";
      Class<?> serviceClass = Class.forName(serviceName, true, Thread.currentThread().getContextClassLoader());
      Object serviceInstance = SpringIntegration.getSpringBean(serviceClass);
      Method getDeedTenantMethod = serviceClass.getMethod("getDeedTenant", long.class);
      long nftIdLong = Long.parseLong(nftId);
      Object resultInstance = getDeedTenantMethod.invoke(serviceInstance, nftIdLong);
      if (resultInstance != null) {
        String resultName = "io.meeds.deeds.model.DeedTenant";
        Class<?> resultClass = Class.forName(resultName, true, Thread.currentThread().getContextClassLoader());
        short cityIndex = (short) resultClass.getMethod("getCityIndex").invoke(resultInstance);
        short cardType = (short) resultClass.getMethod("getCardType").invoke(resultInstance);
        boolean isProvisioned = (boolean) resultClass.getMethod("isStartProvisioningStatus").invoke(resultInstance);
        String managerAddress = (String) resultClass.getMethod("getManagerAddress").invoke(resultInstance);
        String managerEmail = (String) resultClass.getMethod("getManagerEmail").invoke(resultInstance);

        return DeedTenantHost.setInstance(nftIdLong, cityIndex, cardType, isProvisioned, managerAddress, managerEmail);
      }
    }
    return null;
  }

  private boolean hasConfiguredDeedId() {
    return StringUtils.isNotBlank(this.nftId);
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
