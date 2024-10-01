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

import static io.meeds.tenant.hub.utils.EntityMapper.toHubLocalReport;
import static io.meeds.tenant.hub.utils.EntityMapper.toHubReport;
import static io.meeds.wom.api.utils.JsonUtils.toJsonString;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import io.meeds.wallet.reward.service.RewardReportService;
import org.apache.commons.lang3.StringUtils;
import org.exoplatform.container.ExoContainerContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Hash;

import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.UserStatus;
import io.meeds.wallet.wallet.model.reward.RewardPeriod;
import io.meeds.wallet.wallet.model.reward.RewardReport;

import io.meeds.gamification.constant.DateFilterType;
import io.meeds.gamification.constant.EntityStatusType;
import io.meeds.gamification.constant.IdentityType;
import io.meeds.gamification.constant.RealizationStatus;
import io.meeds.gamification.model.filter.RealizationFilter;
import io.meeds.gamification.model.filter.RuleFilter;
import io.meeds.gamification.service.RealizationService;
import io.meeds.gamification.service.RuleService;
import io.meeds.tenant.hub.constant.HubReportStatusType;
import io.meeds.tenant.hub.model.HubReportLocalStatus;
import io.meeds.tenant.hub.model.HubTenant;
import io.meeds.tenant.hub.rest.client.WomClientService;
import io.meeds.tenant.hub.storage.HubReportStorage;
import io.meeds.tenant.hub.storage.HubWalletStorage;
import io.meeds.wom.api.constant.WomException;
import io.meeds.wom.api.constant.WomParsingException;
import io.meeds.wom.api.model.HubReport;
import io.meeds.wom.api.model.HubReportPayload;
import io.meeds.wom.api.model.HubReportVerifiableData;

import lombok.SneakyThrows;

@Service
public class HubReportService {

  public static final String  REPORT_SENT_EVENT                = "deed.tenant.report.sent";

  public static final String  REPORT_SENDING_IN_PROGRESS_EVENT = "deed.tenant.report.sending";

  public static final String  REPORT_SENDING_ERROR_EVENT       = "deed.tenant.report.error";

  public static final String  REPORT_PERSISTED_EVENT           = "deed.tenant.report.persistedInServer";

  public static final String  REPORT_PERSIST_ERROR_EVENT       = "deed.tenant.report.persistError";

  private static final String DEFAULT_LOG_LANG                 = Locale.ENGLISH.toLanguageTag();

  private static final Log    LOG                              = ExoLogger.getLogger(HubReportService.class);

  @Autowired
  private OrganizationService organizationService;

  @Autowired
  private RealizationService  realizationService;

  @Autowired
  private RuleService         ruleService;

  @Autowired
  private HubService          hubService;

  @Autowired
  private WomClientService    womServiceClient;

  @Autowired
  private HubWalletStorage    hubWalletStorage;

  @Autowired
  private HubReportStorage    hubReportStorage;

  @Autowired
  private ListenerService     listenerService;

  private RewardReportService rewardReportService;

  public HubReportLocalStatus sendReport(long periodId) throws WomException {
    RewardReport rewardReport = getRewardReportService().getRewardReportByPeriodId(periodId);
    if (rewardReport == null) {
      return null;
    } else {
      return sendReport(rewardReport);
    }
  }

  public HubReportLocalStatus sendReport(RewardReport rewardReport) throws WomException { // NOSONAR
    if (!hubService.isConnected()) {
      return null;
    }

    RewardPeriod rewardPeriod = rewardReport.getPeriod();
    if (!rewardReport.isCompletelyProceeded()) {
      return null;
    } else {
      HubReportPayload reportData = toReport(rewardReport);

      HubTenant hub = hubService.getHub(true);
      long start = System.currentTimeMillis();
      LOG.info("Sending: Hub Report of Reward date '{}' to '{}' to UEM",
               rewardPeriod.getStartDateFormatted(DEFAULT_LOG_LANG),
               rewardPeriod.getEndDateFormatted(DEFAULT_LOG_LANG));

      long reportId = sendTransaction(rewardPeriod, reportData, hub);
      reportData.setReportId(reportId);

      LOG.info("Sent: Hub Report with id '{}' of Reward date '{}' to '{}' to UEM within {}ms",
               rewardPeriod.getStartDateFormatted(DEFAULT_LOG_LANG),
               rewardPeriod.getEndDateFormatted(DEFAULT_LOG_LANG),
               reportId,
               System.currentTimeMillis() - start);

      HubReport report = persistReport(reportData);
      return toHubLocalReport(report,
                              hubReportStorage.getPeriodKey(rewardPeriod),
                              HubReportStatusType.SENT.isCanRefresh(),
                              HubReportStatusType.SENT.isCanSend(),
                              HubReportStatusType.SENT,
                              null);
    }
  }

