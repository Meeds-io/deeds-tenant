/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2023 Meeds Association contact@meeds.io
 *
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
package io.meeds.tenant.hub.utils;

import static org.exoplatform.wallet.utils.WalletUtils.getContractAddress;
import static org.exoplatform.wallet.utils.WalletUtils.getNetworkId;

import java.time.Instant;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import org.exoplatform.wallet.model.reward.RewardPeriod;
import org.exoplatform.wallet.model.reward.RewardReport;
import org.exoplatform.wallet.model.reward.WalletReward;
import org.exoplatform.wallet.model.transaction.TransactionDetail;

import io.meeds.tenant.hub.model.HubReportLocalStatus;
import io.meeds.wom.api.constant.HubReportStatusType;
import io.meeds.wom.api.model.Hub;
import io.meeds.wom.api.model.HubReport;
import io.meeds.wom.api.model.HubReportPayload;

public class EntityMapper {

  private EntityMapper() {
    // Utils Class
  }

  public static HubReportPayload toHubReport(RewardReport rewardReport,
                                             String hubAddress,
                                             long deedId,
                                             long usersCount,
                                             long participantsCount,
                                             long achievementsCount,
                                             Instant sentDate) {
    RewardPeriod rewardPeriod = rewardReport.getPeriod();
    long recipientsCount = rewardReport.getValidRewardCount();
    double hubRewardAmount = rewardReport.getTokensSent();
    SortedSet<String> transactions = rewardReport.getValidRewards()
                                                 .stream()
                                                 .filter(Objects::nonNull)
                                                 .map(WalletReward::getTransaction)
                                                 .filter(Objects::nonNull)
                                                 .filter(TransactionDetail::isSucceeded)
                                                 .map(TransactionDetail::getHash)
                                                 .filter(Objects::nonNull)
                                                 .collect(Collectors.toCollection(TreeSet::new));

    String periodType = rewardPeriod.getRewardPeriodType().name();
    Instant toDate = Instant.ofEpochSecond(rewardPeriod.getEndDateInSeconds());
    Instant fromDate = Instant.ofEpochSecond(rewardPeriod.getStartDateInSeconds());
    String rewardTokenAddress = getContractAddress();
    long rewardTokenNetworkId = getNetworkId();

    return new HubReportPayload(StringUtils.lowerCase(hubAddress),
                                deedId,
                                fromDate,
                                toDate,
                                sentDate,
                                periodType,
                                usersCount,
                                participantsCount,
                                recipientsCount,
                                achievementsCount,
                                StringUtils.lowerCase(rewardTokenAddress),
                                rewardTokenNetworkId,
                                hubRewardAmount,
                                lowerCase(transactions));
  }

  public static HubReportLocalStatus toHubLocalReport(HubReportPayload reportData, // NOSONAR
                                                      Hub hub,
                                                      long id,
                                                      long reportId,
                                                      boolean canRefresh,
                                                      boolean canSend,
                                                      HubReportStatusType statusType,
                                                      String errorMessageKey) {
    return new HubReportLocalStatus(id,
                                    canRefresh,
                                    canSend,
                                    reportId,
                                    null,
                                    null,
                                    reportData.getHubAddress(),
                                    reportData.getDeedId(),
                                    reportData.getFromDate(),
                                    reportData.getToDate(),
                                    reportData.getSentDate(),
                                    reportData.getPeriodType(),
                                    reportData.getUsersCount(),
                                    reportData.getParticipantsCount(),
                                    reportData.getRecipientsCount(),
                                    reportData.getAchievementsCount(),
                                    reportData.getRewardTokenAddress(),
                                    reportData.getRewardTokenNetworkId(),
                                    reportData.getHubRewardAmount(),
                                    lowerCase(reportData.getTransactions()),
                                    hub == null ? null : StringUtils.lowerCase(hub.getEarnerAddress()),
                                    hub == null ? null : StringUtils.lowerCase(hub.getDeedManagerAddress()),
                                    hub == null ? null : StringUtils.lowerCase(hub.getDeedOwnerAddress()),
                                    0,
                                    statusType,
                                    errorMessageKey,
                                    // Computed in WoM Server
                                    0d,
                                    0d,
                                    0d,
                                    0d,
                                    0d,
                                    0d,
                                    0d,
                                    0d,
                                    0l);
  }

  public static HubReportLocalStatus toHubLocalReport(HubReport report,
                                                      long id,
                                                      boolean canRefresh,
                                                      boolean canSend) {
    return new HubReportLocalStatus(id,
                                    canRefresh,
                                    canSend,
                                    report.getReportId(),
                                    report.getHash(),
                                    report.getSignature(),
                                    report.getHubAddress(),
                                    report.getDeedId(),
                                    report.getFromDate(),
                                    report.getToDate(),
                                    report.getSentDate(),
                                    report.getPeriodType(),
                                    report.getUsersCount(),
                                    report.getParticipantsCount(),
                                    report.getRecipientsCount(),
                                    report.getAchievementsCount(),
                                    report.getRewardTokenAddress(),
                                    report.getRewardTokenNetworkId(),
                                    report.getHubRewardAmount(),
                                    lowerCase(report.getTransactions()),
                                    StringUtils.lowerCase(report.getEarnerAddress()),
                                    StringUtils.lowerCase(report.getDeedManagerAddress()),
                                    StringUtils.lowerCase(report.getOwnerAddress()),
                                    report.getOwnerMintingPercentage(),
                                    report.getStatus(),
                                    report.getError(),
                                    // Computed in WoM Server
                                    report.getUemRewardIndex(),
                                    report.getUemRewardAmount(),
                                    report.getLastPeriodUemRewardAmount(),
                                    report.getLastPeriodUemDiff(),
                                    report.getHubRewardAmountPerPeriod(),
                                    report.getHubRewardLastPeriodDiff(),
                                    report.getLastPeriodUemRewardAmountPerPeriod(),
                                    report.getMp(),
                                    report.getRewardId());
  }

  public static SortedSet<String> lowerCase(SortedSet<String> hashes) {
    if (CollectionUtils.isEmpty(hashes)) {
      return new TreeSet<>();
    } else {
      return hashes.stream()
                   .map(StringUtils::lowerCase)
                   .collect(Collectors.toCollection(TreeSet::new));
    }
  }

}
