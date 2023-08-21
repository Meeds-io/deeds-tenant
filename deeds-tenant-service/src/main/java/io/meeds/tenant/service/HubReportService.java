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

import static io.meeds.deeds.api.utils.JsonUtils.fromJsonString;
import static io.meeds.deeds.api.utils.JsonUtils.toJsonString;
import static io.meeds.tenant.utils.EntityMapper.toHubLocalReport;
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
import io.meeds.deeds.api.model.HubReportPayload;
import io.meeds.deeds.api.model.HubReportVerifiableData;
import io.meeds.deeds.api.model.WomErrorMessage;
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
      HubReportPayload reportData = toReport(rewardReport);

      String signature = hubService.signHubMessage(reportData);
      String hash = StringUtils.lowerCase(Hash.sha3(signature));
      HubReportVerifiableData reportRequest = new HubReportVerifiableData(hash,
                                                                          signature,
                                                                          reportData);
      try {
        HubReport report = womServiceClient.sendReport(reportRequest);
        String error = report == null ? null : report.getError();

        if (report != null && StringUtils.isBlank(error)) {
          markReportAsSent(rewardPeriod, hash, report.getSentDate());
          return toHubLocalReport(report,
                                  hubReportStorage.getPeriodKey(rewardPeriod),
                                  report.getStatus().isCanRefresh(),
                                  report.getStatus().isCanSend());
        } else {
          markReportAsError(rewardPeriod, hash, error);
          throw new WomException(fromJsonString(hash, WomErrorMessage.class));
        }
      } catch (WomException e) {
        markReportAsError(rewardPeriod, hash, e);
        throw e;
      } catch (RuntimeException e) {
        markReportAsError(rewardPeriod, hash, new WomException("wom.unknownError", true));
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
      return refreshReportFromWoM(periodId);
    } else {
      RewardReport rewardReport = rewardReportService.getRewardReportByPeriodId(periodId);
      if (rewardReport == null) {
        throw new WomException("wom.unableToRetrieveReward");
      } else {
        return generateNewReport(rewardReport);
      }
    }
  }

  private HubReportLocalStatus refreshReportFromWoM(long periodId) throws WomException {
    String hash = hubReportStorage.getRewardPeriodHash(periodId);
    if (StringUtils.isBlank(hash)) {
      hubReportStorage.cleanRewardPeriodStatus(periodId);
      throw new WomException("wom.notSentReward");
    }
    HubReport report = womServiceClient.retrieveReport(hash);
    if (report == null || !StringUtils.equalsIgnoreCase(report.getHubAddress(), hubService.getHubAddress())) {
      hubReportStorage.cleanRewardPeriodHash(periodId);
      hubReportStorage.cleanRewardPeriodStatus(periodId);
      hubReportStorage.cleanRewardPeriodSentDate(periodId);
      throw new WomException("wom.rewardNotFoundInWom");
    } else {
      HubReportStatusType statusType = report.getStatus() == null ? HubReportStatusType.SENT
                                                                  : report.getStatus();
      report.setStatus(statusType);

      if (StringUtils.isBlank(report.getError())) {
        hubReportStorage.saveRewardPeriodStatus(periodId, statusType.name());
      } else {
        hubReportStorage.saveRewardPeriodStatus(periodId, report.getError());
      }
      return toHubLocalReport(report,
                              periodId,
                              statusType.isCanRefresh(),
                              statusType.isCanSend());

    }
  }

  public HubReportPayload toReport(RewardReport rewardReport) {
    RewardPeriod rewardPeriod = rewardReport.getPeriod();
    Date fromDate = Date.from(Instant.ofEpochSecond(rewardPeriod.getStartDateInSeconds()));
    Date toDate = Date.from(Instant.ofEpochSecond(rewardPeriod.getEndDateInSeconds()));

    return toHubReport(rewardReport,
                       hubService.getHubAddress(),
                       hubService.getDeedId(),
                       hubService.computeUsersCount(),
                       countParticipants(fromDate, toDate),
                       countAchievements(fromDate, toDate),
                       hubReportStorage.getRewardPeriodSentDate(rewardPeriod));
  }

  private boolean isValidRewardDate(RewardPeriod rewardPeriod) {
    Instant endDate = Instant.ofEpochMilli(rewardPeriod.getEndDateInSeconds());
    return SEND_OUTDATED_REPORT || hubService.getHubJoinDate().isBefore(endDate);
  }

  private void markReportAsSent(RewardPeriod rewardPeriod, String hash, Instant sentDate) {
    hubReportStorage.saveRewardPeriodHash(rewardPeriod, hash);
    hubReportStorage.saveRewardPeriodStatus(rewardPeriod, HubReportStatusType.SENT.name());
    hubReportStorage.saveRewardPeriodSentDate(rewardPeriod, sentDate);
  }

  private void markReportAsError(RewardPeriod rewardPeriod, String rewardPeriodHash, WomException e) throws WomParsingException {
    String error = toJsonString(e.getErrorCode());
    markReportAsError(rewardPeriod, rewardPeriodHash, error);
  }

  private void markReportAsError(RewardPeriod rewardPeriod, String rewardPeriodHash, String error) {
    hubReportStorage.saveRewardPeriodHash(rewardPeriod, rewardPeriodHash);
    hubReportStorage.saveRewardPeriodStatus(rewardPeriod, error);
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

    HubReportPayload reportData = toReport(rewardReport);
    String hash = hubReportStorage.getRewardPeriodHash(rewardPeriod);

    Hub hub = hubService.getHub();
    long id = hubReportStorage.getPeriodKey(rewardPeriod);
    boolean canRefresh = statusType.isCanRefresh()
        && StringUtils.isNotBlank(hash);
    boolean canSend = statusType.isCanSend()
        && isValidRewardDate(rewardPeriod);
    HubReportLocalStatus reportLocalStatus = toHubLocalReport(reportData,
                                                              hub,
                                                              id,
                                                              hash,
                                                              canRefresh,
                                                              canSend,
                                                              statusType,
                                                              errorMessageKey);
    if (StringUtils.isBlank(hash)) {
      reportLocalStatus.setDeedId(-1);
    }
    return reportLocalStatus;
  }

}