  public Page<HubReportLocalStatus> getReports(Pageable pageable) {
    Page<RewardPeriod> rewardPeriods = getRewardReportService().findRewardReportPeriods(pageable);

    if (rewardPeriods == null || rewardPeriods.isEmpty()) {
      return new PageImpl<>(Collections.emptyList(), pageable, 0);
    }

    List<HubReportLocalStatus> newReports = rewardPeriods.getContent()
                                                         .stream()
                                                         .map(p -> getRewardReportService().getRewardReport(p.getPeriodMedianDate()))
                                                         .filter(Objects::nonNull)
                                                         .map(this::generateNewReport)
                                                         .collect(Collectors.toList());

    return new PageImpl<>(newReports, pageable, rewardPeriods.getTotalElements());
  }

  public HubReportLocalStatus getReport(long periodId, boolean refresh) throws WomException {
    if (refresh) {
      return retrieveReport(periodId);
    } else {
      RewardReport rewardReport = getRewardReportService().getRewardReportByPeriodId(periodId);
      if (rewardReport == null) {
        throw new WomException("wom.unableToRetrieveReward");
      } else {
        return generateNewReport(rewardReport);
      }
    }
  }

  public long getReportId(long periodId) {
    return hubReportStorage.getReportId(periodId);
  }

  public long getPeriodId(long reportId) {
    return hubReportStorage.getPeriodId(reportId);
  }

  private HubReportLocalStatus retrieveReport(long periodId) throws WomException {
    long reportId = hubReportStorage.getReportId(periodId);
    if (reportId == 0) {
      throw new WomException("wom.notSentReward");
    }
    HubReport report = womServiceClient.retrieveReport(reportId);
    if (report == null) {
      throw new WomException("wom.rewardNotFoundInWom");
    } else {
      return toHubLocalReport(report,
                              periodId,
                              HubReportStatusType.SENT.isCanRefresh(),
                              HubReportStatusType.SENT.isCanSend(),
                              HubReportStatusType.SENT,
                              null);

    }
  }

  private long sendTransaction(RewardPeriod rewardPeriod, HubReportPayload reportData, HubTenant hub) throws WomException {
    long reportId;
    try {
      markReportAsSending(rewardPeriod);
      reportId = hubWalletStorage.sendReportTransaction(reportData, hub.getUemAddress(), hub.getNetworkId());
      markReportAsSent(rewardPeriod, reportId);
    } catch (WomException e) {
      markReportAsError(rewardPeriod, e);
      throw e;
    } catch (RuntimeException e) {
      markReportAsError(rewardPeriod, new WomException("wom.unknownError", true));
      throw e;
    } finally {
      String status = hubReportStorage.getStatus(rewardPeriod);
      HubReportStatusType statusType = computeReportStatusType(status);
      if (statusType == HubReportStatusType.SENDING) {
        markReportAsError(rewardPeriod, new WomException("wom.unknownError", true));
      }
    }
    return reportId;
  }

  @SneakyThrows
  private HubReport persistReport(HubReportPayload reportData) throws WomException {
    String signature = signHubMessage(reportData);
    String hash = StringUtils.lowerCase(Hash.sha3(signature));
    HubReportVerifiableData reportRequest = new HubReportVerifiableData(hash,
                                                                        signature,
                                                                        reportData);
    HubReport report = womServiceClient.saveReport(reportRequest);
    listenerService.broadcast(REPORT_PERSISTED_EVENT, reportRequest.getReportId(), null);
    return report;
  }

  private String signHubMessage(HubReportPayload reportData) throws WomException {
    String rawRequest = toJsonString(reportData);
    return hubWalletStorage.signHubMessage(rawRequest);
  }

