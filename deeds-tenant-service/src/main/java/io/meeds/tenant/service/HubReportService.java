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
import static io.meeds.tenant.utils.EntityMapper.toHubReport;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.web3j.crypto.Hash;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wallet.model.reward.RewardPeriod;
import org.exoplatform.wallet.model.reward.RewardReport;
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
import io.meeds.tenant.model.HubRewardReportLocalStatus;
import io.meeds.tenant.rest.client.WoMServiceClient;
import io.meeds.tenant.storage.HubReportStorage;

public class HubReportService {

  private static final Log     LOG                         = ExoLogger.getLogger(HubReportService.class);

  public static final int      MAX_START_TENTATIVES        = 5;

  public static final String   MANAGER_DEFAULT_ROLES_PARAM = "managerDefaultRoles";

  // Used for testnet only, WoM Server won't accept outdated reports
  private static final boolean SEND_OUTDATED_REPORT        =
                                                    Boolean.parseBoolean(System.getProperty("io.meeds.test.sendOutdatedReport",
                                                                                            "false"));

  private RewardReportService  rewardReportService;

  private RealizationService   realizationService;

  private HubService           hubService;

  private WoMServiceClient     womServiceClient;

  private HubReportStorage     hubReportStorage;

  public HubReportService(HubService hubService,
                          RealizationService realizationService,
                          RewardReportService rewardReportService,
                          HubReportStorage hubReportStorage,
                          WoMServiceClient womServiceClient) {
    this.hubReportStorage = hubReportStorage;
    this.womServiceClient = womServiceClient;
    this.realizationService = realizationService;
    this.rewardReportService = rewardReportService;
    this.hubService = hubService;
  }

  public HubRewardReportLocalStatus sendReportToWoM(long periodId) throws WomException {
    RewardReport rewardReport = rewardReportService.getRewardReportByPeriodId(periodId);
    if (rewardReport == null) {
      return null;
    } else {
      return sendReportToWoM(rewardReport);
    }
  }

  public HubRewardReportLocalStatus sendReportToWoM(RewardReport rewardReport) throws WomException {
    if (!hubService.isDeedHub()) {
      return null;
    }

    RewardPeriod rewardPeriod = rewardReport.getPeriod();
    if (!rewardReport.isCompletelyProceeded()) {
      throw new IllegalStateException("Reward of period '" + rewardPeriod
          + "' isn't completed proceeded, thus the Rewards report will not be sent");
    } else if (isValidRewardDate(rewardPeriod)) {
      HubRewardReport hubRewardReport = toReport(rewardReport);

      HubRewardReportRequest hubRewardReportRequest = new HubRewardReportRequest();
      hubRewardReportRequest.setRewardReport(hubRewardReport);
      hubRewardReportRequest.setSignature(hubService.signHubMessage(hubRewardReport));
      String rewardPeriodHash = Hash.sha3(hubRewardReportRequest.getSignature());
      hubRewardReportRequest.setHash(rewardPeriodHash);

      try {
        HubRewardReportStatus hubRewardReportStatus = womServiceClient.sendReportToWoM(hubRewardReportRequest);
        markReportAsSent(rewardPeriod, rewardPeriodHash);
        return new HubRewardReportLocalStatus(hubRewardReportStatus,
                                              hubReportStorage.getPeriodKey(rewardPeriod),
                                              hubRewardReportStatus.getStatus().isCanRefresh(),
                                              hubRewardReportStatus.getStatus().isCanSend());
      } catch (WomException e) {
        markReportAsError(rewardPeriod, rewardPeriodHash, e);
        throw e;
      } catch (RuntimeException e) {
        markReportAsError(rewardPeriod, rewardPeriodHash, new WomException("wom.unknownError", true));
        throw e;
      }
    } else {
      LOG.info("Hub rewards will not be sent to WoM server since it's end date is before WoM join date");
      return null;
    }
  }

  public List<HubRewardReportLocalStatus> getHubRewardReports(int offset, int limit) {
    List<RewardPeriod> rewardPeriods = rewardReportService.findRewardReportPeriods(offset, limit);
    if (CollectionUtils.isEmpty(rewardPeriods)) {
      return Collections.emptyList();
    } else {
      return rewardPeriods.stream()
                          .map(p -> rewardReportService.getRewardReport(p.getPeriodMedianDate()))
                          .filter(Objects::nonNull)
                          .map(this::computeReportStatus)
                          .toList();
    }
  }

