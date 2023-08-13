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
package io.meeds.tenant.utils;

import static org.exoplatform.wallet.utils.WalletUtils.getContractAddress;
import static org.exoplatform.wallet.utils.WalletUtils.getNetworkId;

import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import org.exoplatform.wallet.model.reward.RewardPeriod;
import org.exoplatform.wallet.model.reward.RewardReport;
import org.exoplatform.wallet.model.reward.WalletReward;
import org.exoplatform.wallet.model.transaction.TransactionDetail;

import io.meeds.deeds.api.model.HubReportData;

public class EntityMapper {

  private EntityMapper() {
    // Utils Class
  }

  public static HubReportData toHubReport(RewardReport rewardReport,
                                          String hubAddress,
                                          long deedId,
                                          long usersCount,
                                          long participantsCount,
                                          long achievementsCount) {
    RewardPeriod rewardPeriod = rewardReport.getPeriod();
    long recipientsCount = rewardReport.getValidRewardCount();
    double hubRewardAmount = rewardReport.getTokensSent();
    Set<String> transactions = rewardReport.getValidRewards()
                                           .stream()
                                           .filter(Objects::nonNull)
                                           .map(WalletReward::getTransaction)
                                           .filter(Objects::nonNull)
                                           .filter(TransactionDetail::isSucceeded)
                                           .map(TransactionDetail::getHash)
                                           .filter(Objects::nonNull)
                                           .collect(Collectors.toSet());

    String periodType = rewardPeriod.getRewardPeriodType().name();
    Instant toDate = Instant.ofEpochSecond(rewardPeriod.getEndDateInSeconds());
    Instant fromDate = Instant.ofEpochSecond(rewardPeriod.getStartDateInSeconds());
    String rewardTokenAddress = getContractAddress();
    long rewardTokenNetworkId = getNetworkId();

    return new HubReportData(StringUtils.lowerCase(hubAddress),
                             deedId,
                             fromDate,
                             toDate,
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

  public static Set<String> lowerCase(Set<String> hashes) {
    if (CollectionUtils.isEmpty(hashes)) {
      return Collections.emptySet();
    } else {
      return hashes.stream()
                   .map(StringUtils::lowerCase)
                   .collect(Collectors.toSet());
    }
  }

}
