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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@JsonInclude(value = Include.NON_EMPTY)
@Relation(collectionRelation = "reports", itemRelation = "report")
public class HubReport extends HubReportPayload {

  @Getter
  @Setter
  private long    rewardId;

  @Getter
  @Setter
  private short   city;

  @Getter
  @Setter
  private short   cardType;

  @Getter
  @Setter
  private short   mintingPower;

  @Getter
  @Setter
  private long    maxUsers;

  @Getter
  private String  deedManagerAddress;

  @Getter
  private String  ownerAddress;

  @Getter
  @Setter
  private int     ownerMintingPercentage;

  @Getter
  @Setter
  private double  fixedRewardIndex;

  @Getter
  @Setter
  private double  ownerFixedIndex;

  @Getter
  @Setter
  private double  tenantFixedIndex;

  @Getter
  @Setter
  private boolean fraud;

  @Getter
  @Setter
  private double  uemRewardIndex;

  @Getter
  @Setter
  private double  uemRewardAmount;

  @Getter
  @Setter
  private double  lastPeriodUemRewardAmount;

  public HubReport(long reportId, // NOSONAR
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
          StringUtils.lowerCase(hubAddress),
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
    this.rewardId = rewardId;
    this.city = city;
    this.cardType = cardType;
    this.mintingPower = mintingPower;
    this.maxUsers = maxUsers;
    this.deedManagerAddress = StringUtils.lowerCase(deedManagerAddress);
    this.ownerAddress = StringUtils.lowerCase(ownerAddress);
    this.ownerMintingPercentage = ownerMintingPercentage;
    this.uemRewardIndex = uemRewardIndex;
    this.uemRewardAmount = uemRewardAmount;
  }

  public void setDeedManagerAddress(String deedManagerAddress) {
    this.deedManagerAddress = StringUtils.lowerCase(deedManagerAddress);
  }

  public void setOwnerAddress(String ownerAddress) {
    this.ownerAddress = StringUtils.lowerCase(ownerAddress);
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
                                BigDecimal.valueOf(getRecipientsCount())
                                          .divide(BigDecimal.valueOf(getUsersCount()), MathContext.DECIMAL128)
                                          .doubleValue();
  }

}
