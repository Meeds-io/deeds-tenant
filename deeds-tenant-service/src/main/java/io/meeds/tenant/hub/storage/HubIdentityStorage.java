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
package io.meeds.tenant.hub.storage;

import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.ADDRESS;
import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.COLOR;
import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.DEED_CITY;
import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.DEED_ID;
import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.DEED_MANAGER_ADDRESS;
import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.DEED_OWNER_ADDRESS;
import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.DEED_TYPE;
import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.DESCRIPTION;
import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.EARNER_ADDRESS;
import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.HUB_ENABLED;
import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.HUB_OWNER_ADDRESS;
import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.IDENTITY_PROVIDER_NAME;
import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.IDENTITY_REMOTE_ID;
import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.*;
import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.NAME;
import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.REWARD_AMOUNT;
import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.REWARD_PERIOD_TYPE;
import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.UPDATED_DATE;
import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.URL;
import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.USERS_COUNT;
import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.WALLET;
import static io.meeds.wom.api.utils.JsonUtils.fromJsonString;
import static io.meeds.wom.api.utils.JsonUtils.toJsonString;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.manager.IdentityManager;

import io.meeds.tenant.hub.rest.client.WomClientService;
import io.meeds.wom.api.constant.WomException;
import io.meeds.wom.api.constant.WomParsingException;
import io.meeds.wom.api.model.Hub;

@Component
public class HubIdentityStorage {

  @Autowired
  private IdentityManager  identityManager;

  @Autowired
  private WomClientService hubServiceClient;

  private boolean          retrievedFromWom;

  private Hub              hub = null;

  public Hub getHub() {
    return getHub(false);
  }

  public Hub getHub(boolean forceRefresh) {
    if (!forceRefresh
        && retrievedFromWom
        && (hub == null
            || !hub.isConnected()
            || hub.getUntilDate() == null
            || hub.getUntilDate().isAfter(Instant.now()))) {
      return hub;
    }
    Profile hubProfile = getHubProfile();
    boolean retrieved = retrieveHubFromWoM(hubProfile, forceRefresh);
    if (retrieved) {
      hub = mapToHub(hubProfile);
    }
    return hub;
  }

  public String getHubAddress() {
    return (String) getHubProfile().getProperty(ADDRESS);
  }

  public String getHubWallet() {
    return (String) getHubProfile().getProperty(WALLET);
  }

  public void saveHubWallet(String address, String wallet) {
    Profile hubProfile = getHubProfile();
    hubProfile.setProperty(ADDRESS, StringUtils.lowerCase(address));
    hubProfile.setProperty(WALLET, wallet);
    identityManager.updateProfile(hubProfile);
  }

  public void refreshHubIdentity() {
    // Force Retrieve Hub profile again
    retrievedFromWom = false;
    hub = null;
  }

  private Profile getHubProfile() {
    return getHubIdentity().getProfile();
  }

  private Identity getHubIdentity() {
    return identityManager.getOrCreateIdentity(IDENTITY_PROVIDER_NAME, IDENTITY_REMOTE_ID);
  }

  private boolean retrieveHubFromWoM(Profile hubProfile, boolean forceRefresh) {
    try {
      String hubAddress = (String) hubProfile.getProperty(ADDRESS);
      Hub remoteHub = hubServiceClient.getHub(hubAddress, forceRefresh);
      if (remoteHub == null) {
        clearHubProperties(hubProfile);
      } else {
        mapToProfile(hubProfile, remoteHub);
      }
      this.retrievedFromWom = true;
      return remoteHub != null;
    } catch (WomException e) {
      throw new IllegalStateException("Error communicating with WoM Server, couldn't retrieve Hub remote status", e);
    }
  }

  private void mapToProfile(Profile hubProfile, Hub hub) throws WomParsingException {
    hubProfile.setProperty(DEED_ID, String.valueOf(hub.getDeedId()));
    hubProfile.setProperty(DEED_CITY, String.valueOf(hub.getCity()));
    hubProfile.setProperty(DEED_TYPE, String.valueOf(hub.getType()));
    hubProfile.setProperty(NAME, toJsonString(hub.getName()));
    hubProfile.setProperty(DESCRIPTION, toJsonString(hub.getDescription()));
    hubProfile.setProperty(URL, hub.getUrl());
    hubProfile.setProperty(REWARD_PERIOD_TYPE, hub.getRewardsPeriodType());
    hubProfile.setProperty(REWARD_AMOUNT, String.valueOf(hub.getRewardsPerPeriod()));
    hubProfile.setProperty(USERS_COUNT, String.valueOf(hub.getUsersCount()));
    hubProfile.setProperty(COLOR, hub.getColor());
    hubProfile.setProperty(EARNER_ADDRESS, hub.getEarnerAddress());
    hubProfile.setProperty(HUB_OWNER_ADDRESS, hub.getHubOwnerAddress());
    hubProfile.setProperty(DEED_OWNER_ADDRESS, hub.getDeedOwnerAddress());
    hubProfile.setProperty(DEED_MANAGER_ADDRESS, hub.getDeedManagerAddress());
    hubProfile.setProperty(START_JOIN_DATE, String.valueOf(hub.getCreatedDate().toEpochMilli()));
    if (hub.getUntilDate() == null) {
      hubProfile.removeProperty(END_JOIN_DATE);
    } else {
      hubProfile.setProperty(END_JOIN_DATE, String.valueOf(hub.getUntilDate().toEpochMilli()));
    }
    hubProfile.setProperty(UPDATED_DATE, String.valueOf(hub.getUpdatedDate().toEpochMilli()));
    hubProfile.setProperty(HUB_ENABLED, String.valueOf(hub.isConnected()));
    identityManager.updateProfile(hubProfile);
  }

