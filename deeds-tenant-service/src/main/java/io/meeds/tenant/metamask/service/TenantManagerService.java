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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.picocontainer.Startable;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.meeds.tenant.metamask.storage.TenantManagerStorage;
import io.meeds.tenant.model.DeedTenant;

/**
 * A service that allows to detect Deed Tenant Manager address
 */
public class TenantManagerService implements Startable {

  public static final int      MAX_START_TENTATIVES        = 5;

  public static final String   MANAGER_DEFAULT_ROLES_PARAM = "managerDefaultRoles";

  public static final String   NFT_ID_PARAM                = "nftId";

  protected static final Log   LOG                         = ExoLogger.getLogger(TenantManagerService.class);

  private TenantManagerStorage tenantManagerStorage;

  private DeedTenant           deedTenant;

  private String               nftId;

  private List<String>         tenantManagerDefaultRoles   = new ArrayList<>();

  private AtomicInteger        blockchainReadTentatives    = new AtomicInteger(0);

  public TenantManagerService(TenantManagerStorage tenantManagerStorage,
                              InitParams params) {
    this.tenantManagerStorage = tenantManagerStorage;
    this.tenantManagerDefaultRoles = getParamValues(params, MANAGER_DEFAULT_ROLES_PARAM);
    this.nftId = getParamValue(params, NFT_ID_PARAM);
  }

  @Override
  public void start() {
    if (blockchainReadTentatives.incrementAndGet() > MAX_START_TENTATIVES) {
      LOG.error("Reached Maximumum tentatives to retrieve Deed Tenant information from Blockchain. Tenant will be considered as not of Type Deed NFT.");
      return;
    }
    CompletableFuture.runAsync(() -> {
      try {
        if (isEnabled()) {
          deedTenant = this.tenantManagerStorage.getDeedTenant(nftId);
        }
      } catch (Exception e) {
        LOG.warn("Error while retrieving Deed Tenant information from Blockchain, the operation will be retried", e);
        if (hasConfiguredDeedId()) {
          start();
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
      try {
        boolean tenantManager = this.tenantManagerStorage.isManagerAddress(this.nftId, address);
        if (tenantManager && deedTenant != null && !StringUtils.equalsIgnoreCase(address, deedTenant.getManagerAddress())) {
          deedTenant.setManagerAddress(address);
        }
        return tenantManager;
      } catch (Exception e) {
        LOG.warn("Error retrieving information whether address {} is manager of contract or not. Try with already retrieved DeedTenant {}",
                 address,
                 deedTenant,
                 e);
        return deedTenant != null && StringUtils.equalsIgnoreCase(address, deedTenant.getManagerAddress());
      }
    } else {
      return false;
    }
  }

  public List<String> getTenantManagerDefaultRoles() {
    return Collections.unmodifiableList(tenantManagerDefaultRoles);
  }

  public boolean isDeedTenant() throws Exception {
    return isEnabled() && deedTenant != null;
  }

  public DeedTenant getDeedTenant() {
    return deedTenant;
  }

  public long getNftId() {
    return StringUtils.isBlank(nftId) ? -1 : Long.parseLong(nftId);
  }

  protected boolean isEnabled() throws Exception {
    return hasConfiguredDeedId() && tenantManagerStorage.isEnabled();
  }

  protected boolean hasConfiguredDeedId() {
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
