/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package io.meeds.tenant.hub.plugin;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.exoplatform.social.core.identity.IdentityProvider;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.manager.IdentityManager;

import jakarta.annotation.PostConstruct;

@Service
public class WalletHubIdentityProvider extends IdentityProvider<String> {

  public static final String IDENTITY_PROVIDER_NAME   = "WALLET_HUB";

  public static final String IDENTITY_REMOTE_ID       = "HUB";

  public static final String ADDRESS                  = "HUB_ADDRESS";

  public static final String CREATED_JOIN_DATE        = "HUB_CREATED_DATE";

  public static final String START_JOIN_DATE          = "HUB_START_JOIN_DATE";

  public static final String END_JOIN_DATE            = "HUB_END_JOIN_DATE";

  public static final String UPDATED_DATE             = "HUB_UPDATED_DATE";

  public static final String WALLET                   = "HUB_WALLET";

  public static final String NAME                     = "HUB_NAME";

  public static final String DESCRIPTION              = "HUB_DESCRIPTION";

  public static final String URL                      = "HUB_URL";

  public static final String COLOR                    = "HUB_COLOR";

  public static final String HUB_OWNER_ADDRESS        = "HUB_OWNER_ADDRESS";

  public static final String HUB_AVATAR_UPDATE        = "HUB_AVATAR_UPDATE_TIME";

  public static final String REWARD_PERIOD_TYPE       = "HUB_PERIOD_TYPE";

  public static final String REWARD_AMOUNT            = "HUB_REWARDS_AMOUNT";

  public static final String OWNER_CLAIMABLE_AMOUNT   = "HUB_OWNER_CLAIMABLE_AMOUNT";

  public static final String MANAGER_CLAIMABLE_AMOUNT = "HUB_MANAGER_CLAIMABLE_AMOUNT";

  public static final String USERS_COUNT              = "HUB_USERS_COUNT";

  public static final String HUB_ENABLED              = "HUB_ENABLED";

  public static final String DEED_ID                  = "DEED_ID";

  public static final String DEED_CITY                = "DEED_CITY";

  public static final String DEED_TYPE                = "DEED_TYPE";

  public static final String DEED_OWNER_ADDRESS       = "DEED_OWNER_ADDRESS";

  public static final String DEED_MANAGER_ADDRESS     = "DEED_MANAGER_ADDRESS";

  public static final String WOM_NETWORK_ID           = "WOM_NETWORK_ID";

  public static final String WOM_CONTRACT_ADDRESS     = "WOM_CONTRACT_ADDRESS";

  public static final String UEM_CONTRACT_ADDRESS     = "UEM_CONTRACT_ADDRESS";

  @Autowired
  private IdentityManager    identityManager;

  @Override
  public String getName() {
    return IDENTITY_PROVIDER_NAME;
  }

  @Override
  public String findByRemoteId(String remoteId) {
    return StringUtils.equals(remoteId, IDENTITY_REMOTE_ID) ? IDENTITY_REMOTE_ID : null;
  }

  @Override
  public Identity createIdentity(String remoteId) {
    return new Identity(IDENTITY_PROVIDER_NAME, remoteId);
  }

  @Override
  public void populateProfile(Profile profile, String remoteId) {
    // No specific properties
  }

  @PostConstruct
  public void init() {
    identityManager.addIdentityProvider(this);
  }

}
