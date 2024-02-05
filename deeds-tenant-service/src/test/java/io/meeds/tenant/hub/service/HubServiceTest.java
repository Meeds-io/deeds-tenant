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
package io.meeds.tenant.hub.service;

import static io.meeds.tenant.hub.service.HubService.PUBLIC_ACCESS_PERMISSION;
import static io.meeds.tenant.hub.service.HubService.PUBLIC_HUB_SUMMARY_SETTING_NAME;
import static io.meeds.tenant.hub.service.HubService.PUBLIC_SITE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.web3j.utils.EnsUtils;

import org.exoplatform.portal.branding.BrandingService;
import org.exoplatform.portal.branding.model.Logo;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.wiki.model.Page;

import io.meeds.notes.service.NotePageViewService;
import io.meeds.social.cms.model.CMSSetting;
import io.meeds.social.cms.service.CMSService;
import io.meeds.tenant.hub.model.HubTenant;
import io.meeds.tenant.hub.rest.client.WomClientService;
import io.meeds.tenant.hub.storage.HubIdentityStorage;
import io.meeds.tenant.hub.storage.HubWalletStorage;
import io.meeds.wom.api.constant.WomException;
import io.meeds.wom.api.model.WomConnectionRequest;
import io.meeds.wom.api.model.WomConnectionResponse;
import io.meeds.wom.api.model.WomDisconnectionRequest;

@SpringBootTest(classes = {
                            HubService.class,
})
@ExtendWith(MockitoExtension.class)
public class HubServiceTest {

  private static final String PUBLIC_DESCRIPTION_CMS_SETTING_NAME = "publicDescription";

  @MockBean
  private HubIdentityStorage  hubIdentityStorage;

  @MockBean
  private HubWalletStorage    hubWalletStorage;

  @MockBean
  private BrandingService     brandingService;

  @MockBean
  private WomClientService    womServiceClient;

  @MockBean
  private CMSService          cmsService;

  @MockBean
  private LocaleConfigService localeConfigService;

  @MockBean
  private LayoutService       layoutService;

  @MockBean
  private NotePageViewService notePageViewService;

  @Mock
  private HubTenant           hub;

  @Mock
  private PortalConfig        portalConfig;

  @Mock
  private CMSSetting          setting;

  @Mock
  private LocaleConfig        localeConfig;

  @Mock
  private Page                note;

  @Mock
  private Logo                logo;

  @Autowired
  private HubService          hubService;

  private long                deedId                              = 35l;

  private short               city                                = 1;

  private short               cardType                            = 3;

  private String              deedManagerAddress                  = "0x609a6f01b7976439603356e41d5456b42df957b7";

  private String              tokenId                             = "16968669685-8666328585-877411225";

  private String              companyName                         = "companyName";

  private String              defaultLanguage                     = "fr";

  private String              noteContent                         = "Description";

  private Map<String, String> themeStyle                          = Collections.singletonMap("primaryColor", "#554863");

  private String              hubAddress                          = "0x33f2694eC1eB5EEC902e6EA1De0A905aAcBaBb49";

  private String              womAddress                          = "0x3Ef2694eC1eB5EEC902e6EA1De0A905aAcBaBb49";

  private String              uemAddress                          = "0x290b11b1ab6a31fF95490e4e0EeffEC6402cce99";

  private long                networkId                           = 80001l;

  private String              rawMessage                          = "rawMessage";

  private String              signedMessage                       = "signedMessage";

  @Test
  public void getHubAddress() {
    String address = hubService.getHubAddress();
    assertNull(address);
    when(hubIdentityStorage.getHubAddress()).thenReturn(hubAddress);
    address = hubService.getHubAddress();
    assertEquals(hubAddress, address);
  }

  @Test
  public void isConnected() {
    assertFalse(hubService.isConnected());

    when(hubIdentityStorage.getHub(false)).thenReturn(hub);
    assertFalse(hubService.isConnected());

    when(hub.getDeedId()).thenReturn(deedId);
    assertFalse(hubService.isConnected());

    when(hub.isConnected()).thenReturn(true);
    assertTrue(hubService.isConnected());

    when(hub.getUntilDate()).thenReturn(Instant.now().minusSeconds(1));
    assertFalse(hubService.isConnected());
  }

