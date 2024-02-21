/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.tenant.hub.storage;

import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.ADDRESS;
import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.CREATED_DATE;
import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.DEED_CITY;
import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.DEED_ID;
import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.DEED_MANAGER_ADDRESS;
import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.DEED_OWNER_ADDRESS;
import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.DEED_TYPE;
import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.DESCRIPTION;
import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.END_JOIN_DATE;
import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.HUB_AVATAR_UPDATE;
import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.HUB_ENABLED;
import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.HUB_OWNER_ADDRESS;
import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.IDENTITY_PROVIDER_NAME;
import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.IDENTITY_REMOTE_ID;
import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.MANAGER_CLAIMABLE_AMOUNT;
import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.NAME;
import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.OWNER_CLAIMABLE_AMOUNT;
import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.REWARD_AMOUNT;
import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.REWARD_PERIOD_TYPE;
import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.UEM_CONTRACT_ADDRESS;
import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.UPDATED_DATE;
import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.URL;
import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.USERS_COUNT;
import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.WALLET;
import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.WOM_CONTRACT_ADDRESS;
import static io.meeds.tenant.hub.plugin.WalletHubIdentityProvider.WOM_NETWORK_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.wallet.service.WalletTokenAdminService;

import io.meeds.tenant.hub.model.HubTenant;
import io.meeds.tenant.hub.rest.client.WomClientService;
import io.meeds.wom.api.constant.WomException;
import io.meeds.wom.api.model.Hub;
import io.meeds.wom.api.model.WomConnectionResponse;

@SpringBootTest(classes = {
                            HubIdentityStorage.class,
})
@ExtendWith(MockitoExtension.class)
class HubIdentityStorageTest {

  @MockBean
  private IdentityManager         identityManager;

  @MockBean
  private WomClientService        womServiceClient;

  @MockBean
  private WalletTokenAdminService walletTokenAdminService;

  @MockBean
  private ListenerService         listenerService;

  @Autowired
  private HubIdentityStorage      hubIdentityStorage;

  @Mock
  private Identity                hubIdentity;

  @Mock
  private Hub                     hubFromWom;

  private Profile                 hubProfile;

  private String                  hubIdentityId          = "1542";

  private String                  hubAddress             = "0x27d282d1e7e790df596f50a234602d9e761d22aa";

  private short                   city                   = 1;

  private short                   cardType               = 3;

  private double                  managerClaimableAmount = 0.225d;

  private double                  ownerClaimableAmount   = 0.225d;

  private Instant                 createdDate            = Instant.now();

  private Instant                 updatedDate            = Instant.now();

  private Instant                 joinDate               = Instant.now();

  private long                    deedId                 = 35l;

  private long                    usersCount             = 125l;

  private double                  rewardsPerPeriod       = 100d;

  private String                  rewardsPeriodType      = "WEEK";

  private String                  deedManagerAddress     = "0xB36b174DC531B8055631A4E8d32f44eADC1B9695";

  private String                  color                  = "#544863";

  private String                  name                   = "name";

  private String                  url                    = "url";

  private String                  description            = "description";

  private String                  ownerAddress           = "0x27D282d1e7e790df596f50a234602d9e761d22aa";

  private String                  hubOwnerAddress        = "0x609a6F01b7976439603356e41d5456b42df957b7";

  @BeforeEach
  void init() {
    hubIdentityStorage.refreshHubIdentity();

    when(identityManager.getOrCreateIdentity(IDENTITY_PROVIDER_NAME, IDENTITY_REMOTE_ID)).thenReturn(hubIdentity);
    hubProfile = new Profile(hubIdentity);
    hubProfile.setProperty(ADDRESS, hubAddress);
    lenient().when(hubIdentity.getProfile()).thenReturn(hubProfile);
    lenient().when(hubIdentity.getId()).thenReturn(hubIdentityId);
  }

  @Test
  void getHubWhenNotExistsInWom() {
    hubProfile = mock(Profile.class);
    when(hubIdentity.getProfile()).thenReturn(hubProfile);
    when(hubProfile.getProperty(any())).thenReturn(null);
    when(hubProfile.getProperty(ADDRESS)).thenReturn(hubAddress);
    when(hubProfile.getProperty(NAME)).thenReturn("{\"name\":\"" + name + "\"}"); // NOSONAR
    when(hubProfile.getIdentity()).thenReturn(hubIdentity);
    when(hubProfile.getIdentity().getId()).thenReturn(hubIdentityId);

    HubTenant hub = hubIdentityStorage.getHub();
    assertNotNull(hub);

    // Should remove those properties if hub doesn't exist in WoM
    verify(hubProfile).removeProperty(DEED_ID);
    verify(hubProfile).removeProperty(DEED_CITY);
    verify(hubProfile).removeProperty(DEED_TYPE);
    verify(hubProfile).removeProperty(DEED_OWNER_ADDRESS);
    verify(hubProfile).removeProperty(DEED_MANAGER_ADDRESS);
    verify(hubProfile).removeProperty(END_JOIN_DATE);
    verify(hubProfile).removeProperty(REWARD_PERIOD_TYPE);
    verify(hubProfile).removeProperty(REWARD_AMOUNT);
    verify(hubProfile).removeProperty(OWNER_CLAIMABLE_AMOUNT);
    verify(hubProfile).removeProperty(MANAGER_CLAIMABLE_AMOUNT);
    verify(hubProfile).removeProperty(USERS_COUNT);
    verify(hubProfile).removeProperty(HUB_ENABLED);
    verify(hubProfile).removeProperty(HUB_AVATAR_UPDATE);
    verify(identityManager).updateProfile(hubProfile);

    // Shouldn't change already retrieve and
    // once connected those properties to allow
    // display card as disabled with previous
    // characteristics
    verify(hubProfile, never()).removeProperty(NAME);
    verify(hubProfile, never()).removeProperty(DESCRIPTION);
    verify(hubProfile, never()).removeProperty(URL);
    verify(hubProfile, never()).removeProperty(HUB_OWNER_ADDRESS);
    verify(hubProfile, never()).removeProperty(CREATED_DATE);
    verify(hubProfile, never()).removeProperty(UPDATED_DATE);

    assertEquals(hubAddress, hub.getAddress());
  }

