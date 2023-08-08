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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;

import org.apache.commons.lang3.StringUtils;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.UserStatus;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;
import org.exoplatform.wallet.model.reward.RewardPeriodType;
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

  private UploadService         uploadService;

  private HubIdentityStorage    hubIdentityStorage;

  private HubWalletStorage      hubWalletStorage;

  private WoMServiceClient      womServiceClient;

  public HubService(WalletAccountService walletAccountService, // NOSONAR
                    RewardSettingsService rewardSettingsService,
                    RewardReportService rewardReportService,
                    OrganizationService organizationService,
                    UploadService uploadService,
                    WoMServiceClient womServiceClient,
                    HubIdentityStorage hubIdentityStorage,
                    HubWalletStorage hubWalletStorage) {
    this.womServiceClient = womServiceClient;
    this.walletAccountService = walletAccountService;
    this.rewardSettingsService = rewardSettingsService;
    this.organizationService = organizationService;
    this.uploadService = uploadService;
    this.rewardReportService = rewardReportService;
    this.hubIdentityStorage = hubIdentityStorage;
    this.hubWalletStorage = hubWalletStorage;
  }

  public Hub getHub() {
    return hubIdentityStorage.getHub();
  }

  public boolean isDeedHub() {
    return getHub() != null;
  }

  public long getDeedId() {
    Hub hub = getHub();
    return hub == null ? -1 : hub.getDeedId();
  }

  public short getDeedCity() {
    return isDeedHub() ? getHub().getCity() : null;
  }

  public short getDeedType() {
    return isDeedHub() ? getHub().getType() : null;
  }

  public String getDeedManager() {
    return isDeedHub() ? getHub().getDeedManagerAddress() : null;
  }

  public String getHubAddress() {
    return hubIdentityStorage.getHubAddress();
  }

  public Instant getHubJoinDate() {
    return isDeedHub() ? getHub().getCreatedDate() : null;
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

  public String generateWoMToken() throws WomException {
    return womServiceClient.generateToken();
  }

  public HubConfiguration getConfiguration() {
    HubConfiguration deedTenantConfiguration = new HubConfiguration();
    deedTenantConfiguration.setAdminWallet(walletAccountService.getAdminWallet().getAddress());
    deedTenantConfiguration.setHubAddress(getHubAddress());
    deedTenantConfiguration.setUsersCount(computeUsersCount());
    deedTenantConfiguration.setRewardsPeriodType(getRewardsPeriodType().name());
    deedTenantConfiguration.setRewardsPerPeriod(getRewardsForPeriod(LocalDate.now()));
    deedTenantConfiguration.setWomServerUrl(womServiceClient.getWomUrl());
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

  public void saveHubAvatar(String uploadId,
                            String signedMessage,
                            String rawMessage,
                            String token) throws ObjectNotFoundException, WomException, IOException {
    UploadResource uploadResource = uploadService.getUploadResource(uploadId);
    if (uploadResource == null) {
      throw new ObjectNotFoundException("wom.uploadedFileNotFound");
    }
    String hubAddress = getHubAddress();
    if (StringUtils.isBlank(hubAddress)) {
      throw new WomException("wom.notConnected");
    }
    womServiceClient.saveHubAvatar(hubAddress,
                                   signedMessage,
                                   rawMessage,
                                   token,
                                   uploadResource);
  }

  public void saveHubBanner(String uploadId,
                            String signedMessage,
                            String rawMessage,
                            String token) throws ObjectNotFoundException, WomException, IOException {
    UploadResource uploadResource = uploadService.getUploadResource(uploadId);
    if (uploadResource == null) {
      throw new ObjectNotFoundException("wom.uploadedFileNotFound");
    }
    String hubAddress = getHubAddress();
    if (StringUtils.isBlank(hubAddress)) {
      throw new WomException("wom.notConnected");
    }
    womServiceClient.saveHubBanner(hubAddress,
                                   signedMessage,
                                   rawMessage,
                                   token,
                                   uploadResource);
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

  private RewardPeriodType getRewardsPeriodType() {
    RewardSettings settings = rewardSettingsService.getSettings();
    return settings.getPeriodType();
  }

  private double getRewardsForPeriod(LocalDate date) {
    RewardReport rewardReport = rewardReportService.computeRewards(date);
    if (rewardReport != null) {
      return rewardReport.getTokensToSend();
    } else {
      return 0;
    }
  }

  private long computeUsersCount() {
    try {
      return organizationService.getUserHandler().findAllUsers(UserStatus.ENABLED).getSize();
    } catch (Exception e) {
      LOG.warn("Error computing Hub users count information, retrieve already computed data", e);
      return 0;
    }
  }

}
