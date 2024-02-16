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
package io.meeds.tenant.hub.service;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.portal.branding.BrandingService;
import org.exoplatform.portal.branding.model.Logo;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
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
import io.meeds.wom.api.model.Hub;
import io.meeds.wom.api.model.WomConnectionRequest;
import io.meeds.wom.api.model.WomConnectionResponse;
import io.meeds.wom.api.model.WomDisconnectionRequest;

import lombok.SneakyThrows;

@Service
public class HubService {

  public static final int     MAX_START_TENTATIVES            = 5;

  public static final String  HUB_CONNECTED_EVENT             = "deed.tenant.hub.connected";

  public static final String  HUB_DISCONNECTED_EVENT          = "deed.tenant.hub.disconnected";

  public static final String  HUB_UPDATE_ON_WOM_EVENT         = "deed.tenant.hub.updatedOnWoM";

  public static final String  MANAGER_DEFAULT_ROLES_PARAM     = "managerDefaultRoles";

  public static final String  PUBLIC_SITE_NAME                = "public";

  public static final String  PUBLIC_ACCESS_PERMISSION        = "Everyone";

  public static final String  PUBLIC_HUB_SUMMARY_SETTING_NAME = "publicHubSummary";

  private static final Log    LOG                             = ExoLogger.getLogger(HubService.class);

  @Autowired
  private HubIdentityStorage  hubIdentityStorage;

  @Autowired
  private HubWalletStorage    hubWalletStorage;

  @Autowired
  private BrandingService     brandingService;

  @Autowired
  private WomClientService    womServiceClient;

  @Autowired
  private CMSService          cmsService;

  @Autowired
  private LocaleConfigService localeConfigService;

  @Autowired
  private LayoutService       layoutService;

  @Autowired
  private NotePageViewService notePageViewService;

  @Autowired
  private ListenerService     listenerService;

  public String getHubAddress() {
    return hubIdentityStorage.getHubAddress();
  }

  public boolean isConnected() {
    Hub hub = getHub();
    return hub != null
           && hub.isConnected()
           && hub.getDeedId() > 0
           && isAfterNow(hub.getUntilDate());
  }

  public HubTenant getHub() {
    return getHub(false);
  }

  public HubTenant getHub(boolean forceRefresh) {
    return hubIdentityStorage.getHub(forceRefresh);
  }

  public long getDeedId() {
    return isConnected() ? getHub().getDeedId() : -1;
  }

  public short getDeedCity() {
    return isConnected() ? getHub().getCity() : -1;
  }

  public short getDeedType() {
    return isConnected() ? getHub().getType() : -1;
  }

  public String getDeedManager() {
    return isConnected() ? getHub().getDeedManagerAddress() : null;
  }

  public Instant getHubJoinDate() {
    return isConnected() ? getHub().getJoinDate() : null;
  }

  public boolean isDeedManager(String address) {
    return StringUtils.isNotBlank(address)
           && isConnected()
           && StringUtils.equalsIgnoreCase(getDeedManager(), address);
  }

  public boolean isDeedManager(String address, long nftId) throws WomException {
    if (StringUtils.isNotBlank(address)) {
      return womServiceClient.isDeedManager(address, nftId);
    } else {
      return false;
    }
  }

  public String generateWomToken() throws WomException {
    return womServiceClient.generateToken();
  }

  @SneakyThrows
  public WomConnectionResponse connectToWoM(WomConnectionRequest connectionRequest) throws WomException { // NOSONAR
    try {
      String address = hubWalletStorage.getOrCreateHubAddress();
      connectionRequest.setAddress(address);
      connectionRequest.setHubSignedMessage(hubWalletStorage.signHubMessage(connectionRequest.getRawMessage()));
      setHubCardProperties(connectionRequest);
      WomConnectionResponse connectionResponse = womServiceClient.connectToWom(connectionRequest);
      hubIdentityStorage.saveHubConnectionResponse(connectionResponse);
      try {
        saveHubAvatar();
      } catch (Exception e) {
        LOG.warn("Error while saving Hub Avatar. This isn't be blocker, thus continue processing WoM Connection",
                 e);
      }
      hubIdentityStorage.getHub(true); // Must refresh from wom, thus force
                                       // refresh
      return connectionResponse;
    } finally {
      hubIdentityStorage.refreshHubIdentity();
    }
  }

