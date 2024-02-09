/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
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
package io.meeds.wom.api.model;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.Instant;
import java.util.SortedSet;

import org.apache.commons.lang3.StringUtils;
import org.springframework.hateoas.server.core.Relation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@JsonInclude(value = Include.NON_EMPTY)
@Relation(collectionRelation = "reports", itemRelation = "report")
public class HubReport extends HubReportPayload {

  private long    rewardId;

  private short   city;

  private short   cardType;

  private short   mintingPower;

  private long    maxUsers;

  private String  deedManagerAddress;

  private String  ownerAddress;

  private int     ownerMintingPercentage;

  private double  fixedRewardIndex;

  private double  ownerFixedIndex;

  private double  tenantFixedIndex;

  private boolean fraud;

  /**
   * UEM Computed values
   */
  private double  lastPeriodUemRewardAmount;

  private double  uemRewardAmount;

  private double  engagementScore;

  private Instant updatedDate;

  public HubReport(HubReportPayload hubReportPayload) {
    super(hubReportPayload.getReportId(),
          StringUtils.lowerCase(hubReportPayload.getHubAddress()),
          hubReportPayload.getDeedId(),
          hubReportPayload.getFromDate(),
          hubReportPayload.getToDate(),
          hubReportPayload.getSentDate(),
          hubReportPayload.getPeriodType(),
          hubReportPayload.getUsersCount(),
          hubReportPayload.getParticipantsCount(),
          hubReportPayload.getRecipientsCount(),
          hubReportPayload.getAchievementsCount(),
          hubReportPayload.getActionsCount(),
          StringUtils.lowerCase(hubReportPayload.getRewardTokenAddress()),
          hubReportPayload.getRewardTokenNetworkId(),
          hubReportPayload.getHubRewardAmount(),
          hubReportPayload.getHubTopRewardedAmount(),
          lowerCase(hubReportPayload.getTransactions()));
  }

  public HubReport(long reportId, // NOSONAR
                   String hubAddress,
                   long deedId,
                   Instant fromDate,
                   Instant toDate,
                   Instant sentDate,
                   String periodType,
                   long usersCount,
                   long participantsCount,
                   long recipientsCount,
                   long achievementsCount,
                   long actionsCount,
                   String rewardTokenAddress,
                   long rewardTokenNetworkId,
                   double hubRewardAmount,
                   double hubTopReceiverAmount,
                   SortedSet<String> transactions,
                   long rewardId,
                   short city,
                   short cardType,
                   short mintingPower,
                   long maxUsers,
                   String deedManagerAddress,
                   String ownerAddress,
                   int ownerMintingPercentage,
                   double fixedRewardIndex,
                   double ownerFixedIndex,
                   double tenantFixedIndex,
                   boolean fraud,
                   double lastPeriodUemRewardAmount,
                   double uemRewardAmount,
                   Instant updatedDate) {
    super(reportId,
          hubAddress,
          deedId,
          fromDate,
          toDate,
          sentDate,
          periodType,
          usersCount,
          participantsCount,
          recipientsCount,
          achievementsCount,
          actionsCount,
          rewardTokenAddress,
          rewardTokenNetworkId,
          hubRewardAmount,
          hubTopReceiverAmount,
          transactions);
    this.rewardId = rewardId;
    this.city = city;
    this.cardType = cardType;
    this.mintingPower = mintingPower;
    this.maxUsers = maxUsers;
    this.deedManagerAddress = deedManagerAddress;
    this.ownerAddress = ownerAddress;
    this.ownerMintingPercentage = ownerMintingPercentage;
    this.fixedRewardIndex = fixedRewardIndex;
    this.ownerFixedIndex = ownerFixedIndex;
    this.tenantFixedIndex = tenantFixedIndex;
    this.fraud = fraud;
    this.lastPeriodUemRewardAmount = lastPeriodUemRewardAmount;
    this.uemRewardAmount = uemRewardAmount;
    this.updatedDate = updatedDate;
  }

  public HubReport(long reportId, // NOSONAR
                   String hubAddress,
                   long deedId,
                   Instant fromDate,
                   Instant toDate,
                   Instant sentDate,
                   String periodType,
                   long usersCount,
                   long participantsCount,
                   long recipientsCount,
                   long achievementsCount,
                   long actionsCount,
                   String rewardTokenAddress,
                   long rewardTokenNetworkId,
                   double hubRewardAmount,
                   double hubTopReceiverAmount,
                   SortedSet<String> transactions,
                   long rewardId,
                   short city,
                   short cardType,
                   short mintingPower,
                   long maxUsers,
                   String deedManagerAddress,
                   String ownerAddress,
                   int ownerMintingPercentage,
                   double fixedRewardIndex,
                   double ownerFixedIndex,
                   double tenantFixedIndex,
                   boolean fraud,
                   double lastPeriodUemRewardAmount,
                   double uemRewardAmount,
                   Instant updatedDate,
                   double engagementScore) {
    this(reportId,
         hubAddress,
         deedId,
         fromDate,
         toDate,
         sentDate,
         periodType,
         usersCount,
         participantsCount,
         recipientsCount,
         achievementsCount,
         actionsCount,
         rewardTokenAddress,
         rewardTokenNetworkId,
         hubRewardAmount,
         hubTopReceiverAmount,
         transactions,
         rewardId,
         city,
         cardType,
         mintingPower,
         maxUsers,
         deedManagerAddress,
         ownerAddress,
         ownerMintingPercentage,
         fixedRewardIndex,
         ownerFixedIndex,
         tenantFixedIndex,
         fraud,
         lastPeriodUemRewardAmount,
         uemRewardAmount,
         updatedDate);
    this.engagementScore = engagementScore;
  }

  public double getEd() {
    double achievementsCount = getAchievementsCount();
    double participantsCount = getParticipantsCount();
    if (participantsCount == 0 || achievementsCount == 0) {
      return 0;
    } else {
      return BigDecimal.valueOf(achievementsCount)
                       .divide(BigDecimal.valueOf(participantsCount), MathContext.DECIMAL128)
                       .doubleValue();
    }
  }

  public double getDr() {
    return lastPeriodUemRewardAmount == 0 ? 1d :
                                          BigDecimal.valueOf(getHubRewardAmount())
                                                    .divide(BigDecimal.valueOf(lastPeriodUemRewardAmount),
                                                            MathContext.DECIMAL128)
                                                    .doubleValue();
  }

  public double getDs() {
    return getUsersCount() == 0 ? 0d :
                                BigDecimal.valueOf(Math.min(getRecipientsCount(), getMaxUsers()))
                                          .divide(BigDecimal.valueOf(getUsersCount()), MathContext.DECIMAL128)
                                          .doubleValue();
  }

  public double getMp() {
    return mintingPower / 100d;
  }

}
