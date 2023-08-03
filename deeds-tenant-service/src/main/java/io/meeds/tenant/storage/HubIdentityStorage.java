/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2023 Meeds Association contact@meeds.io
 *
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
package io.meeds.tenant.storage;

import static io.meeds.tenant.plugin.WalletHubIdentityProvider.ADDRESS;
import static io.meeds.tenant.plugin.WalletHubIdentityProvider.COLOR;
import static io.meeds.tenant.plugin.WalletHubIdentityProvider.DEED_CITY;
import static io.meeds.tenant.plugin.WalletHubIdentityProvider.DEED_ID;
import static io.meeds.tenant.plugin.WalletHubIdentityProvider.DEED_MANAGER_ADDRESS;
import static io.meeds.tenant.plugin.WalletHubIdentityProvider.DEED_TYPE;
import static io.meeds.tenant.plugin.WalletHubIdentityProvider.DESCRIPTION;
import static io.meeds.tenant.plugin.WalletHubIdentityProvider.EARNER_ADDRESS;
import static io.meeds.tenant.plugin.WalletHubIdentityProvider.IDENTITY_PROVIDER_NAME;
import static io.meeds.tenant.plugin.WalletHubIdentityProvider.IDENTITY_REMOTE_ID;
import static io.meeds.tenant.plugin.WalletHubIdentityProvider.JOIN_DATE;
import static io.meeds.tenant.plugin.WalletHubIdentityProvider.LOGO_URL;
import static io.meeds.tenant.plugin.WalletHubIdentityProvider.NAME;
import static io.meeds.tenant.plugin.WalletHubIdentityProvider.URL;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.manager.IdentityManager;

import io.meeds.deeds.constant.WomException;
import io.meeds.deeds.model.Hub;
import io.meeds.tenant.rest.client.WoMServiceClient;

public class HubIdentityStorage {

  private static final Log LOG         = ExoLogger.getLogger(HubIdentityStorage.class);

  private IdentityManager  identityManager;

  private WoMServiceClient womServiceClient;

  private Identity         hubIdentity = null;

  public HubIdentityStorage(IdentityManager identityManager,
                            WoMServiceClient womServiceClient) {
    this.identityManager = identityManager;
    this.womServiceClient = womServiceClient;
  }

  public Identity getHubIdentity() {
    if (this.hubIdentity == null) {
      this.hubIdentity = identityManager.getOrCreateIdentity(IDENTITY_PROVIDER_NAME, IDENTITY_REMOTE_ID);
      if (this.hubIdentity != null) {
        populateProfile(this.hubIdentity.getProfile());
      }
    }
    return this.hubIdentity;
  }

  public Profile getHubProfile() {
    Identity identity = getHubIdentity();
    return identity == null ? null : identity.getProfile();
  }

  public String getHubProperty(String attributeName) {
    Profile hubProfile = getHubProfile();
    return hubProfile == null ? null : (String) hubProfile.getProperty(attributeName);
  }

  public void saveHubProperty(String name, String value) {
    Identity identity = getHubIdentity();
    if (identity == null || identity.getProfile() == null) {
      return;
    }
    Profile hubProfile = identity.getProfile();
    if (StringUtils.isBlank(value)) {
      hubProfile.removeProperty(name);
    } else {
      hubProfile.setProperty(name, value);
    }
    identityManager.updateProfile(hubProfile);
  }

  public void refreshHubIdentity() {
    // Force Retrieve Hub profile again
    hubIdentity = null;
  }

  public void populateProfile(Profile hubProfile) {
    try {
      String hubAddress = (String) hubProfile.getProperty(ADDRESS);
      Hub hub = womServiceClient.getHub(hubAddress);
      if (hub != null) {
        populateProfile(hubProfile, hub);
      } else {
        clearHubProperties(hubProfile);
      }
    } catch (WomException e) {
      LOG.warn("Error communicating with WoM Server, couldn't retrieve Hub remote status", e);
    }
  }

  private void populateProfile(Profile hubProfile, Hub hub) {
    hubProfile.setProperty(DEED_ID, String.valueOf(hub.getDeedId()));
    hubProfile.setProperty(DEED_CITY, String.valueOf(hub.getCity()));
    hubProfile.setProperty(DEED_TYPE, String.valueOf(hub.getType()));
    hubProfile.setProperty(NAME, hub.getName());
    hubProfile.setProperty(DESCRIPTION, hub.getDescription());
    hubProfile.setProperty(URL, hub.getUrl());
    hubProfile.setProperty(LOGO_URL, hub.getLogoUrl());
    hubProfile.setProperty(COLOR, hub.getColor());
    hubProfile.setProperty(EARNER_ADDRESS, hub.getEarnerAddress());
    hubProfile.setProperty(DEED_MANAGER_ADDRESS, hub.getDeedManagerAddress());
    hubProfile.setProperty(JOIN_DATE, String.valueOf(hub.getCreatedDate().toEpochMilli()));
    identityManager.updateProfile(hubProfile);
  }

  private void clearHubProperties(Profile hubProfile) {
    if (hubProfile.getIdentity() != null && StringUtils.isNotBlank(hubProfile.getIdentity().getId())) {
      hubProfile.removeProperty(DEED_ID);
      hubProfile.removeProperty(DEED_CITY);
      hubProfile.removeProperty(DEED_TYPE);
      hubProfile.removeProperty(NAME);
      hubProfile.removeProperty(DESCRIPTION);
      hubProfile.removeProperty(URL);
      hubProfile.removeProperty(LOGO_URL);
      hubProfile.removeProperty(COLOR);
      hubProfile.removeProperty(EARNER_ADDRESS);
      hubProfile.removeProperty(DEED_MANAGER_ADDRESS);
      hubProfile.removeProperty(JOIN_DATE);
      identityManager.updateProfile(hubProfile);
    }
  }

}
