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
package io.meeds.tenant.hub.model;

import java.time.Instant;
import java.util.SortedSet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.meeds.wom.api.constant.HubReportStatusType;
import io.meeds.wom.api.model.HubReport;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@JsonInclude(value = Include.NON_EMPTY)
public class HubReportLocalStatus extends HubReport {

  private long                periodId;

  private boolean             canRefresh;

  private boolean             canSend;

  private HubReportStatusType status;

  private String              error;

  public HubReportLocalStatus(long periodId, // NOSONAR
                              boolean canRefresh,
                              boolean canSend,
                              HubReportStatusType status,
                              String error,
                              long reportId,
                              long rewardId,
                              String hubAddress,
                              long deedId,
                              short city,
                              short cardType,
                              short mintingPower,
                              long maxUsers,
                              Instant fromDate,
                              Instant toDate,
                              Instant sentDate,
                              String periodType,
                              long usersCount,
                              long participantsCount,
                              long recipientsCount,
                              long achievementsCount,
                              String rewardTokenAddress,
                              long rewardTokenNetworkId,
                              double hubRewardAmount,
                              SortedSet<String> transactions,
                              String deedManagerAddress,
                              String ownerAddress,
                              int ownerMintingPercentage,
                              double uemRewardIndex,
                              double uemRewardAmount) {
    super(reportId,
          rewardId,
          hubAddress,
          deedId,
          city,
          cardType,
          mintingPower,
          maxUsers,
          fromDate,
          toDate,
          sentDate,
          periodType,
          usersCount,
          participantsCount,
          recipientsCount,
          achievementsCount,
          rewardTokenAddress,
          rewardTokenNetworkId,
          hubRewardAmount,
          transactions,
          deedManagerAddress,
          ownerAddress,
          ownerMintingPercentage,
          uemRewardIndex,
          uemRewardAmount);

    this.periodId = periodId;
    this.canRefresh = canRefresh;
    this.canSend = canSend;
    this.status = status;
    this.error = error;
  }

}
