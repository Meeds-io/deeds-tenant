/**
 * This file is part of the Meeds project (https://meeds.io/).
 * 
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.tenant.wom.listener;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
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

import io.meeds.tenant.wom.service.WomService;

import jakarta.annotation.PostConstruct;

@Component
public class DeedManagerUserListener extends UserEventListener {

  private static final Log    LOG                       = ExoLogger.getLogger(DeedManagerUserListener.class);

  @Autowired
  private OrganizationService organizationService;

  @Autowired
  private WomService          womService;

  private List<String>        tenantManagerDefaultRoles = Arrays.asList("*:/platform/users",
                                                                        "*:/platform/administrators",
                                                                        "*:/platform/analytics",
                                                                        "*:/platform/rewarding");

  @PostConstruct
  public void init() {
    organizationService.getUserHandler().addUserEventListener(this);
  }

  @Override
  public void postSave(User user, boolean isNew) throws Exception {
    String address = user.getUserName();
    if (isNew
        && user.isEnabled()
        && WalletUtils.isValidAddress(address)
        && womService.isDeedManager(address)) {
      saveHubHostRoles(user);
    }
  }

  public void saveHubHostRoles(User user) {
    LOG.info("Tenant manager registered, setting its default memberships as manager.");
    for (String membership : tenantManagerDefaultRoles) {
      if (StringUtils.isNotBlank(membership)) {
        LOG.info("Add Tenant manager membership {}.", membership);
        if (StringUtils.contains(membership, ":")) {
          String[] membershipParts = StringUtils.split(membership, ":");
          addUserToGroup(user, membershipParts[1], membershipParts[0]);
        } else {
          addUserToGroup(user, membership, "*");
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