  @Test
  void getHubWhenExistsInWomButDisconnected() throws WomException {
    when(womServiceClient.getHub(hubAddress, false)).thenReturn(hubFromWom);

    when(hubFromWom.getCity()).thenReturn(city);
    when(hubFromWom.getDeedId()).thenReturn(deedId);
    when(hubFromWom.getType()).thenReturn(cardType);
    when(hubFromWom.getColor()).thenReturn(color);
    when(hubFromWom.getCreatedDate()).thenReturn(createdDate);
    when(hubFromWom.getUpdatedDate()).thenReturn(updatedDate);
    when(hubFromWom.getHubOwnerAddress()).thenReturn(hubOwnerAddress);
    when(hubFromWom.getDeedManagerAddress()).thenReturn(deedManagerAddress);
    when(hubFromWom.getDeedOwnerAddress()).thenReturn(ownerAddress);
    when(hubFromWom.getDescription()).thenReturn(Collections.singletonMap("en", description));
    when(hubFromWom.getName()).thenReturn(Collections.singletonMap("en", name));
    when(hubFromWom.getJoinDate()).thenReturn(joinDate);
    when(hubFromWom.getManagerClaimableAmount()).thenReturn(managerClaimableAmount);
    when(hubFromWom.getOwnerClaimableAmount()).thenReturn(ownerClaimableAmount);
    when(hubFromWom.getRewardsPeriodType()).thenReturn(rewardsPeriodType);
    when(hubFromWom.getRewardsPerPeriod()).thenReturn(rewardsPerPeriod);
    when(hubFromWom.getUrl()).thenReturn(url);
    when(hubFromWom.getUsersCount()).thenReturn(usersCount);

    HubTenant hub = hubIdentityStorage.getHub();
    assertNotNull(hub);
    verify(identityManager, never()).updateProfile(hubProfile);

    hubProfile.setProperty(NAME, "{\"name\":\"" + name + "\"}");

    hubIdentityStorage.refreshHubIdentity();
    hub = hubIdentityStorage.getHub();
    assertNotNull(hub);
    verify(identityManager).updateProfile(hubProfile);

    assertFalse(hub.isConnected());
    assertEquals(hubAddress, hub.getAddress());
    assertEquals(deedId, hub.getDeedId());
    assertEquals(city, hub.getCity());
    assertEquals(cardType, hub.getType());
    assertEquals(color, hub.getColor());
    assertEquals(StringUtils.lowerCase(hubOwnerAddress), hub.getHubOwnerAddress());
    assertEquals(StringUtils.lowerCase(ownerAddress), hub.getDeedOwnerAddress());
    assertEquals(StringUtils.lowerCase(deedManagerAddress), hub.getDeedManagerAddress());
    assertEquals(Collections.singletonMap("en", description), hub.getDescription());
    assertEquals(Collections.singletonMap("en", name), hub.getName());
    assertEquals(joinDate.getEpochSecond(), hub.getJoinDate().getEpochSecond());
    assertEquals(managerClaimableAmount, hub.getManagerClaimableAmount());
    assertEquals(ownerClaimableAmount, hub.getOwnerClaimableAmount());
    assertEquals(rewardsPeriodType, hub.getRewardsPeriodType());
    assertEquals(rewardsPerPeriod, hub.getRewardsPerPeriod());
    assertEquals(url, hub.getUrl());
    assertEquals(usersCount, hub.getUsersCount());
  }