  private HubReportPayload toReport(RewardReport rewardReport) {
    RewardPeriod rewardPeriod = rewardReport.getPeriod();
    Date fromDate = Date.from(Instant.ofEpochSecond(rewardPeriod.getStartDateInSeconds()));
    Date toDate = Date.from(Instant.ofEpochSecond(rewardPeriod.getEndDateInSeconds()));

    return toHubReport(rewardReport,
                       hubService.getHubAddress(),
                       hubService.getDeedId(),
                       computeUsersCount(),
                       countParticipants(fromDate, toDate),
                       countAchievements(fromDate, toDate),
                       countActions(),
                       hubReportStorage.getSentDate(rewardPeriod));
  }

  @SneakyThrows
  private void markReportAsSent(RewardPeriod rewardPeriod, long reportId) {
    hubReportStorage.saveReportPeriodId(rewardPeriod, reportId);
    hubReportStorage.saveStatus(rewardPeriod, HubReportStatusType.SENT.name());
    hubReportStorage.saveSentDate(rewardPeriod, Instant.now());
    listenerService.broadcast(REPORT_SENT_EVENT, rewardPeriod.getId(), reportId);
  }

  @SneakyThrows
  private void markReportAsSending(RewardPeriod rewardPeriod) {
    hubReportStorage.saveStatus(rewardPeriod, HubReportStatusType.SENDING.name());
    listenerService.broadcast(REPORT_SENDING_IN_PROGRESS_EVENT, rewardPeriod.getId(), null);
  }

  @SneakyThrows
  private void markReportAsError(RewardPeriod rewardPeriod, String error) {
    hubReportStorage.saveStatus(rewardPeriod, error);
    listenerService.broadcast(REPORT_SENDING_ERROR_EVENT, rewardPeriod.getId(), null);
  }

  private void markReportAsError(RewardPeriod rewardPeriod, WomException e) throws WomParsingException {
    String error = toJsonString(e.getErrorCode());
    markReportAsError(rewardPeriod, error);
  }

  private HubReportStatusType computeReportStatusType(String status) {
    if (StringUtils.isBlank(status)) {
      if (!hubService.isConnected()) {
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
      case "SENDING":
        yield HubReportStatusType.SENDING;
      case "INVALID":
        yield HubReportStatusType.INVALID;
      default:
        yield HubReportStatusType.ERROR_SENDING;
      };
    }
  }

  @SneakyThrows
  public long computeUsersCount() {
    return organizationService.getUserHandler().findAllUsers(UserStatus.ENABLED).getSize();
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

  private int countActions() {
    RuleFilter ruleFilter = new RuleFilter();
    ruleFilter.setAllSpaces(true);
    ruleFilter.setStatus(EntityStatusType.ENABLED);
    ruleFilter.setProgramStatus(EntityStatusType.ENABLED);
    ruleFilter.setDateFilterType(DateFilterType.STARTED);
    return ruleService.countRules(ruleFilter);
  }

  private HubReportLocalStatus generateNewReport(RewardReport rewardReport) {
    RewardPeriod rewardPeriod = rewardReport.getPeriod();
    String status = hubReportStorage.getStatus(rewardPeriod);

    HubReportStatusType statusType = computeReportStatusType(status);
    String errorMessageKey = statusType == HubReportStatusType.ERROR_SENDING ? status : null;

    HubReportPayload reportData = toReport(rewardReport);
    long reportId = hubReportStorage.getReportId(rewardPeriod);
    long periodId = hubReportStorage.getPeriodKey(rewardPeriod);
    boolean canRefresh = statusType.isCanRefresh() && reportId == 0;
    boolean canSend = statusType.isCanSend();

    HubReportLocalStatus reportLocalStatus = toHubLocalReport(reportData,
                                                              periodId,
                                                              reportId,
                                                              canRefresh,
                                                              canSend,
                                                              statusType,
                                                              errorMessageKey);
    if (reportId == 0 && !canSend) {
      reportLocalStatus.setDeedId(-1);
    }
    return reportLocalStatus;
  }

  protected RewardReportService getRewardReportService() {
    if (rewardReportService == null) {
      rewardReportService = ExoContainerContext.getService(RewardReportService.class);
    }
    return rewardReportService;
  }

}
