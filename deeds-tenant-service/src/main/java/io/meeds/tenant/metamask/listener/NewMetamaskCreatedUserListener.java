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
package io.meeds.tenant.metamask.listener;

import org.web3j.crypto.WalletUtils;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserEventListener;

import io.meeds.tenant.service.TenantManagerService;

public class NewMetamaskCreatedUserListener extends UserEventListener {

  private static final Log     LOG = ExoLogger.getLogger(NewMetamaskCreatedUserListener.class);

  private TenantManagerService tenantManagerService;

  public NewMetamaskCreatedUserListener(TenantManagerService tenantManagerService) {
    this.tenantManagerService = tenantManagerService;
  }

  @Override
  public void postSave(User user, boolean isNew) throws Exception {
    String address = user.getUserName();
    if (isNew && user.isEnabled() && WalletUtils.isValidAddress(address)) {
      try {
        tenantManagerService.createUserWalletByAddress(address);
      } catch (Exception e) {
        LOG.warn("Error while associating Metamask wallet for user {}", address, e);
      }
      try {
        tenantManagerService.setTenantManagerRoles(address);
      } catch (Exception e) {
        LOG.warn("Error while setting admin roles for user {}", address, e);
      }
    }
  }

}
