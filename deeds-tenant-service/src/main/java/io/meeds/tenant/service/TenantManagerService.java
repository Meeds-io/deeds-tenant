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

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.meeds.deeds.constant.ObjectNotFoundException;
import io.meeds.deeds.constant.TenantProvisioningStatus;
import io.meeds.deeds.elasticsearch.model.DeedTenant;
import io.meeds.deeds.service.ListenerService;
import io.meeds.deeds.service.TenantService;
import io.meeds.tenant.model.DeedTenantHost;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;

/**
 * A service that allows to detect Deed Tenant Manager address
 */
@Service
public class TenantManagerService {

  @Autowired
  private TenantService   tenantService;

  @Autowired
  private ListenerService listenerService;

  @Getter
  @Setter
  @Value("${meeds.tenantManagement.nftId:-1}")
  private long            nftId;

  @Getter
  private List<String>    tenantManagerDefaultRoles = Arrays.asList("*:/platform/users",
                                                                    "*:/platform/administrators",
                                                                    "*:/platform/analytics",
                                                                    "*:/platform/rewarding");

  @PostConstruct
  public void start() {
    if (isTenant()) {
      DeedTenantHost deedTenantHost = retrieveDeedTenant();
      if (deedTenantHost == null) {
        throw new IllegalStateException("Can't get Deed Tenant from WoM Server");
      } else {
        initMetaverseIntegration();
      }
    }
  }

  public boolean isTenantManager(String address) {
    if (isTenant() && StringUtils.isNotBlank(address)) {
      DeedTenantHost deedTenantHost = DeedTenantHost.getInstance();
      if (deedTenantHost == null || StringUtils.isBlank(deedTenantHost.getManagerAddress())) {
        boolean isTenantManager = isTenantManager(address, nftId);
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
    return nftId > -1;
  }

  private boolean isTenantManager(String address, long nftId) {
    return tenantService.isDeedManager(address, nftId);
  }

  private DeedTenantHost retrieveDeedTenant() {// NOSONAR
    if (nftId >= 0 && DeedTenantHost.getInstance() == null) {
      DeedTenant deedTenant = tenantService.getDeedTenant(nftId);
      if (deedTenant == null) {
        try {
          deedTenant = tenantService.buildDeedTenantFromBlockchain(nftId);
        } catch (ObjectNotFoundException e) {
          throw new IllegalStateException("Deed with NFT " + nftId + " doesn't exist on Blockchain");
        }
      }
      if (deedTenant != null) {
        boolean isProvisioned = deedTenant.getTenantProvisioningStatus() != null
                                && deedTenant.getTenantProvisioningStatus() != TenantProvisioningStatus.STOP_CONFIRMED;
        return DeedTenantHost.setInstance(deedTenant.getNftId(),
                                          deedTenant.getCityIndex(),
                                          deedTenant.getCardType(),
                                          isProvisioned,
                                          deedTenant.getManagerAddress(),
                                          deedTenant.getManagerEmail());
      }
    }
    return null;
  }

  private void initMetaverseIntegration() {
    listenerService.enable();
  }

}
