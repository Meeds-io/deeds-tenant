/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 - 2022 Meeds Association contact@meeds.io
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
package io.meeds.tenant.integration.proxy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.meeds.deeds.constant.ObjectNotFoundException;
import io.meeds.deeds.constant.TenantProvisioningStatus;
import io.meeds.deeds.elasticsearch.model.DeedTenant;
import io.meeds.deeds.service.TenantService;
import io.meeds.tenant.integration.SpringIntegration;
import io.meeds.tenant.integration.service.TenantServiceFacade;
import io.meeds.tenant.model.DeedTenantHost;

@Component("io.meeds.tenant.integration.service.TenantServiceFacade")
public class TenantServiceFacadeImpl implements TenantServiceFacade {

  @Autowired
  private TenantService tenantService;

  @Override
  public DeedTenantHost getDeedTenant(long nftId) {
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

  @Override
  @SpringIntegration
  public boolean isTenantManager(String address, long nftId) {
    return tenantService.isDeedManager(address, nftId);
  }

}
