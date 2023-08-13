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

import static io.meeds.deeds.api.utils.JsonUtils.toJsonString;
import static io.meeds.tenant.utils.EntityMapper.toHubReport;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Hash;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wallet.model.reward.RewardPeriod;
import org.exoplatform.wallet.model.reward.RewardReport;
import org.exoplatform.wallet.reward.service.RewardReportService;

import io.meeds.deeds.api.constant.HubReportStatusType;
import io.meeds.deeds.api.constant.WomException;
import io.meeds.deeds.api.constant.WomParsingException;
import io.meeds.deeds.api.model.Hub;
import io.meeds.deeds.api.model.HubReport;
import io.meeds.deeds.api.model.HubReportData;
import io.meeds.deeds.api.model.HubReportRequest;
import io.meeds.gamification.constant.IdentityType;
import io.meeds.gamification.constant.RealizationStatus;
import io.meeds.gamification.model.filter.RealizationFilter;
import io.meeds.gamification.service.RealizationService;
import io.meeds.tenant.model.HubReportLocalStatus;
import io.meeds.tenant.rest.client.WoMServiceClient;
import io.meeds.tenant.storage.HubReportStorage;

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
  private RewardReportService  rewardReportService;

  @Autowired
  private RealizationService   realizationService;

  @Autowired
  private HubService           hubService;

  @Autowired
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

  public HubReportLocalStatus sendReport(long periodId) throws WomException {
    RewardReport rewardReport = rewardReportService.getRewardReportByPeriodId(periodId);
    if (rewardReport == null) {
      return null;
    } else {
      return sendReport(rewardReport);
    }
  }

  public HubReportLocalStatus sendReport(RewardReport rewardReport) throws WomException {
    if (!hubService.isDeedHub()) {
      return null;
    }

    RewardPeriod rewardPeriod = rewardReport.getPeriod();
    if (!rewardReport.isCompletelyProceeded()) {
      throw new IllegalStateException("Reward of period '" + rewardPeriod
          + "' isn't completed proceeded, thus the Rewards report will not be sent");
    } else if (isValidRewardDate(rewardPeriod)) {
      HubReportData report = toReport(rewardReport);

      HubReportRequest reportRequest = new HubReportRequest();
      reportRequest.setReport(report);
      reportRequest.setSignature(hubService.signHubMessage(report));
      String rewardPeriodHash = Hash.sha3(reportRequest.getSignature());
      reportRequest.setHash(rewardPeriodHash);

      try {
        HubReport reportStatus = womServiceClient.sendReport(reportRequest);
        markReportAsSent(rewardPeriod, rewardPeriodHash);
        return new HubReportLocalStatus(reportStatus,
                                        hubReportStorage.getPeriodKey(rewardPeriod),
                                        reportStatus.getStatus().isCanRefresh(),
                                        reportStatus.getStatus().isCanSend());
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

  public List<HubReportLocalStatus> getReports(int offset, int limit) {
    List<RewardPeriod> rewardPeriods = rewardReportService.findRewardReportPeriods(offset, limit);
    if (CollectionUtils.isEmpty(rewardPeriods)) {
      return Collections.emptyList();
    } else {
      return rewardPeriods.stream()
                          .map(p -> rewardReportService.getRewardReport(p.getPeriodMedianDate()))
                          .filter(Objects::nonNull)
                          .map(this::generateNewReport)
                          .toList();
    }
  }

  public HubReportLocalStatus getReport(long periodId, boolean refresh) throws WomException {
    if (refresh) {
      String hash = hubReportStorage.getRewardPeriodHash(periodId);
      if (StringUtils.isBlank(hash)) {
        hubReportStorage.cleanRewardPeriodStatus(periodId);
        throw new WomException("wom.notSentReward");
      }
      HubReport reportStatus = womServiceClient.retrieveReport(hash);
      if (reportStatus == null || !StringUtils.equalsIgnoreCase(reportStatus.getHubAddress(), hubService.getHubAddress())) {
        hubReportStorage.cleanRewardPeriodHash(periodId);
        hubReportStorage.cleanRewardPeriodStatus(periodId);
        throw new WomException("wom.rewardNotFoundInWom");
      } else {
        HubReportStatusType statusType = reportStatus.getStatus() == null ? HubReportStatusType.SENT
                                                                          : reportStatus.getStatus();
        hubReportStorage.saveRewardPeriodStatus(periodId, statusType.name());
        reportStatus.setStatus(statusType);
        return new HubReportLocalStatus(reportStatus,
                                        periodId,
                                        statusType.isCanRefresh(),
                                        statusType.isCanSend());

      }
    } else {
      RewardReport rewardReport = rewardReportService.getRewardReportByPeriodId(periodId);
      if (rewardReport == null) {
        throw new WomException("wom.unableToRetrieveReward");
      } else {
        return generateNewReport(rewardReport);
      }
    }
  }

  public HubReportData toReport(RewardReport rewardReport) {
    RewardPeriod rewardPeriod = rewardReport.getPeriod();
    Date fromDate = Date.from(Instant.ofEpochSecond(rewardPeriod.getStartDateInSeconds()));
    Date toDate = Date.from(Instant.ofEpochSecond(rewardPeriod.getEndDateInSeconds()));

    return toHubReport(rewardReport,
                       hubService.getHubAddress(),
                       hubService.getDeedId(),
                       hubService.computeUsersCount(),
                       countParticipants(fromDate, toDate),
                       countAchievements(fromDate, toDate));
  }

  private boolean isValidRewardDate(RewardPeriod rewardPeriod) {
    Instant endDate = Instant.ofEpochMilli(rewardPeriod.getEndDateInSeconds());
    return SEND_OUTDATED_REPORT || hubService.getHubJoinDate().isBefore(endDate);
  }

  private void markReportAsSent(RewardPeriod rewardPeriod, String rewardPeriodHash) {
    hubReportStorage.saveRewardPeriodHash(rewardPeriod, rewardPeriodHash);
    hubReportStorage.saveRewardPeriodStatus(rewardPeriod, HubReportStatusType.SENT.name());
  }

  private void markReportAsError(RewardPeriod rewardPeriod, String rewardPeriodHash, WomException e) throws WomParsingException {
    hubReportStorage.saveRewardPeriodHash(rewardPeriod, rewardPeriodHash);
    hubReportStorage.saveRewardPeriodStatus(rewardPeriod, toJsonString(e.getErrorCode()));
  }

  private HubReportStatusType computeReportStatusType(RewardPeriod rewardPeriod, String status) {
    if (StringUtils.isBlank(status)) {
      if (!hubService.isDeedHub() || isValidRewardDate(rewardPeriod)) {
        return HubReportStatusType.NONE;
      } else {
        return HubReportStatusType.INVALID;
      }
    } else {
      return switch (status) {
      case "SENT":
        yield HubReportStatusType.SENT;
      case "PENDING_REWARD":
        yield HubReportStatusType.PENDING_REWARD;
      case "REWARDED":
        yield HubReportStatusType.REWARDED;
      case "REJECTED":
        yield HubReportStatusType.REJECTED;
      default:
        yield HubReportStatusType.ERROR_SENDING;
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

  private HubReportLocalStatus generateNewReport(RewardReport rewardReport) {
    RewardPeriod rewardPeriod = rewardReport.getPeriod();
    String status = hubReportStorage.getRewardPeriodStatus(rewardPeriod);

    HubReportStatusType statusType = computeReportStatusType(rewardPeriod, status);
    String errorMessageKey = statusType == HubReportStatusType.ERROR_SENDING ? status : null;

    HubReportData report = toReport(rewardReport);
    String hash = hubReportStorage.getRewardPeriodHash(rewardPeriod);

    Hub hub = hubService.getHub();
    HubReport reportStatus = new HubReport(hash,
                                           null,
                                           hub == null ? null : hub.getEarnerAddress(),
                                           hub == null ? null : hub.getDeedManagerAddress(),
                                           statusType,
                                           errorMessageKey,
                                           null,
                                           0d,
                                           0d,
                                           0d,
                                           0d,
                                           0d,
                                           null,
                                           null);
    reportStatus.setReportData(report);
    HubReportLocalStatus reportLocalStatus = new HubReportLocalStatus(reportStatus,
                                                                      hubReportStorage.getPeriodKey(rewardPeriod),
                                                                      statusType.isCanRefresh()
                                                                          && StringUtils.isNotBlank(hash),
                                                                      statusType.isCanSend()
                                                                          && isValidRewardDate(rewardPeriod));
    if (StringUtils.isBlank(hash)) {
      reportLocalStatus.setDeedId(-1);
    }
    return reportLocalStatus;
  }

}
