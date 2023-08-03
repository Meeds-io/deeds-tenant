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
import org.web3j.crypto.WalletUtils;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.GroupHandler;
import org.exoplatform.services.organization.MembershipHandler;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.MembershipTypeHandler;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.model.WalletProvider;
import org.exoplatform.wallet.service.WalletAccountService;

import lombok.Getter;

/**
 * A service that allows to detect Deed Tenant Manager address
 */
public class TenantManagerService {

  private static final Log     LOG                         = ExoLogger.getLogger(TenantManagerService.class);

  public static final int      MAX_START_TENTATIVES        = 5;

  public static final String   MANAGER_DEFAULT_ROLES_PARAM = "managerDefaultRoles";

  private HubService           hubService;

  private OrganizationService  organizationService;

  private IdentityManager      identityManager;

  private WalletAccountService walletAccountService;

  @Getter
  private List<String>         tenantManagerDefaultRoles   = new ArrayList<>();

  public TenantManagerService(HubService hubService,
                              OrganizationService organizationService,
                              IdentityManager identityManager,
                              WalletAccountService walletAccountService,
                              InitParams params) {
    this.hubService = hubService;
    this.identityManager = identityManager;
    this.organizationService = organizationService;
    this.walletAccountService = walletAccountService;
    this.tenantManagerDefaultRoles = getParamValues(params, MANAGER_DEFAULT_ROLES_PARAM);
  }

  public boolean isTenantManager(String address) {
    return StringUtils.isNotBlank(address)
        && hubService.isDeedHub()
        && StringUtils.equalsIgnoreCase(hubService.getDeedManager(), address);
  }

  public void setTenantManagerRoles(String address) throws Exception {
    User user = organizationService.getUserHandler().findUserByName(address);
    if (user == null) {
      throw new IllegalStateException("User " + address + " doesn't exists");
    }
    LOG.info("Tenant manager registered, setting its default memberships as manager.");
    for (String role : tenantManagerDefaultRoles) {
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

  public Wallet createUserWalletByAddress(String address) {
    if (!WalletUtils.isValidAddress(address)) {
      return null;
    }
    Wallet wallet = walletAccountService.getWalletByAddress(address);
    if (wallet != null) {
      LOG.warn("Wallet with address {} is already associated to identity id {}."
          + "The used Metamask address will not be associated to current user account.",
               address,
               wallet.getTechnicalId());
      return null;
    }
    Identity identity = identityManager.getOrCreateUserIdentity(address);
    Wallet userWallet = walletAccountService.createWalletInstance(WalletProvider.METAMASK,
                                                                  address,
                                                                  Long.valueOf(identity.getId()));
    return walletAccountService.saveWallet(userWallet, true);
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

  private List<String> getParamValues(InitParams params, String paramName) {
    if (params != null && params.containsKey(paramName)) {
      return params.getValuesParam(paramName).getValues();
    }
    return Collections.emptyList();
  }

}
