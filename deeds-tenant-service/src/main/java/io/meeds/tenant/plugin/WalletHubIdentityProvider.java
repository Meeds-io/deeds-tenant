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
package io.meeds.tenant.plugin;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.social.core.identity.IdentityProvider;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;

public class WalletHubIdentityProvider extends IdentityProvider<String> {

  public static final String IDENTITY_PROVIDER_NAME = "WALLET_HUB";

  public static final String IDENTITY_REMOTE_ID     = "HUB";

  public static final String ADDRESS                = "HUB_ADDRESS";

  public static final String WALLET                 = "HUB_WALLET";

  public static final String NAME                   = "HUB_NAME";

  public static final String DESCRIPTION            = "HUB_DESCRIPTION";

  public static final String URL                    = "HUB_URL";

  public static final String LOGO_URL               = "HUB_LOGO_URL";

  public static final String COLOR                  = "HUB_COLOR";

  public static final String EARNER_ADDRESS         = "HUB_EARNER_ADDRESS";

  public static final String DEED_ID                = "HUB_DEED_ID";

  public static final String DEED_CITY              = "HUB_CITY";

  public static final String DEED_TYPE              = "HUB_TYPE";

  public static final String DEED_MANAGER_ADDRESS   = "HUB_MANAGER_ADDRESS";

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

}
