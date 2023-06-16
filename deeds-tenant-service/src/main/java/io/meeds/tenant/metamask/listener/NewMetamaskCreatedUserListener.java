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

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.web3j.crypto.WalletUtils;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.GroupHandler;
import org.exoplatform.services.organization.MembershipHandler;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.MembershipTypeHandler;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserEventListener;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.model.WalletProvider;
import org.exoplatform.wallet.service.WalletAccountService;

import io.meeds.tenant.service.TenantManagerService;

public class NewMetamaskCreatedUserListener extends UserEventListener {

  private static final Log     LOG = ExoLogger.getLogger(NewMetamaskCreatedUserListener.class);

  private OrganizationService  organizationService;

  private IdentityManager      identityManager;

  private WalletAccountService walletAccountService;

  private TenantManagerService tenantManagerService;

  public NewMetamaskCreatedUserListener(IdentityManager identityManager,
                                        OrganizationService organizationService,
                                        WalletAccountService walletAccountService,
                                        TenantManagerService tenantManagerService) {
    this.organizationService = organizationService;
    this.walletAccountService = walletAccountService;
    this.tenantManagerService = tenantManagerService;
    this.identityManager = identityManager;
  }

  @Override
  public void postSave(User user, boolean isNew) throws Exception {
    String address = user.getUserName();
    if (!isNew || !user.isEnabled() || !WalletUtils.isValidAddress(address)) {
      return;
    }
    Wallet wallet = walletAccountService.getWalletByAddress(address);
    if (wallet != null) {
      LOG.info("Wallet with address {} is already associated to identity id {}."
          + "The used Metamask address will not be associated to current user account.", address, wallet.getTechnicalId());
      return;
    }
    createUserWalletByAddress(address);
    if (isTenantManager(address)) {
      setTenantManagerRoles(user);
    }
  }

  private Wallet createUserWalletByAddress(String address) {
    try {
      Identity identity = identityManager.getOrCreateUserIdentity(address);
      Wallet userWallet = walletAccountService.createWalletInstance(WalletProvider.METAMASK,
                                                                    address,
                                                                    Long.valueOf(identity.getId()));
      return walletAccountService.saveWallet(userWallet, true);
    } catch (Exception e) {
      LOG.warn("Error while associating Metamask wallet for user {}", address, e);
      return null;
    }
  }

  private boolean isTenantManager(String walletAddress) {
    return tenantManagerService.isTenant()
        && tenantManagerService.isTenantManager(walletAddress);
  }

  private void setTenantManagerRoles(User user) {
    List<String> tenantManagerRoles = tenantManagerService.getTenantManagerDefaultRoles();
    LOG.info("Tenant manager registered, setting its default memberships as manager.");
    for (String role : tenantManagerRoles) {
      if (StringUtils.isNotBlank(role)) {
        LOG.info("Add Tenant manager membership {}.", role);
        if (StringUtils.contains(role, ":")) {
          String[] roleParts = StringUtils.split(role, ":");
          String membershipTypeId = roleParts[0];
          String groupId = roleParts[1];

          addUserToGroup(user, groupId, membershipTypeId);
        } else {
          addUserToGroup(user, role, "*");
        }
      }
    }
  }

  private void addUserToGroup(User user, String groupId, String membershipTypeId) {
    GroupHandler groupHandler = organizationService.getGroupHandler();
    MembershipHandler membershipHandler = organizationService.getMembershipHandler();
    MembershipTypeHandler membershipTypeHandler = organizationService.getMembershipTypeHandler();
    try {
      Group group = groupHandler.findGroupById(groupId);
      MembershipType membershipType = membershipTypeHandler.findMembershipType(membershipTypeId);
      if (group != null && membershipType != null) {
        membershipHandler.linkMembership(user, group, membershipType, true);
      } else if (group == null) {
        LOG.warn("Group with id {} wasn't found. Tenant manager membership {} will not be set.",
                 groupId,
                 membershipTypeId + ":" + groupId);
      } else {
        LOG.warn("Membership Type with id {} wasn't found. Tenant manager membership {} will not be set.",
                 membershipTypeId,
                 membershipTypeId + ":" + groupId);
      }
    } catch (Exception e) {
      LOG.warn("Error while adding user {} to role {}:{}", user.getUserName(), membershipTypeId, groupId, e);
    }
  }
}
