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

import static io.meeds.wom.api.utils.JsonUtils.toJsonString;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.portal.branding.BrandingService;
import org.exoplatform.portal.branding.model.Logo;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.UserStatus;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.wallet.model.reward.RewardPeriodType;
import org.exoplatform.wallet.model.reward.RewardSettings;
import org.exoplatform.wallet.reward.service.RewardSettingsService;
import org.exoplatform.wallet.service.WalletAccountService;
import org.exoplatform.wiki.model.Page;

import io.meeds.notes.service.NotePageViewService;
import io.meeds.social.cms.model.CMSSetting;
import io.meeds.social.cms.service.CMSService;
import io.meeds.tenant.hub.model.HubConfiguration;
import io.meeds.tenant.hub.rest.client.WomClientService;
import io.meeds.tenant.hub.storage.HubIdentityStorage;
import io.meeds.tenant.hub.storage.HubWalletStorage;
import io.meeds.wom.api.constant.WomException;
import io.meeds.wom.api.model.Hub;
import io.meeds.wom.api.model.WomConnectionRequest;
import io.meeds.wom.api.model.WomDisconnectionRequest;

@Service
public class HubService {

  public static final int       MAX_START_TENTATIVES            = 5;

  public static final String    MANAGER_DEFAULT_ROLES_PARAM     = "managerDefaultRoles";

  public static final String    PUBLIC_SITE_NAME                = "public";

  public static final String    PUBLIC_ACCESS_PERMISSION        = "Everyone";

  public static final String    PUBLIC_HUB_SUMMARY_SETTING_NAME = "publicHubSummary";

  private static final Log      LOG                             = ExoLogger.getLogger(HubService.class);

  @Autowired
  private OrganizationService   organizationService;

  @Autowired
  private RewardSettingsService rewardSettingsService;

  @Autowired
  private WalletAccountService  walletAccountService;

  @Autowired
  private HubIdentityStorage    hubIdentityStorage;

  @Autowired
  private HubWalletStorage      hubWalletStorage;

  @Autowired
  private BrandingService       brandingService;

  @Autowired
  private WomClientService      womServiceClient;

  @Autowired
  private CMSService            cmsService;

  @Autowired
  private LocaleConfigService   localeConfigService;

  @Autowired
  private LayoutService         layoutService;

  @Autowired
  private NotePageViewService   notePageViewService;

  private long                  synchronizeLogoUpdateDate;

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

  public Hub getHub() {
    return getHub(false);
  }

  public Hub getHub(boolean forceRefresh) {
    return hubIdentityStorage.getHub(forceRefresh);
  }

  public long getDeedId() {
    return isConnected() ? getHub().getDeedId() : -1; // NOSONAR
  }

  public short getDeedCity() {
    return isConnected() ? getHub().getCity() : null; // NOSONAR
  }

  public short getDeedType() {
    return isConnected() ? getHub().getType() : null; // NOSONAR
  }

  public String getDeedManager() {
    return isConnected() ? getHub().getDeedManagerAddress() : null;
  }

