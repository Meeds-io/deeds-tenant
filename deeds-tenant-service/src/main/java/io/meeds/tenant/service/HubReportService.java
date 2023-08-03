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
import static org.exoplatform.wallet.utils.WalletUtils.getContractAddress;
import static org.exoplatform.wallet.utils.WalletUtils.getNetworkId;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Hash;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wallet.model.reward.RewardPeriod;
import org.exoplatform.wallet.model.reward.RewardReport;
import org.exoplatform.wallet.model.reward.WalletReward;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.reward.service.RewardReportService;

import io.meeds.deeds.constant.HubRewardReportStatusType;
import io.meeds.deeds.constant.WomException;
import io.meeds.deeds.constant.WomParsingException;
import io.meeds.deeds.model.HubRewardReport;
import io.meeds.deeds.model.HubRewardReportRequest;
import io.meeds.deeds.model.HubRewardReportStatus;
import io.meeds.gamification.constant.IdentityType;
import io.meeds.gamification.constant.RealizationStatus;
import io.meeds.gamification.model.filter.RealizationFilter;
import io.meeds.gamification.service.RealizationService;
import io.meeds.tenant.rest.client.WoMServiceClient;

@Service
public class HubReportService {

  private static final Log     LOG                         = ExoLogger.getLogger(HubReportService.class);

  public static final int      MAX_START_TENTATIVES        = 5;

  public static final String   MANAGER_DEFAULT_ROLES_PARAM = "managerDefaultRoles";

  // Used for testnet only, WoM Server won't accept outdated reports
  private static final boolean SEND_OUTDATED_REPORT        =
                                                    Boolean.parseBoolean(System.getProperty("io.meeds.test.sendOutdatedReport",
                                                                                            "false"));

  @Autowired
  private SettingService       settingService;

  @Autowired
  private RewardReportService  rewardReportService;

  @Autowired
  private RealizationService   realizationService;

  @Autowired
  private HubService           hubService;

  @Autowired
  private WoMServiceClient     womServiceClient;

  public HubReportService(HubService hubService,
                          SettingService settingService,
                          RealizationService realizationService,
                          RewardReportService rewardReportService,
                          WoMServiceClient womServiceClient) {
    this.settingService = settingService;
    this.womServiceClient = womServiceClient;
    this.realizationService = realizationService;
    this.rewardReportService = rewardReportService;
    this.hubService = hubService;
  }

  public void sendReportToWoM(RewardReport rewardReport) throws WomException {
    if (!hubService.isDeedHub()) {
      return;
    }

    RewardPeriod rewardPeriod = rewardReport.getPeriod();
    if (!rewardReport.isCompletelyProceeded()) {
      throw new IllegalStateException("Reward of period '" + rewardPeriod
          + "' isn't completed proceeded, thus the Rewards report will not be sent");
    } else if (isValidRewardDate(rewardPeriod)) {
      HubRewardReport hubRewardReport = mapToHubRewardReport(rewardReport);

      HubRewardReportRequest hubRewardReportRequest = new HubRewardReportRequest();
      hubRewardReportRequest.setRewardReport(hubRewardReport);
      hubRewardReportRequest.setSignature(hubService.signHubMessage(hubRewardReportRequest));
      String rewardPeriodHash = Hash.sha3(hubRewardReportRequest.getSignature());
      hubRewardReportRequest.setHash(rewardPeriodHash);

      try {
        womServiceClient.sendReportToWoM(hubRewardReportRequest);
        markReportAsSent(rewardPeriod, rewardPeriodHash);
      } catch (WomException e) {
        markReportAsError(rewardPeriod, rewardPeriodHash, e);
        throw e;
      } catch (RuntimeException e) {
        markReportAsError(rewardPeriod, rewardPeriodHash, new WomException("wom.unknownError", true));
        throw e;
      }
    } else {
      LOG.info("Hub rewards will not be sent to WoM server since it's end date is before WoM join date");
    }
  }

  public List<HubRewardReportStatus> getHubRewardReports(int offset, int limit) {
    List<RewardPeriod> rewardPeriods = rewardReportService.findRewardReportPeriods(offset, limit);
    if (CollectionUtils.isEmpty(rewardPeriods)) {
      return Collections.emptyList();
    } else {
      return rewardPeriods.stream()
                          .map(p -> rewardReportService.getRewardReport(p.getPeriodMedianDate()))
                          .filter(Objects::nonNull)
                          .map(this::mapToHubRewardReportStatus)
                          .toList();
    }
  }

  public HubRewardReport mapToHubRewardReport(RewardReport rewardReport) {
    RewardPeriod rewardPeriod = rewardReport.getPeriod();
    long startDateMillis = rewardPeriod.getStartDateInSeconds();
    long endDateMillis = rewardPeriod.getEndDateInSeconds();
    Instant startDateInstant = Instant.ofEpochMilli(startDateMillis);
    Instant endDateInstant = Instant.ofEpochMilli(endDateMillis);
    Date fromDate = Date.from(startDateInstant);
    Date toDate = Date.from(endDateInstant);

    HubRewardReport hubRewardReport = new HubRewardReport();
    hubRewardReport.setHubAddress(hubService.getHubAddress());
    hubRewardReport.setDeedId(hubService.getDeedId());
    hubRewardReport.setRewardTokenAddress(getContractAddress());
    hubRewardReport.setRewardTokenNetworkId(getNetworkId());

    hubRewardReport.setFromDate(startDateInstant);
    hubRewardReport.setToDate(endDateInstant);
    hubRewardReport.setPeriodType(rewardPeriod.getRewardPeriodType().name());

    hubRewardReport.setParticipantsCount(countParticipants(fromDate, toDate));
    hubRewardReport.setAchievementsCount(countAchievements(fromDate, toDate));

    hubRewardReport.setRecipientsCount(rewardReport.getValidRewardCount());
    hubRewardReport.setRewardAmount(rewardReport.getTokensSent());
    hubRewardReport.setTransactions(rewardReport.getValidRewards()
                                                .stream()
                                                .filter(Objects::nonNull)
                                                .map(WalletReward::getTransaction)
                                                .filter(Objects::nonNull)
                                                .filter(TransactionDetail::isSucceeded)
                                                .map(TransactionDetail::getHash)
                                                .filter(Objects::nonNull)
                                                .collect(Collectors.toSet()));
    hubRewardReport.setSentRewardsDate(Instant.now());
    return hubRewardReport;
  }