  @Test
  public void getHub() {
    when(hubIdentityStorage.getHub(false)).thenReturn(hub);
    assertEquals(hub, hubService.getHub());
  }

  @Test
  public void getHubNoRefresh() {
    when(hubIdentityStorage.getHub(false)).thenReturn(hub);
    assertEquals(hub, hubService.getHub(false));
  }

  public void getHubWithRefresh() {
    when(hubIdentityStorage.getHub(true)).thenReturn(hub);
    assertEquals(hub, hubService.getHub(true));
  }

  @Test
  public void getDeedId() {
    assertEquals(-1l, hubService.getDeedId());

    when(hubIdentityStorage.getHub(false)).thenReturn(hub);
    assertEquals(-1l, hubService.getDeedId());

    when(hub.getDeedId()).thenReturn(deedId);
    assertEquals(-1l, hubService.getDeedId());

    when(hub.isConnected()).thenReturn(true);
    assertEquals(deedId, hubService.getDeedId());

    when(hub.getUntilDate()).thenReturn(Instant.now().minusSeconds(1));
    assertEquals(-1l, hubService.getDeedId());
  }

  @Test
  public void getDeedCity() {
    assertEquals(-1l, hubService.getDeedCity());

    when(hubIdentityStorage.getHub(false)).thenReturn(hub);
    assertEquals(-1l, hubService.getDeedCity());

    when(hub.getDeedId()).thenReturn(deedId);
    when(hub.getCity()).thenReturn(city);
    assertEquals(-1l, hubService.getDeedCity());

    when(hub.isConnected()).thenReturn(true);
    assertEquals(city, hubService.getDeedCity());

    when(hub.getUntilDate()).thenReturn(Instant.now().minusSeconds(1));
    assertEquals(-1l, hubService.getDeedCity());
  }

  @Test
  public void getDeedType() {
    assertEquals(-1l, hubService.getDeedType());

    when(hubIdentityStorage.getHub(false)).thenReturn(hub);
    assertEquals(-1l, hubService.getDeedType());

    when(hub.getDeedId()).thenReturn(deedId);
    when(hub.getType()).thenReturn(cardType);
    assertEquals(-1l, hubService.getDeedType());

    when(hub.isConnected()).thenReturn(true);
    assertEquals(cardType, hubService.getDeedType());

    when(hub.getUntilDate()).thenReturn(Instant.now().minusSeconds(1));
    assertEquals(-1l, hubService.getDeedType());
  }

  @Test
  public void isDeedManager() {
    assertFalse(hubService.isDeedManager(null));
    assertFalse(hubService.isDeedManager(deedManagerAddress));

    when(hub.getDeedManagerAddress()).thenReturn(deedManagerAddress);
    assertFalse(hubService.isDeedManager(deedManagerAddress));

    when(hubIdentityStorage.getHub(false)).thenReturn(hub);
    assertFalse(hubService.isDeedManager(deedManagerAddress));

    when(hub.isConnected()).thenReturn(true);
    assertFalse(hubService.isDeedManager(deedManagerAddress));

    when(hub.getDeedId()).thenReturn(deedId);
    assertTrue(hubService.isDeedManager(deedManagerAddress));
    assertFalse(hubService.isDeedManager(null));
    assertFalse(hubService.isDeedManager(EnsUtils.EMPTY_ADDRESS));
    assertTrue(hubService.isDeedManager(deedManagerAddress.toUpperCase()));
    assertTrue(hubService.isDeedManager(deedManagerAddress.toLowerCase()));
    assertFalse(hubService.isDeedManager(deedManagerAddress.replace("1", "2")));

    when(hub.getUntilDate()).thenReturn(Instant.now().minusSeconds(1));
    assertFalse(hubService.isDeedManager(deedManagerAddress));
  }

  @Test
  public void isDeedManagerByNftId() throws WomException {
    assertFalse(hubService.isDeedManager(null, deedId));
    assertFalse(hubService.isDeedManager(deedManagerAddress, deedId));
    when(womServiceClient.isDeedManager(deedManagerAddress, deedId)).thenReturn(true);
    assertTrue(hubService.isDeedManager(deedManagerAddress, deedId));
  }

