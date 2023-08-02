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
import static io.meeds.tenant.plugin.WalletHubIdentityProvider.LOGO_URL;
import static io.meeds.tenant.plugin.WalletHubIdentityProvider.NAME;
import static io.meeds.tenant.plugin.WalletHubIdentityProvider.URL;
import static io.meeds.tenant.plugin.WalletHubIdentityProvider.WALLET;
import static org.exoplatform.wallet.utils.WalletUtils.toJsonString;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.WalletFile;
import org.web3j.crypto.WalletUtils;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.UserStatus;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.wallet.model.reward.RewardPeriod;
import org.exoplatform.wallet.model.reward.RewardReport;
import org.exoplatform.wallet.model.reward.RewardSettings;
import org.exoplatform.wallet.reward.service.RewardReportService;
import org.exoplatform.wallet.reward.service.RewardSettingsService;
import org.exoplatform.wallet.service.WalletAccountService;

import io.meeds.deeds.model.Hub;
import io.meeds.deeds.model.WomConnectionRequest;
import io.meeds.deeds.model.WomDisconnectionRequest;
import io.meeds.tenant.constant.WomConnectionException;
import io.meeds.tenant.model.HubConfiguration;
import io.meeds.tenant.model.HubReward;
import io.meeds.tenant.rest.client.TenantServiceConsumer;

/**
 * A service that allows to detect Deed Tenant Manager address
 */
public class TenantManagerService {

  private static final Log      LOG                         = ExoLogger.getLogger(TenantManagerService.class);

  public static final int       MAX_START_TENTATIVES        = 5;

  public static final String    MANAGER_DEFAULT_ROLES_PARAM = "managerDefaultRoles";

  private IdentityManager       identityManager;

  private OrganizationService   organizationService;

  private RewardSettingsService rewardSettingsService;

  private RewardReportService   rewardReportService;

  private WalletAccountService  walletAccountService;

  private TenantServiceConsumer tenantServiceConsumer;

  private List<String>          tenantManagerDefaultRoles   = new ArrayList<>();

  private Identity              hubIdentity                 = null;

  public TenantManagerService(IdentityManager identityManager,
                              WalletAccountService walletAccountService,
                              RewardSettingsService rewardSettingsService,
                              RewardReportService rewardReportService,
                              OrganizationService organizationService,
                              TenantServiceConsumer tenantServiceConsumer,
                              InitParams params) {
    this.tenantServiceConsumer = tenantServiceConsumer;
    this.walletAccountService = walletAccountService;
    this.rewardSettingsService = rewardSettingsService;
    this.organizationService = organizationService;
    this.rewardReportService = rewardReportService;
    this.identityManager = identityManager;
    this.tenantManagerDefaultRoles = getParamValues(params, MANAGER_DEFAULT_ROLES_PARAM);
  }

  public List<String> getTenantManagerDefaultRoles() {
    return Collections.unmodifiableList(tenantManagerDefaultRoles);
  }

  public boolean isTenant() {
    return getDeedId() > -1;
  }

  public long getDeedId() {
    String deedId = getHubProfileAttribute(DEED_ID);
    return StringUtils.isBlank(deedId) ? -1 : Long.parseLong(deedId);
  }

  public short getDeedCity() {
    if (isTenant()) {
      String city = getHubProfileAttribute(DEED_CITY);
      return StringUtils.isBlank(city) ? -1 : Short.parseShort(city);
    } else {
      return -1;
    }
  }

  public short getDeedType() {
    if (isTenant()) {
      String type = getHubProfileAttribute(DEED_TYPE);
      return StringUtils.isBlank(type) ? -1 : Short.parseShort(type);
    } else {
      return -1;
    }
  }

  public String getDeedManager() {
    if (isTenant()) {
      return getHubProfileAttribute(DEED_MANAGER_ADDRESS);
    } else {
      return null;
    }
  }

