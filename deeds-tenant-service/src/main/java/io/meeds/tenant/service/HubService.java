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

import static io.meeds.deeds.utils.JsonUtils.toJsonString;
import static io.meeds.tenant.plugin.WalletHubIdentityProvider.ADDRESS;
import static io.meeds.tenant.plugin.WalletHubIdentityProvider.COLOR;
import static io.meeds.tenant.plugin.WalletHubIdentityProvider.DEED_CITY;
import static io.meeds.tenant.plugin.WalletHubIdentityProvider.DEED_ID;
import static io.meeds.tenant.plugin.WalletHubIdentityProvider.DEED_MANAGER_ADDRESS;
import static io.meeds.tenant.plugin.WalletHubIdentityProvider.DEED_TYPE;
import static io.meeds.tenant.plugin.WalletHubIdentityProvider.DESCRIPTION;
import static io.meeds.tenant.plugin.WalletHubIdentityProvider.EARNER_ADDRESS;
import static io.meeds.tenant.plugin.WalletHubIdentityProvider.JOIN_DATE;
import static io.meeds.tenant.plugin.WalletHubIdentityProvider.LOGO_URL;
import static io.meeds.tenant.plugin.WalletHubIdentityProvider.NAME;
import static io.meeds.tenant.plugin.WalletHubIdentityProvider.URL;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.UserStatus;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.wallet.model.reward.RewardPeriod;
import org.exoplatform.wallet.model.reward.RewardReport;
import org.exoplatform.wallet.model.reward.RewardSettings;
import org.exoplatform.wallet.reward.service.RewardReportService;
import org.exoplatform.wallet.reward.service.RewardSettingsService;
import org.exoplatform.wallet.service.WalletAccountService;

import io.meeds.deeds.constant.WomException;
import io.meeds.deeds.model.Hub;
import io.meeds.deeds.model.WomConnectionRequest;
import io.meeds.deeds.model.WomDisconnectionRequest;
import io.meeds.tenant.model.HubConfiguration;
import io.meeds.tenant.model.HubReward;
import io.meeds.tenant.rest.client.WoMServiceClient;
import io.meeds.tenant.storage.HubIdentityStorage;
import io.meeds.tenant.storage.HubWalletStorage;

public class HubService {

  private static final Log      LOG                         = ExoLogger.getLogger(HubService.class);

  public static final int       MAX_START_TENTATIVES        = 5;

  public static final String    MANAGER_DEFAULT_ROLES_PARAM = "managerDefaultRoles";

  private OrganizationService   organizationService;

  private RewardSettingsService rewardSettingsService;

  private RewardReportService   rewardReportService;

  private WalletAccountService  walletAccountService;

  private HubIdentityStorage    hubIdentityStorage;

  private HubWalletStorage      hubWalletStorage;

  private WoMServiceClient      womServiceClient;

  public HubService(WalletAccountService walletAccountService, // NOSONAR
                    RewardSettingsService rewardSettingsService,
                    RewardReportService rewardReportService,
                    OrganizationService organizationService,
                    WoMServiceClient womServiceClient,
                    HubIdentityStorage hubIdentityStorage,
                    HubWalletStorage hubWalletStorage) {
    this.womServiceClient = womServiceClient;
    this.walletAccountService = walletAccountService;
    this.rewardSettingsService = rewardSettingsService;
    this.organizationService = organizationService;
    this.rewardReportService = rewardReportService;
    this.hubIdentityStorage = hubIdentityStorage;
    this.hubWalletStorage = hubWalletStorage;
  }

  public long getDeedId() {
    String deedId = hubIdentityStorage.getHubProperty(DEED_ID);
    return StringUtils.isBlank(deedId) ? -1 : Long.parseLong(deedId);
  }

  public boolean isDeedHub() {
    return getDeedId() > -1;
  }

  public short getDeedCity() {
    if (isDeedHub()) {
      String city = hubIdentityStorage.getHubProperty(DEED_CITY);
      return StringUtils.isBlank(city) ? -1 : Short.parseShort(city);
    } else {
      return -1;
    }
  }

  public short getDeedType() {
    if (isDeedHub()) {
      String type = hubIdentityStorage.getHubProperty(DEED_TYPE);
      return StringUtils.isBlank(type) ? -1 : Short.parseShort(type);
    } else {
      return -1;
    }
  }

  public String getDeedManager() {
    if (isDeedHub()) {
      return hubIdentityStorage.getHubProperty(DEED_MANAGER_ADDRESS);
    } else {
      return null;
    }
  }

  public boolean isDeedManager(String address, long nftId) throws WomException {
    if (StringUtils.isNotBlank(address)) {
      return womServiceClient.isDeedManager(address, nftId);
    } else {
      return false;
    }
  }

  public String getHubAddress() {
    return hubIdentityStorage.getHubProperty(ADDRESS);
  }

  public Instant getHubJoinDate() {
    String joinDate = hubIdentityStorage.getHubProperty(JOIN_DATE);
    return StringUtils.isBlank(joinDate) ? null : Instant.ofEpochMilli(Long.parseLong(joinDate));
  }

  public Hub getHub(long nftId) throws WomException {
    return womServiceClient.getHub(nftId);
  }

  public Hub getHub() {
    Profile hubProfile = hubIdentityStorage.getHubProfile();
    if (!isDeedHub() || hubProfile == null) {
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
    String joinDateMillis = (String) hubProfile.getProperty(JOIN_DATE);
    Instant joinDate = Instant.ofEpochMilli(Long.parseLong(joinDateMillis));

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
                                  earnerAddress,
                                  joinDate);
    computeRewardingInfo(hub);
    computeUsersCount(hub);
    return hub;
  }

  public HubConfiguration getConfiguration() throws WomException {
    HubConfiguration deedTenantConfiguration = new HubConfiguration();
    deedTenantConfiguration.setToken(womServiceClient.generateToken());
    deedTenantConfiguration.setAdminWallet(walletAccountService.getAdminWallet().getAddress());
    return deedTenantConfiguration;
  }

  public String connectToWoM(WomConnectionRequest connectionRequest) throws WomException {
    try {
      String hubAddress = hubWalletStorage.getOrCreateHubAddress(null);
      connectionRequest.setHubAddress(hubAddress);
      womServiceClient.connectToWoM(connectionRequest);
      return hubAddress;
    } finally {
      hubIdentityStorage.refreshHubIdentity();
    }
  }

  public void disconnectFromWoM(WomDisconnectionRequest disconnectionRequest) throws WomException {
    if (!isDeedHub()) {
      throw new WomException("wom.alreadyDisconnected");
    }
    try {
      disconnectionRequest.setHubAddress(getHubAddress());
      womServiceClient.disconnectFromWoM(disconnectionRequest);
    } finally {
      hubIdentityStorage.refreshHubIdentity();
    }
  }

  public String signHubMessage(Object object) throws WomException {
    String rawRequest = toJsonString(object);
    byte[] encodedRequest = rawRequest.getBytes(StandardCharsets.UTF_8);
    Sign.SignatureData signatureData = Sign.signPrefixedMessage(encodedRequest, hubWalletStorage.getHubWallet());
    byte[] retval = new byte[65];
    System.arraycopy(signatureData.getR(), 0, retval, 0, 32);
    System.arraycopy(signatureData.getS(), 0, retval, 32, 32);
    System.arraycopy(signatureData.getV(), 0, retval, 64, 1);
    return Numeric.toHexString(retval);
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