  private Hub mapToHub(Profile hubProfile) {
    String deedId = (String) hubProfile.getProperty(DEED_ID);
    String city = (String) hubProfile.getProperty(DEED_CITY);
    String type = (String) hubProfile.getProperty(DEED_TYPE);
    String address = (String) hubProfile.getProperty(ADDRESS);
    String name = (String) hubProfile.getProperty(NAME);
    String description = (String) hubProfile.getProperty(DESCRIPTION);
    String url = (String) hubProfile.getProperty(URL);
    String rewardsAmount = (String) hubProfile.getProperty(REWARD_AMOUNT);
    String rewardsPeriodType = (String) hubProfile.getProperty(REWARD_PERIOD_TYPE);
    String usersCount = (String) hubProfile.getProperty(USERS_COUNT);
    String color = (String) hubProfile.getProperty(COLOR);
    String earnerAddress = (String) hubProfile.getProperty(EARNER_ADDRESS);
    String hubOwnerAddress = (String) hubProfile.getProperty(HUB_OWNER_ADDRESS);
    String deedOwnerAddress = (String) hubProfile.getProperty(DEED_OWNER_ADDRESS);
    String deedManagerAddress = (String) hubProfile.getProperty(DEED_MANAGER_ADDRESS);
    Instant joinDate = parseInstant(hubProfile, START_JOIN_DATE);
    Instant untilDate = parseInstant(hubProfile, END_JOIN_DATE);
    Instant updatedDate = parseInstant(hubProfile, UPDATED_DATE);
    boolean enabled = parseBoolean((String) hubProfile.getProperty(HUB_ENABLED), true)
                      && (untilDate == null || untilDate.isAfter(Instant.now()));

    return new Hub(Long.parseLong(deedId),
                   Short.parseShort(city),
                   Short.parseShort(type),
                   address,
                   parseMap(name),
                   parseMap(description),
                   url,
                   color,
                   hubOwnerAddress,
                   deedOwnerAddress,
                   deedManagerAddress,
                   earnerAddress,
                   joinDate,
                   untilDate,
                   updatedDate,
                   parseLong(usersCount),
                   rewardsPeriodType,
                   parseDouble(rewardsAmount),
                   enabled);
  }

  private void clearHubProperties(Profile hubProfile) {
    if (hubProfile.getIdentity() != null && StringUtils.isNotBlank(hubProfile.getIdentity().getId())) {
      hubProfile.removeProperty(DEED_ID);
      hubProfile.removeProperty(DEED_CITY);
      hubProfile.removeProperty(DEED_TYPE);
      hubProfile.removeProperty(EARNER_ADDRESS);
      hubProfile.removeProperty(DEED_OWNER_ADDRESS);
      hubProfile.removeProperty(DEED_MANAGER_ADDRESS);
      hubProfile.removeProperty(START_JOIN_DATE);
      hubProfile.removeProperty(END_JOIN_DATE);
      hubProfile.removeProperty(UPDATED_DATE);
      hubProfile.removeProperty(REWARD_PERIOD_TYPE);
      hubProfile.removeProperty(REWARD_AMOUNT);
      hubProfile.removeProperty(USERS_COUNT);
      hubProfile.removeProperty(HUB_ENABLED);
      identityManager.updateProfile(hubProfile);
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, String> parseMap(String value) {
    try {
      return StringUtils.isBlank(value) ? Collections.emptyMap() : (Map<String, String>) fromJsonString(value, Map.class);
    } catch (Exception e) {
      throw new IllegalStateException("Error parsing value: " + value, e);
    }
  }

  private Instant parseInstant(Profile hubProfile, String propName) {
    String propDateMillis = (String) hubProfile.getProperty(propName);
    return StringUtils.isBlank(propDateMillis) ? null : Instant.ofEpochMilli(Long.parseLong(propDateMillis));
  }

  private double parseDouble(String value) {
    return StringUtils.isBlank(value) ? 0 : Double.parseDouble(value);
  }

  private long parseLong(String value) {
    return StringUtils.isBlank(value) ? 0 : Long.parseLong(value);
  }

  private boolean parseBoolean(String value, boolean defaulValue) {
    return StringUtils.isBlank(value) ? defaulValue : StringUtils.equals(value, "true");
  }

}