  public boolean isTenantManager(String address) {
    return StringUtils.isNotBlank(address)
        && StringUtils.equalsIgnoreCase(getDeedManager(), address);
  }

  public boolean isTenantManager(String address, long nftId) throws WomConnectionException {
    if (StringUtils.isNotBlank(address)) {
      return tenantServiceConsumer.isDeedManager(address, nftId);
    } else {
      return false;
    }
  }

  public Hub getHub(long nftId) throws WomConnectionException {
    return tenantServiceConsumer.getHub(nftId);
  }

  public Hub getHub() {
    Profile hubProfile = getHubProfile();
    if (!isTenant() || hubProfile == null) {
      return null;
    }
    String deedId = (String) hubProfile.getProperty(DEED_ID);
    String city = (String) hubProfile.getProperty(DEED_CITY);
    String type = (String) hubProfile.getProperty(DEED_TYPE);
    String address = (String) hubProfile.getProperty(ADDRESS);
    String name = (String) hubProfile.getProperty(NAME);
    String description = (String) hubProfile.getProperty(DESCRIPTION);
    String url = (String) hubProfile.getProperty(URL);
    String logoUrl = (String) hubProfile.getProperty(LOGO_URL);
    String color = (String) hubProfile.getProperty(COLOR);
    String earnerAddress = (String) hubProfile.getProperty(EARNER_ADDRESS);
    String deedManagerAddress = (String) hubProfile.getProperty(DEED_MANAGER_ADDRESS);

    HubReward hub = new HubReward(Long.parseLong(deedId),
                                  Short.parseShort(city),
                                  Short.parseShort(type),
                                  address,
                                  name,
                                  description,
                                  url,
                                  logoUrl,
                                  color,
                                  deedManagerAddress,
                                  earnerAddress);
    computeRewardingInfo(hub);
    computeUsersCount(hub);
    return hub;
  }

  public HubConfiguration getDeedTenantConfiguration() throws WomConnectionException {
    HubConfiguration deedTenantConfiguration = new HubConfiguration();
    deedTenantConfiguration.setToken(tenantServiceConsumer.generateToken());
    deedTenantConfiguration.setAdminWallet(walletAccountService.getAdminWallet().getAddress());
    return deedTenantConfiguration;
  }

  public String connectToWoM(WomConnectionRequest connectionRequest) throws WomConnectionException {
    try {
      String hubAddress = createHubAddress(null);
      connectionRequest.setHubAddress(hubAddress);
      tenantServiceConsumer.connectToWoM(connectionRequest);
      return hubAddress;
    } finally {
      refreshHubIdentity();
    }
  }

  public void disconnectFromWoM(WomDisconnectionRequest disconnectionRequest) throws WomConnectionException {
    if (!isTenant()) {
      throw new WomConnectionException("wom.alreadyDisconnected");
    }
    try {
      disconnectionRequest.setHubAddress(getHubAddress());
      tenantServiceConsumer.disconnectFromWoM(disconnectionRequest);
    } finally {
      refreshHubIdentity();
    }
  }

  private String createHubAddress(String hubPrivateKey) {
    String hubAddress = getHubAddress();
    if (hubAddress != null) {
      return hubAddress;
    }

    ECKeyPair ecKeyPair = generateWalletKeys(hubPrivateKey);
    try {
      Identity identity = identityManager.getOrCreateIdentity(IDENTITY_PROVIDER_NAME, IDENTITY_REMOTE_ID);
      return saveHubWallet(identity, ecKeyPair);
    } catch (CipherException e) {
      throw new IllegalStateException("Error creating new Hub wallet", e);
    }
  }

  private String getHubAddress() {
    return getHubProfileAttribute(ADDRESS);
  }

  private Identity getHubIdentity() {
    if (this.hubIdentity == null) {
      this.hubIdentity = identityManager.getOrCreateIdentity(IDENTITY_PROVIDER_NAME, IDENTITY_REMOTE_ID);
      if (this.hubIdentity != null) {
        populateProfile(this.hubIdentity.getProfile());
      }
    }
    return this.hubIdentity;
  }

