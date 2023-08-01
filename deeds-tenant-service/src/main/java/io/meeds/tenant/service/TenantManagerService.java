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

import io.meeds.tenant.constant.WomConnectionException;
import io.meeds.tenant.model.DeedTenantConfiguration;
import io.meeds.tenant.model.DeedTenantNft;
import io.meeds.tenant.model.HubStatus;
import io.meeds.tenant.model.WomConnectionRequest;
import io.meeds.tenant.plugin.WalletHubIdentityProvider;
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

  private DeedTenantNft         currentDeedTenantHost;

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

  public boolean isTenantManager(String address) {
    if (isTenant() && StringUtils.isNotBlank(address)) {
      if (currentDeedTenantHost == null) {
        currentDeedTenantHost = tenantServiceConsumer.getDeedTenant(getNftId());
        if (currentDeedTenantHost == null) {
          return false;
        }
      }
      if (StringUtils.isBlank(currentDeedTenantHost.getManagerAddress())) {
        boolean isTenantManager = tenantServiceConsumer.isDeedManager(address, getNftId());
        if (isTenantManager) {
          currentDeedTenantHost.setManagerAddress(address);
        }
        return isTenantManager;
      } else {
        return StringUtils.equalsIgnoreCase(address, currentDeedTenantHost.getManagerAddress());
      }
    } else {
      return false;
    }
  }

  public boolean isTenantManager(String address, long nftId) {
    if (StringUtils.isNotBlank(address)) {
      return tenantServiceConsumer.isDeedManager(address, nftId);
    } else {
      return false;
    }
  }

  public DeedTenantNft getDeedTenant(long nftId) {
    return tenantServiceConsumer.getDeedTenant(nftId);
  }

  public DeedTenantNft getDeedTenantHub() {
    if (currentDeedTenantHost != null) {
      return currentDeedTenantHost;
    } else if (isTenant()) {
      currentDeedTenantHost = tenantServiceConsumer.getDeedTenant(getNftId());
      return currentDeedTenantHost;
    } else {
      return null;
    }
  }

  public boolean isTenant() {
    return getNftId() > -1;
  }

  public long getNftId() {
    Identity hubIdentity = getHubIdentity();
    if (hubIdentity == null || hubIdentity.getProfile() == null) {
      return -1;
    }
    Profile hubProfile = hubIdentity.getProfile();
    String deedId = (String) hubProfile.getProperty(WalletHubIdentityProvider.DEED_ID);
    String address = (String) hubProfile.getProperty(WalletHubIdentityProvider.ADDRESS);
    return StringUtils.isBlank(deedId) || StringUtils.isBlank(address) ? -1 : Long.parseLong(deedId);
  }

  public DeedTenantConfiguration getDeedTenantConfiguration() {
    DeedTenantConfiguration deedTenantConfiguration = new DeedTenantConfiguration();
    deedTenantConfiguration.setToken(tenantServiceConsumer.generateToken());
    deedTenantConfiguration.setAdminWallet(walletAccountService.getAdminWallet().getAddress());
    return deedTenantConfiguration;
  }

  public HubStatus getHubStatus() {
    Identity hubIdentity = getHubIdentity();
    if (hubIdentity == null || hubIdentity.getProfile() == null) {
      return null;
    }
    Profile hubProfile = hubIdentity.getProfile();
    String deedId = (String) hubProfile.getProperty(WalletHubIdentityProvider.DEED_ID);
    if (StringUtils.isBlank(deedId)) {
      return null;
    }
    String city = (String) hubProfile.getProperty(WalletHubIdentityProvider.CITY);
    String type = (String) hubProfile.getProperty(WalletHubIdentityProvider.TYPE);
    String address = (String) hubProfile.getProperty(WalletHubIdentityProvider.ADDRESS);
    String name = (String) hubProfile.getProperty(WalletHubIdentityProvider.NAME);
    String description = (String) hubProfile.getProperty(WalletHubIdentityProvider.DESCRIPTION);
    String url = (String) hubProfile.getProperty(WalletHubIdentityProvider.URL);
    String logoUrl = (String) hubProfile.getProperty(WalletHubIdentityProvider.LOGO_URL);
    String color = (String) hubProfile.getProperty(WalletHubIdentityProvider.COLOR);
    String earnerAddress = (String) hubProfile.getProperty(WalletHubIdentityProvider.EARNER_ADDRESS);
    String deedManagerAddress = (String) hubProfile.getProperty(WalletHubIdentityProvider.MANAGER_ADDRESS);

    HubStatus hubStatus = new HubStatus(Long.parseLong(deedId),
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
    computeRewardingInfo(hubStatus);
    computeUsersCount(hubStatus);
    return hubStatus;
  }

  public String connectToWoM(WomConnectionRequest connectionRequest) throws WomConnectionException {
    String hubAddress = createHubAddress(null);
    connectionRequest.setHubAddress(hubAddress);
    tenantServiceConsumer.connectToWoM(connectionRequest);
    populateHubProfile(connectionRequest);
    currentDeedTenantHost = null;
    return hubAddress;
  }

  private String createHubAddress(String hubPrivateKey) {
    String hubAddress = getHubAddress();
    if (hubAddress != null) {
      return hubAddress;
    }

    ECKeyPair ecKeyPair = generateWalletKeys(hubPrivateKey);
    try {
      Identity hubIdentity = getHubIdentity();
      return saveHubWallet(hubIdentity, ecKeyPair);
    } catch (CipherException e) {
      throw new IllegalStateException("Error creating new Hub wallet", e);
    }
  }

  private String getHubAddress() {
    Identity hubIdentity = getHubIdentity();
    if (hubIdentity == null) {
      throw new IllegalStateException("Can't find identity of hub");
    }
    return (String) hubIdentity.getProfile().getProperty(WalletHubIdentityProvider.ADDRESS);
  }

  private Identity getHubIdentity() {
    return identityManager.getOrCreateIdentity(WalletHubIdentityProvider.PROVIDER_NAME, WalletHubIdentityProvider.ID);
  }

  private String saveHubWallet(Identity hubIdentity, ECKeyPair ecKeyPair) throws CipherException {
    WalletFile hubWalletFile = org.web3j.crypto.Wallet.createStandard(walletAccountService.getAdminAccountPassword(), ecKeyPair);

    Profile hubProfile = hubIdentity.getProfile();
    hubProfile.setProperty(WalletHubIdentityProvider.WALLET, toJsonString(hubWalletFile));
    hubProfile.setProperty(WalletHubIdentityProvider.ADDRESS, hubWalletFile.getAddress());
    identityManager.updateProfile(hubProfile);

    return hubWalletFile.getAddress();
  }

  private void populateHubProfile(WomConnectionRequest connectionRequest) {
    Identity hubIdentity = getHubIdentity();
    Profile hubProfile = hubIdentity.getProfile();
    hubProfile.setProperty(WalletHubIdentityProvider.NAME, connectionRequest.getHubName());
    hubProfile.setProperty(WalletHubIdentityProvider.DESCRIPTION, connectionRequest.getHubDescription());
    hubProfile.setProperty(WalletHubIdentityProvider.URL, connectionRequest.getHubUrl());
    hubProfile.setProperty(WalletHubIdentityProvider.LOGO_URL, connectionRequest.getHubLogoUrl());
    hubProfile.setProperty(WalletHubIdentityProvider.COLOR, connectionRequest.getColor());
    hubProfile.setProperty(WalletHubIdentityProvider.EARNER_ADDRESS, connectionRequest.getEarnerAddress());
    hubProfile.setProperty(WalletHubIdentityProvider.MANAGER_ADDRESS, connectionRequest.getDeedManagerAddress());

    DeedTenantNft deedTenantNft = getDeedTenant(connectionRequest.getDeedId());
    hubProfile.setProperty(WalletHubIdentityProvider.DEED_ID, String.valueOf(deedTenantNft.getNftId()));
    hubProfile.setProperty(WalletHubIdentityProvider.CITY, String.valueOf(deedTenantNft.getCity()));
    hubProfile.setProperty(WalletHubIdentityProvider.TYPE, String.valueOf(deedTenantNft.getType()));
    identityManager.updateProfile(hubProfile);
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

  private void computeRewardingInfo(HubStatus hubStatus) {
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

  private void computeUsersCount(HubStatus hubStatus) {
    try {
      hubStatus.setUsersCount(organizationService.getUserHandler().findAllUsers(UserStatus.ENABLED).getSize());
    } catch (Exception e) {
      LOG.warn("Error computing Hub users count information, retrieve already computed data", e);
    }
  }

}