  public HubRewardReportLocalStatus getHubRewardReport(long periodId, boolean refresh) throws WomException {
    if (refresh) {
      String hash = hubReportStorage.getRewardPeriodHash(periodId);
      if (StringUtils.isBlank(hash)) {
        hubReportStorage.cleanRewardPeriodStatus(periodId);
        throw new WomException("wom.notSentReward");
      }
      HubRewardReportStatus rewardReportStatus = womServiceClient.getRewardReportStatus(hash);
      if (rewardReportStatus == null) {
        hubReportStorage.cleanRewardPeriodHash(periodId);
        hubReportStorage.cleanRewardPeriodStatus(periodId);
        throw new WomException("wom.rewardNotFoundInWom");
      } else {
        HubRewardReportStatusType statusType = rewardReportStatus.getStatus() == null ? rewardReportStatus.getStatus()
                                                                                      : HubRewardReportStatusType.SENT;
        hubReportStorage.saveRewardPeriodStatus(periodId, statusType.name());
        rewardReportStatus.setStatus(statusType);
        return new HubRewardReportLocalStatus(rewardReportStatus,
                                              periodId,
                                              statusType.isCanRefresh(),
                                              statusType.isCanSend());

      }
    } else {
      RewardReport rewardReport = rewardReportService.getRewardReportByPeriodId(periodId);
      if (rewardReport == null) {
        throw new WomException("wom.unableToRetrieveReward");
      } else {
        return computeReportStatus(rewardReport);
      }
    }
  }

  public HubRewardReport toReport(RewardReport rewardReport) {
    RewardPeriod rewardPeriod = rewardReport.getPeriod();
    Date fromDate = Date.from(Instant.ofEpochSecond(rewardPeriod.getStartDateInSeconds()));
    Date toDate = Date.from(Instant.ofEpochSecond(rewardPeriod.getEndDateInSeconds()));

    return toHubReport(rewardReport,
                       hubService.getHubAddress(),
                       hubService.getDeedId(),
                       countParticipants(fromDate, toDate),
                       countAchievements(fromDate, toDate));
  }

  private boolean isValidRewardDate(RewardPeriod rewardPeriod) {
    Instant endDate = Instant.ofEpochMilli(rewardPeriod.getEndDateInSeconds());
    return SEND_OUTDATED_REPORT || hubService.getHubJoinDate().isBefore(endDate);
  }

  private void markReportAsSent(RewardPeriod rewardPeriod, String rewardPeriodHash) {
    hubReportStorage.saveRewardPeriodHash(rewardPeriod, rewardPeriodHash);
    hubReportStorage.saveRewardPeriodStatus(rewardPeriod, HubRewardReportStatusType.SENT.name());
  }

  private void markReportAsError(RewardPeriod rewardPeriod, String rewardPeriodHash, WomException e) throws WomParsingException {
    hubReportStorage.saveRewardPeriodHash(rewardPeriod, rewardPeriodHash);
    hubReportStorage.saveRewardPeriodStatus(rewardPeriod, toJsonString(e.getErrorCode()));
  }

  private HubRewardReportStatusType computeReportStatusType(RewardPeriod rewardPeriod, String status) {
    if (StringUtils.isBlank(status)) {
      if (!hubService.isDeedHub() || isValidRewardDate(rewardPeriod)) {
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

  private HubRewardReportLocalStatus computeReportStatus(RewardReport rewardReport) {
    RewardPeriod rewardPeriod = rewardReport.getPeriod();
    String status = hubReportStorage.getRewardPeriodStatus(rewardPeriod);

    HubRewardReportStatusType statusType = computeReportStatusType(rewardPeriod, status);
    String errorMessageKey = statusType == HubRewardReportStatusType.ERROR_SENDING ? status : null;

    HubRewardReport hubRewardReport = toReport(rewardReport);
    String hash = hubReportStorage.getRewardPeriodHash(rewardPeriod);
    HubRewardReportLocalStatus hubRewardReportStatus = new HubRewardReportLocalStatus(hubReportStorage.getPeriodKey(rewardPeriod),
                                                                                      hash,
                                                                                      hubRewardReport,
                                                                                      statusType,
                                                                                      errorMessageKey,
                                                                                      statusType.isCanRefresh()
                                                                                          && StringUtils.isNotBlank(hash),
                                                                                      statusType.isCanSend()
                                                                                          && isValidRewardDate(rewardPeriod));
    if (StringUtils.isBlank(hash)) {
      hubRewardReport.setDeedId(-1);
    }
    return hubRewardReportStatus;
  }

}