  public Instant getHubJoinDate() {
    return isConnected() ? getHub().getCreatedDate() : null;
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

  public Hub getHub(long nftId) throws WomException {
    return womServiceClient.getHub(nftId);
  }

  public String generateWomToken() throws WomException {
    return womServiceClient.generateToken();
  }

  public HubConfiguration getConfiguration() {
    HubConfiguration hubConfiguration = new HubConfiguration();
    hubConfiguration.setAdminWallet(walletAccountService.getAdminWallet().getAddress());
    hubConfiguration.setHubAddress(getHubAddress());
    hubConfiguration.setUsersCount(computeUsersCount());
    hubConfiguration.setRewardsPeriodType(getRewardsPeriodType().name());
    hubConfiguration.setWomServerUrl(womServiceClient.getWomUrl());
    return hubConfiguration;
  }

  public String connectToWoM(WomConnectionRequest connectionRequest) throws WomException { // NOSONAR
    try {
      String address = hubWalletStorage.getOrCreateHubAddress();
      connectionRequest.setAddress(address);
      if (StringUtils.isBlank(connectionRequest.getEarnerAddress())) {
        connectionRequest.setEarnerAddress(walletAccountService.getAdminWallet().getAddress());
      }
      connectionRequest.setUsersCount(computeUsersCount());
      connectionRequest.setHubSignedMessage(signHubMessage(connectionRequest.getRawMessage()));
      setHubCardProperties(connectionRequest);
      String womAddress = womServiceClient.connectToWom(connectionRequest);
      try {
        saveHubAvatar();
      } catch (Exception e) {
        LOG.warn("Error while saving Hub Avatar. This isn't be blocker, thus continue processing WoM Connection",
                 e);
      }
      return womAddress;
    } finally {
      hubIdentityStorage.refreshHubIdentity();
    }
  }

  public void disconnectFromWom(WomDisconnectionRequest disconnectionRequest) throws WomException {
    if (!isConnected()) {
      throw new WomException("wom.alreadyDisconnected");
    }
    try {
      disconnectionRequest.setHubAddress(getHubAddress());
      womServiceClient.disconnectFromWom(disconnectionRequest);
    } finally {
      hubIdentityStorage.refreshHubIdentity();
    }
  }

  public void updateHubCard() throws WomException { // NOSONAR
    if (!isConnected()) {
      return;
    }
    LOG.info("Updating Hub Card");

    String token = womServiceClient.generateToken();
    String hubSignedMessage = signHubMessage(token);

    try {
      Hub hub = getHub();
      Hub original = hub.clone();
      setHubCardProperties(hub);
      if (!original.equals(hub)) {
        LOG.debug("Updating Hub Card on WoM Server");
        womServiceClient.saveHub(hub, hubSignedMessage, token);
      }
      long logoUpdateDate = getLogoUpdateDate();
      if (synchronizeLogoUpdateDate == 0) {
        synchronizeLogoUpdateDate = logoUpdateDate;
      } else if (synchronizeLogoUpdateDate != logoUpdateDate) {
        LOG.debug("Updating Hub Card Avatar on WoM Server");
        saveHubAvatar();
        synchronizeLogoUpdateDate = logoUpdateDate;
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
    String signedMessage = signHubMessage(token);

    womServiceClient.saveHubAvatar(hubAddress,
                                   signedMessage,
                                   token,
                                   new ByteArrayInputStream(logo.getData()));
  }

  public long computeUsersCount() {
    try {
      return organizationService.getUserHandler().findAllUsers(UserStatus.ENABLED).getSize();
    } catch (Exception e) {
      LOG.warn("Error computing Hub users count information, retrieve already computed data", e);
      return 0;
    }
  }

  public String signHubMessage(Object object) throws WomException {
    String rawRequest = toJsonString(object);
    return signHubMessage(rawRequest);
  }

  private String signHubMessage(String rawRequest) throws WomException {
    byte[] encodedRequest = rawRequest.getBytes(StandardCharsets.UTF_8);
    Sign.SignatureData signatureData = Sign.signPrefixedMessage(encodedRequest, hubWalletStorage.getHubWallet());
    byte[] retval = new byte[65];
    System.arraycopy(signatureData.getR(), 0, retval, 0, 32);
    System.arraycopy(signatureData.getS(), 0, retval, 32, 32);
    System.arraycopy(signatureData.getV(), 0, retval, 64, 1);
    return Numeric.toHexString(retval);
  }

  private RewardPeriodType getRewardsPeriodType() {
    RewardSettings settings = rewardSettingsService.getSettings();
    return settings.getPeriodType();
  }

  private void setHubCardProperties(Hub hub) {
    String enLanguage = Locale.ENGLISH.toLanguageTag();
    hub.setName(Collections.singletonMap(enLanguage, brandingService.getCompanyName()));
    hub.setDescription(Collections.singletonMap(enLanguage, getPublicDescription(enLanguage)));
    hub.setColor(brandingService.getThemeStyle().get("primaryColor"));
    hub.setUrl(CommonsUtils.getCurrentDomain());
  }

  private String getPublicDescription(String enLanguage) {
    Page note = null;
    if (isPublisSitePublished()) {
      CMSSetting setting = cmsService.getSetting(NotePageViewService.CMS_CONTENT_TYPE, PUBLIC_HUB_SUMMARY_SETTING_NAME);
      if (setting != null) {
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
    return logo == null ? synchronizeLogoUpdateDate : logo.getUpdatedDate();
  }

  private boolean isAfterNow(Instant untilDate) {
    return untilDate == null || untilDate.isAfter(Instant.now());
  }

}