  @Test
  void getHubWhenExistsInWomWhenConnected() throws WomException {
    when(womServiceClient.getHub(hubAddress, false)).thenReturn(hubFromWom);

    when(hubFromWom.isConnected()).thenReturn(true);
    when(hubFromWom.getCity()).thenReturn(city);
    when(hubFromWom.getDeedId()).thenReturn(deedId);
    when(hubFromWom.getType()).thenReturn(cardType);
    when(hubFromWom.getColor()).thenReturn(color);
    when(hubFromWom.getCreatedDate()).thenReturn(createdDate);
    when(hubFromWom.getUpdatedDate()).thenReturn(updatedDate);
    when(hubFromWom.getHubOwnerAddress()).thenReturn(hubOwnerAddress);
    when(hubFromWom.getDeedManagerAddress()).thenReturn(deedManagerAddress);
    when(hubFromWom.getDeedOwnerAddress()).thenReturn(ownerAddress);
    when(hubFromWom.getDescription()).thenReturn(Collections.singletonMap("en", description));
    when(hubFromWom.getName()).thenReturn(Collections.singletonMap("en", name));
    when(hubFromWom.getJoinDate()).thenReturn(joinDate);
    when(hubFromWom.getManagerClaimableAmount()).thenReturn(managerClaimableAmount);
    when(hubFromWom.getOwnerClaimableAmount()).thenReturn(ownerClaimableAmount);
    when(hubFromWom.getRewardsPeriodType()).thenReturn(rewardsPeriodType);
    when(hubFromWom.getRewardsPerPeriod()).thenReturn(rewardsPerPeriod);
    when(hubFromWom.getUrl()).thenReturn(url);
    when(hubFromWom.getUsersCount()).thenReturn(usersCount);

    HubTenant hub = hubIdentityStorage.getHub();
    assertNotNull(hub);
    verify(identityManager, never()).updateProfile(hubProfile);

    hubProfile.setProperty(NAME, "{\"name\":\"" + name + "\"}");

    hubIdentityStorage.refreshHubIdentity();
    hub = hubIdentityStorage.getHub();
    assertNotNull(hub);
    verify(identityManager).updateProfile(hubProfile);

    assertTrue(hub.isConnected());
    assertEquals(hubAddress, hub.getAddress());
    assertEquals(deedId, hub.getDeedId());
    assertEquals(city, hub.getCity());
    assertEquals(cardType, hub.getType());
    assertEquals(color, hub.getColor());
    assertEquals(StringUtils.lowerCase(hubOwnerAddress), hub.getHubOwnerAddress());
    assertEquals(StringUtils.lowerCase(ownerAddress), hub.getDeedOwnerAddress());
    assertEquals(StringUtils.lowerCase(deedManagerAddress), hub.getDeedManagerAddress());
    assertEquals(Collections.singletonMap("en", description), hub.getDescription());
    assertEquals(Collections.singletonMap("en", name), hub.getName());
    assertEquals(joinDate.getEpochSecond(), hub.getJoinDate().getEpochSecond());
    assertEquals(managerClaimableAmount, hub.getManagerClaimableAmount());
    assertEquals(ownerClaimableAmount, hub.getOwnerClaimableAmount());
    assertEquals(rewardsPeriodType, hub.getRewardsPeriodType());
    assertEquals(rewardsPerPeriod, hub.getRewardsPerPeriod());
    assertEquals(url, hub.getUrl());
    assertEquals(usersCount, hub.getUsersCount());
  }

  @Test
  void getHubAddress() {
    assertEquals(hubAddress, hubIdentityStorage.getHubAddress());
    hubProfile.removeProperty(ADDRESS);
    assertNull(hubIdentityStorage.getHubAddress());
  }

  @Test
  void getHubWallet() {
    assertNull(hubIdentityStorage.getHubWallet());
    String wallet = "Wallet";
    hubProfile.setProperty(WALLET, wallet);
    assertEquals(wallet, hubIdentityStorage.getHubWallet());
  }

  @Test
  void saveHubWallet() {
    String wallet = "Wallet";
    hubIdentityStorage.saveHubWallet(hubAddress, wallet);
    assertEquals(wallet, hubProfile.getProperty(WALLET));
    verify(identityManager).updateProfile(hubProfile);
  }

  @Test
  void saveHubConnectionResponse() {
    String womAddress = "womAddress";
    String uemAddress = "uemAddress";
    long networkId = 12l;
    WomConnectionResponse connectionResponse = new WomConnectionResponse();
    connectionResponse.setWomAddress(womAddress);
    connectionResponse.setUemAddress(uemAddress);
    connectionResponse.setNetworkId(networkId);
    hubIdentityStorage.saveHubConnectionResponse(connectionResponse);
    assertEquals(womAddress, hubProfile.getProperty(WOM_CONTRACT_ADDRESS));
    assertEquals(uemAddress, hubProfile.getProperty(UEM_CONTRACT_ADDRESS));
    assertEquals(networkId, hubProfile.getProperty(WOM_NETWORK_ID));
    verify(identityManager).updateProfile(hubProfile);
  }

  @Test
  void saveHubAvatarUpdateTime() {
    long avatarUpdateTime = System.currentTimeMillis();
    hubIdentityStorage.saveHubAvatarUpdateTime(avatarUpdateTime);
    assertEquals(String.valueOf(avatarUpdateTime), hubProfile.getProperty(HUB_AVATAR_UPDATE));
    verify(identityManager).updateProfile(hubProfile);
  }

}
