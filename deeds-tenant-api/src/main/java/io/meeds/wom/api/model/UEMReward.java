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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;

import org.springframework.hateoas.server.core.Relation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(value = Include.NON_EMPTY)
@Relation(collectionRelation = "rewards", itemRelation = "reward")
public class UEMReward {

  /**
   * UEM Blockchain Id
   */
  private long                    rewardId;

  /**
   * UEM Blockchain configured amount for current period
   */
  private double                  amount;

  /**
   * UEM Blockchain fromReport
   */
  private long                    fromReport;

  /**
   * UEM Blockchain toReport
   */
  private long                    toReport;

  /**
   * UEM Blockchain computed fixed Global Index
   */
  private double                  fixedGlobalIndex;

  /**
   * UEM Period Start Date
   */
  private Instant                 fromDate;

  /**
   * UEM Period End Date
   */
  private Instant                 toDate;

  /**
   * Report Id => UEM Reward amount
   */
  private SortedMap<Long, Double> reportRewards;

  private Set<String>             hubAddresses;

  /**
   * Total internal hub achievements
   */
  private long                    hubAchievementsCount;

  /**
   * Total internal hub rewards sent to hub users
   */
  private double                  hubRewardsAmount;

  public List<Long> getReportIds() {
    List<Long> reportIds = new ArrayList<>();
    for (long i = fromReport; i <= toReport; i++) {
      reportIds.add(i);
    }
    return reportIds;
  }

  public long getReportsCount() {
    return fromReport == 0 ? 0 : (toReport - fromReport + 1);
  }

  public double getEw() {
    long hubsCount = getReportsCount();
    return hubsCount == 0 ? 0d :
                          BigDecimal.valueOf(fixedGlobalIndex)
                                    .divide(BigDecimal.valueOf(hubsCount), MathContext.DECIMAL128)
                                    .doubleValue();
  }

}