  @Test
  public void generateWomToken() throws WomException {
    assertNull(hubService.generateWomToken());
    when(hubService.generateWomToken()).thenReturn(tokenId);
    assertEquals(tokenId, hubService.generateWomToken());
  }

  @Test
  public void getHubJoinDate() {
    assertNull(hubService.getHubJoinDate());

    when(hubIdentityStorage.getHub(false)).thenReturn(hub);
    assertNull(hubService.getHubJoinDate());

    when(hub.getJoinDate()).thenReturn(Instant.now());
    when(hub.isConnected()).thenReturn(true);
    assertNull(hubService.getHubJoinDate());

    when(hub.getDeedId()).thenReturn(deedId);
    assertEquals(hub.getJoinDate(), hubService.getHubJoinDate());

    when(hub.getUntilDate()).thenReturn(Instant.now().minusSeconds(1));
    assertNull(hubService.getHubJoinDate());
  }

  @Test
  public void getDeedManager() {
    assertNull(hubService.getDeedManager());

    when(hubIdentityStorage.getHub(false)).thenReturn(hub);
    assertNull(hubService.getDeedManager());

    when(hub.getDeedManagerAddress()).thenReturn(deedManagerAddress);
    assertNull(hubService.getDeedManager());

    when(hub.isConnected()).thenReturn(true);
    assertNull(hubService.getDeedManager());

    when(hub.getDeedId()).thenReturn(deedId);
    assertEquals(deedManagerAddress, hubService.getDeedManager());

    when(hub.getUntilDate()).thenReturn(Instant.now().minusSeconds(1));
    assertNull(hubService.getDeedManager());
  }

  @Test
  public void connectToWoM() throws WomException  {
    when(hubWalletStorage.getOrCreateHubAddress()).thenReturn(hubAddress);
    when(hubIdentityStorage.getHubAddress()).thenReturn(hubAddress);
    setHubCardProperties();
    WomConnectionResponse response =  new WomConnectionResponse(deedId, hubAddress, womAddress, uemAddress, networkId);
    when(womServiceClient.connectToWom(any())).thenReturn(response);
    when(womServiceClient.generateToken()).thenReturn(tokenId);

    WomConnectionRequest connectionRequest = new WomConnectionRequest(null, null, rawMessage, tokenId);
    hubService.connectToWoM(connectionRequest);

    verify(hubIdentityStorage).saveHubConnectionResponse(argThat(connectionResponse -> connectionResponse != null && connectionResponse.equals(response)));
    verify(womServiceClient, never()).saveHubAvatar(any(), any(), any(), any());

    when(logo.getData()).thenReturn(new byte[3]);

    hubService.connectToWoM(connectionRequest);
    verify(womServiceClient, never()).saveHubAvatar(any(), any(), any(), any());

    when(brandingService.getLogo()).thenReturn(logo);
    hubService.connectToWoM(connectionRequest);
    verify(womServiceClient).saveHubAvatar(eq(hubAddress), anyString(), anyString(), any());
  }

  @Test
  public void disconnectFromWomWhenDisconnected() {
    WomDisconnectionRequest disconnectionRequest = new WomDisconnectionRequest(hubAddress,
                                                                               deedManagerAddress,
                                                                               signedMessage,
                                                                               rawMessage,
                                                                               tokenId);
    assertThrows(WomException.class, () -> hubService.disconnectFromWom(disconnectionRequest));
    verify(hubIdentityStorage).refreshHubIdentity();
  }

  @Test
  public void disconnectFromWomWhenErrorThrown() throws WomException {
    when(hubIdentityStorage.getHubAddress()).thenReturn(hubAddress);
    setHubConnected();
    
    WomDisconnectionRequest disconnectionRequest = new WomDisconnectionRequest(hubAddress,
                                                                               deedManagerAddress,
                                                                               signedMessage,
                                                                               rawMessage,
                                                                               tokenId);
    
    when(womServiceClient.disconnectFromWom(disconnectionRequest)).thenThrow(WomException.class);
    assertThrows(WomException.class, () -> hubService.disconnectFromWom(disconnectionRequest));
    verify(hubIdentityStorage).refreshHubIdentity();
    verify(womServiceClient).disconnectFromWom(disconnectionRequest);
  }

