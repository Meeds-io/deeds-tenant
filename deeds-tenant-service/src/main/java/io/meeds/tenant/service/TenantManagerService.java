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

import org.exoplatform.commons.exception.ObjectNotFoundException;

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
  private BlockchainService blockchainService;

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
  public void start() throws ObjectNotFoundException {
    if (isTenant()) {
      DeedTenantHost deedTenantHost = retrieveDeedTenant();
      if (deedTenantHost == null) {
        throw new IllegalStateException("Can't get Deed Tenant from WoM Server");
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

  /**
   * Checks if address is the provisioning manager of the DEED
   * 
   * @param  nftId   DEED NFT identifier
   * @param  address Wallet or Contract Ethereum address
   * @return         true if address is the provisioning manager of the DEED
   *                 Tenant
   */
  private boolean isTenantManager(String address, long nftId) {
    return blockchainService.isDeedProvisioningManager(address, nftId);
  }

  /**
   * Retrieve Deed Tenant information from blockchain
   * 
   * @param nftId DEED NFT id in the blockchain
   * @return {@link DeedTenantHost}
   * @throws ObjectNotFoundException when Deed NFT id is not recognized on
   *           blockchain
   */
  private DeedTenantHost retrieveDeedTenant() throws ObjectNotFoundException {// NOSONAR
    if (nftId >= 0 && DeedTenantHost.getInstance() == null) {
      short cityIndex = blockchainService.getDeedCityIndex(nftId);
      short cardType = blockchainService.getDeedCardType(nftId);
      boolean provisioned = blockchainService.isDeedStarted(nftId);
      return DeedTenantHost.setInstance(nftId, cityIndex, cardType, provisioned, null, null);
    }
    return null;
  }

}