  private boolean isValidRewardDate(RewardPeriod rewardPeriod) {
    Instant endDate = Instant.ofEpochMilli(rewardPeriod.getEndDateInSeconds());
    return SEND_OUTDATED_REPORT || hubService.getHubJoinDate().isBefore(endDate);
  }

  private void markReportAsSent(RewardPeriod rewardPeriod, String rewardPeriodHash) {
    saveRewardPeriodHash(rewardPeriod, rewardPeriodHash);
    saveRewardPeriodStatus(rewardPeriod, HubRewardReportStatusType.SENT.name());
  }

  private void markReportAsError(RewardPeriod rewardPeriod, String rewardPeriodHash, WomException e) throws WomParsingException {
    saveRewardPeriodHash(rewardPeriod, rewardPeriodHash);
    saveRewardPeriodStatus(rewardPeriod, toJsonString(e));
  }

  private String getPeriodKey(RewardPeriod rewardPeriod) {
    return rewardPeriod.getStartDateInSeconds() + "-" + rewardPeriod.getEndDateInSeconds();
  }

  private void saveRewardPeriodStatus(RewardPeriod rewardPeriod, String status) {
    String rewardPeriodKey = getPeriodKey(rewardPeriod);
    settingService.set(Context.GLOBAL.id("WoM"),
                       Scope.APPLICATION.id("RewardReportStatus"),
                       rewardPeriodKey,
                       SettingValue.create(status));
  }

  private void saveRewardPeriodHash(RewardPeriod rewardPeriod, String rewardPeriodHash) {
    String rewardPeriodKey = getPeriodKey(rewardPeriod);
    settingService.set(Context.GLOBAL.id("WoM"),
                       Scope.APPLICATION.id("RewardReportHash"),
                       rewardPeriodKey,
                       SettingValue.create(rewardPeriodHash));
  }

  private HubRewardReportStatusType mapToHubRewardReportStatusType(RewardPeriod rewardPeriod, String status) {
    if (StringUtils.isBlank(status)) {
      if (isValidRewardDate(rewardPeriod)) {
        return HubRewardReportStatusType.NONE;
      } else {
        return HubRewardReportStatusType.INVALID;
      }
    } else {
      return switch (status) {
      case "SENT":
        yield HubRewardReportStatusType.SENT;
      case "PENDING_REWARD":
        yield HubRewardReportStatusType.PENDING_REWARD;
      case "REWARDED":
        yield HubRewardReportStatusType.REWARDED;
      case "REJECTED":
        yield HubRewardReportStatusType.REJECTED;
      default:
        yield HubRewardReportStatusType.ERROR_SENDING;
      };
    }
  }

  private String getRewardPeriodStatus(RewardPeriod rewardPeriod) {
    String rewardPeriodKey = getPeriodKey(rewardPeriod);
    SettingValue<?> statusValue = settingService.get(Context.GLOBAL.id("WoM"),
                                                     Scope.APPLICATION.id("RewardReportStatus"),
                                                     rewardPeriodKey);
    return statusValue == null || statusValue.getValue() == null ? null : statusValue.getValue().toString();
  }

  private String getRewardPeriodHash(RewardPeriod rewardPeriod) {
    String rewardPeriodKey = getPeriodKey(rewardPeriod);
    SettingValue<?> settingValue = settingService.get(Context.GLOBAL.id("WoM"),
                                                      Scope.APPLICATION.id("RewardReportHash"),
                                                      rewardPeriodKey);
    return settingValue == null || settingValue.getValue() == null ? null : settingValue.getValue().toString();
  }

  private long countParticipants(Date fromDate, Date toDate) {
    return realizationService.countParticipantsBetweenDates(fromDate, toDate);
  }

  private int countAchievements(Date fromDate, Date toDate) {
    RealizationFilter realizationFilter = new RealizationFilter();
    realizationFilter.setFromDate(fromDate);
    realizationFilter.setToDate(toDate);
    realizationFilter.setEarnerType(IdentityType.USER);
    realizationFilter.setStatus(RealizationStatus.ACCEPTED);
    return realizationService.countRealizationsByFilter(realizationFilter);
  }

  private HubRewardReportStatus mapToHubRewardReportStatus(RewardReport rewardReport) {
    RewardPeriod rewardPeriod = rewardReport.getPeriod();
    String status = getRewardPeriodStatus(rewardPeriod);

    HubRewardReportStatusType statusType = mapToHubRewardReportStatusType(rewardPeriod, status);
    String errorMessageKey = statusType == HubRewardReportStatusType.ERROR_SENDING ? status : null;

    HubRewardReport hubRewardReport = mapToHubRewardReport(rewardReport);
    String hash = getRewardPeriodHash(rewardPeriod);
    HubRewardReportStatus hubRewardReportStatus = new HubRewardReportStatus(hash,
                                                                            hubRewardReport,
                                                                            statusType,
                                                                            errorMessageKey);
    if (StringUtils.isBlank(hash)) {
      hubRewardReport.setDeedId(-1);
    }
    return hubRewardReportStatus;
  }

}