  @SneakyThrows
  public void disconnectFromWom(WomDisconnectionRequest disconnectionRequest) throws WomException {
    try {
      if (!isConnected()) {
        throw new WomException("wom.alreadyDisconnected");
      }
      HubTenant hub = getHub();
      disconnectionRequest.setHubAddress(hub.getAddress());
      disconnectionRequest.setHubSignedMessage(hubWalletStorage.signHubMessage(disconnectionRequest.getRawMessage()));
      womServiceClient.disconnectFromWom(disconnectionRequest);
    } finally {
      hubIdentityStorage.refreshHubIdentity();
    }
  }

  @SneakyThrows
  public void updateHubCard() throws WomException { // NOSONAR
    if (!isConnected()) {
      return;
    }

    String token = womServiceClient.generateToken();
    String hubSignedMessage = hubWalletStorage.signHubMessage(token);

    try {
      HubTenant hub = getHub();
      HubTenant original = hub.clone();
      setHubCardProperties(hub);
      boolean updated = false;
      if (!original.equals(hub)) {
        LOG.info("Updating Hub Card on WoM Server");
        womServiceClient.saveHub(hub, hubSignedMessage, token);
        updated = true;
      }
      long logoUpdateDate = getLogoUpdateDate();
      if (hub.getAvatarUpdateTime() == 0 || hub.getAvatarUpdateTime() < logoUpdateDate) {
        LOG.info("Updating Hub Card Avatar on WoM Server");
        saveHubAvatar();
        hubIdentityStorage.saveHubAvatarUpdateTime(logoUpdateDate == 0 ? System.currentTimeMillis() : logoUpdateDate);
        updated = true;
      }
      if (updated) {
        listenerService.broadcast(HUB_DISCONNECTED_EVENT, original, hub);
      }
    } finally {
      hubIdentityStorage.refreshHubIdentity();
    }
  }

  public void saveHubAvatar() throws WomException {
    String hubAddress = getHubAddress();
    if (StringUtils.isBlank(hubAddress)) {
      throw new WomException("wom.notConnected");
    }

    Logo logo = brandingService.getLogo();
    if (logo == null || logo.getData() == null || logo.getData().length == 0) {
      return;
    }
    String token = womServiceClient.generateToken();
    String signedMessage = hubWalletStorage.signHubMessage(token);

    womServiceClient.saveHubAvatar(hubAddress,
                                   signedMessage,
                                   token,
                                   new ByteArrayInputStream(logo.getData()));
  }

  private void setHubCardProperties(Hub hub) {
    String enLanguage = Locale.ENGLISH.toLanguageTag();
    hub.setName(Collections.singletonMap(enLanguage, brandingService.getCompanyName()));
    hub.setDescription(Collections.singletonMap(enLanguage, getPublicDescription()));
    hub.setColor(brandingService.getThemeStyle().get("primaryColor"));
    hub.setUrl(CommonsUtils.getCurrentDomain());
  }

  private String getPublicDescription() {
    Page note = null;
    if (isPublisSitePublished()) {
      CMSSetting setting = cmsService.getSetting(NotePageViewService.CMS_CONTENT_TYPE, PUBLIC_HUB_SUMMARY_SETTING_NAME);
      if (setting != null) {
        String enLanguage = Locale.ENGLISH.toLanguageTag();
        String defaultLanguage = localeConfigService.getDefaultLocaleConfig().getLanguage();
        note = notePageViewService.getNotePage(setting.getName(), defaultLanguage);
        if ((note == null || StringUtils.isBlank(note.getContent()))
            && !StringUtils.equals(defaultLanguage, enLanguage)) {
          note = notePageViewService.getNotePage(setting.getName(), enLanguage);
        }
      }
    }
    return note == null || StringUtils.isBlank(note.getContent()) ? "" : note.getContent();
  }

  private boolean isPublisSitePublished() {
    PortalConfig portalConfig = layoutService.getPortalConfig(PUBLIC_SITE_NAME);
    return portalConfig != null
           && portalConfig.getAccessPermissions() != null
           && Arrays.asList(portalConfig.getAccessPermissions()).contains(PUBLIC_ACCESS_PERMISSION);
  }

  private long getLogoUpdateDate() {
    Logo logo = brandingService.getLogo();
    return logo == null ? 0 : logo.getUpdatedDate();
  }

  private boolean isAfterNow(Instant untilDate) {
    return untilDate == null || untilDate.isAfter(Instant.now());
  }

}