  @Test
  public void disconnectFromWom() throws WomException {
    when(hubIdentityStorage.getHubAddress()).thenReturn(hubAddress);
    setHubConnected();

    WomDisconnectionRequest disconnectionRequest = new WomDisconnectionRequest(hubAddress,
                                                                               deedManagerAddress,
                                                                               signedMessage,
                                                                               rawMessage,
                                                                               tokenId);

    hubService.disconnectFromWom(disconnectionRequest);
    verify(hubIdentityStorage).refreshHubIdentity();
    verify(womServiceClient).disconnectFromWom(disconnectionRequest);
  }

  @Test
  public void updateHubCardWhenNotConnected() throws WomException {
    hubService.updateHubCard();
    verifyNoInteractions(womServiceClient);
  }

  @Test
  public void updateHubCardWhenChanged() throws WomException {
    when(hubIdentityStorage.getHubAddress()).thenReturn(hubAddress);
    setHubConnected();
    setHubCardProperties();

    long logoUpdateTime = System.currentTimeMillis();

    when(brandingService.getLogo()).thenReturn(logo);
    when(logo.getData()).thenReturn(new byte[3]);
    when(logo.getUpdatedDate()).thenReturn(logoUpdateTime);

    when(womServiceClient.generateToken()).thenReturn(tokenId);

    when(hub.clone()).thenAnswer(invocation -> cloneHub(hub));
    hubService.updateHubCard();
    verify(womServiceClient).saveHub(eq(hub), anyString(), eq(tokenId));
    verify(hubIdentityStorage).saveHubAvatarUpdateTime(logoUpdateTime);
  }

  private void setHubConnected() {
    when(hubIdentityStorage.getHub(false)).thenReturn(hub);
    when(hub.isConnected()).thenReturn(true);
    when(hub.getDeedId()).thenReturn(deedId);
  }

  private void setHubCardProperties() throws WomException {
    when(hubWalletStorage.signHubMessage(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(brandingService.getCompanyName()).thenReturn(companyName);
    when(brandingService.getThemeStyle()).thenReturn(themeStyle);
    when(layoutService.getPortalConfig(PUBLIC_SITE_NAME)).thenReturn(portalConfig);
    when(portalConfig.getAccessPermissions()).thenReturn(new String[]{PUBLIC_ACCESS_PERMISSION});
    when(cmsService.getSetting(NotePageViewService.CMS_CONTENT_TYPE, PUBLIC_HUB_SUMMARY_SETTING_NAME)).thenReturn(setting);
    when(setting.getName()).thenReturn(PUBLIC_DESCRIPTION_CMS_SETTING_NAME);
    when(localeConfig.getLanguage()).thenReturn(defaultLanguage);
    when(localeConfigService.getDefaultLocaleConfig()).thenReturn(localeConfig);
    when(notePageViewService.getNotePage(setting.getName(), defaultLanguage)).thenReturn(note);
    when(note.getContent()).thenReturn(noteContent);
  }

  private HubTenant cloneHub(HubTenant hub) {
    return new HubTenant(hub.getDeedId(),
                         hub.getCity(),
                         hub.getType(),
                         hub.getAddress(),
                         hub.getName(),
                         hub.getDescription(),
                         hub.getUrl(),
                         hub.getColor(),
                         hub.getHubOwnerAddress(),
                         hub.getDeedOwnerAddress(),
                         hub.getDeedManagerAddress(),
                         hub.getCreatedDate(),
                         hub.getUntilDate(),
                         hub.getJoinDate(),
                         hub.getUpdatedDate(),
                         hub.getUsersCount(),
                         hub.getRewardsPeriodType(),
                         hub.getRewardsPerPeriod(),
                         hub.isConnected(),
                         hub.getOwnerClaimableAmount(),
                         hub.getManagerClaimableAmount(),
                         hub.getWomServerUrl(),
                         hub.getAdminAddress(),
                         hub.getWomAddress(),
                         hub.getUemAddress(),
                         hub.getNetworkId(),
                         hub.getAvatarUpdateTime());
  }

}