  private Profile getHubProfile() {
    Identity identity = getHubIdentity();
    return identity == null ? null : identity.getProfile();
  }

  private String saveHubWallet(Identity hubIdentity, ECKeyPair ecKeyPair) throws CipherException {
    if (hubIdentity == null) {
      return null;
    }
    WalletFile hubWalletFile = org.web3j.crypto.Wallet.createStandard(walletAccountService.getAdminAccountPassword(), ecKeyPair);

    Profile hubProfile = hubIdentity.getProfile();
    hubProfile.setProperty(WALLET, toJsonString(hubWalletFile));
    hubProfile.setProperty(ADDRESS, hubWalletFile.getAddress());
    identityManager.updateProfile(hubProfile);

    return hubWalletFile.getAddress();
  }

  private void refreshHubIdentity() {
    // Force Retrieve Hub profile again
    hubIdentity = null;
  }

  private String getHubProfileAttribute(String attributeName) {
    Profile hubProfile = getHubProfile();
    return hubProfile == null ? null : (String) hubProfile.getProperty(attributeName);
  }

  private void populateProfile(Profile hubProfile) {
    try {
      String hubAddress = (String) hubProfile.getProperty(ADDRESS);
      Hub hub = tenantServiceConsumer.getHub(hubAddress);
      if (hub != null) {
        populateProfile(hubProfile, hub);
      } else {
        clearHubProperties(hubProfile);
      }
    } catch (WomConnectionException e) {
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
      identityManager.updateProfile(hubProfile);
    }
  }

  private ECKeyPair generateWalletKeys(String hubPrivateKey) {
    ECKeyPair ecKeyPair = null;
    if (StringUtils.isBlank(hubPrivateKey)) {
      try {
        ecKeyPair = Keys.createEcKeyPair();
      } catch (Exception e) {
        throw new IllegalStateException("Error creating new wallet keys pair", e);
      }
    } else {
      if (!WalletUtils.isValidPrivateKey(hubPrivateKey)) {
        throw new IllegalStateException("Private key isn't valid");
      }
      ecKeyPair = Credentials.create(hubPrivateKey).getEcKeyPair();
    }
    return ecKeyPair;
  }

  private List<String> getParamValues(InitParams params, String paramName) {
    if (params != null && params.containsKey(paramName)) {
      return params.getValuesParam(paramName).getValues();
    }
    return Collections.emptyList();
  }

  private void computeRewardingInfo(HubReward hubStatus) {
    try {
      RewardSettings settings = rewardSettingsService.getSettings();
      hubStatus.setRewardsPeriod(settings.getPeriodType());
      List<RewardPeriod> rewardPeriodsInProgress = rewardReportService.getRewardPeriodsInProgress();
      if (CollectionUtils.isNotEmpty(rewardPeriodsInProgress)) {
        RewardPeriod rewardPeriod = rewardPeriodsInProgress.get(rewardPeriodsInProgress.size() - 1);
        RewardReport rewardReport = rewardReportService.getRewardReport(rewardPeriod.getPeriodMedianDate());
        if (rewardReport != null) {
          hubStatus.setRewardsAmount(rewardReport.getTokensToSend());
        }
      } else {
        RewardReport rewardReport = rewardReportService.computeRewards(LocalDate.now(ZoneId.of(settings.getTimeZone())));
        if (rewardReport != null) {
          hubStatus.setRewardsAmount(rewardReport.getTokensToSend());
        }
      }
    } catch (Exception e) {
      LOG.warn("Error computing Hub rewarding information, retrieve already computed data", e);
    }
  }

  private void computeUsersCount(HubReward hubStatus) {
    try {
      hubStatus.setUsersCount(organizationService.getUserHandler().findAllUsers(UserStatus.ENABLED).getSize());
    } catch (Exception e) {
      LOG.warn("Error computing Hub users count information, retrieve already computed data", e);
    }
  }

}
