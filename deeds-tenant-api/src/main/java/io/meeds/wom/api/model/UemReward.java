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
import java.util.List;
import java.util.Set;

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
public class UemReward {

  /**
   * UEM Blockchain Id
   */
  private long              rewardId;

  /**
   * UEM Blockchain configured amount for current period
   */
  private double            amount;

  /**
   * UEM Blockchain computed fixed Global Index
   */
  private double            fixedGlobalIndex;

  /**
   * UEM Period Start Date
   */
  private Instant           fromDate;

  /**
   * UEM Period End Date
   */
  private Instant           toDate;

  /**
   * UEM Blockchain fromReport -> toReport
   */
  private List<Long>        reportIds;

  private Set<String>       hubAddresses;

  private double            sumEd;

  public long getReportsCount() {
    return reportIds == null ? 0 : reportIds.size();
  }

  public double getEw() {
    return getReportsCount() == 0 ? 1d :
                                  BigDecimal.valueOf(sumEd)
                                            .divide(BigDecimal.valueOf(getReportsCount()),
                                                    MathContext.DECIMAL128)
                                            .doubleValue();
  }

}
