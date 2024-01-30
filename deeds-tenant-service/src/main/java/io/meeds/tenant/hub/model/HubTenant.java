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
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.tenant.hub.model;

import java.time.Instant;
import java.util.Map;

import io.meeds.wom.api.model.Hub;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class HubTenant extends Hub {

  private String womServerUrl;

  private String adminAddress;

  private String womAddress;

  private String uemAddress;

  private long   networkId;

  private long   avatarUpdateTime;

  public HubTenant(long deedId, // NOSONAR
                   short city,
                   short type,
                   String address,
                   Map<String, String> name,
                   Map<String, String> description,
                   String url,
                   String color,
                   String hubOwnerAddress,
                   String deedOwnerAddress,
                   String deedManagerAddress,
                   Instant createdDate,
                   Instant untilDate,
                   Instant joinDate,
                   Instant updatedDate,
                   long usersCount,
                   String rewardsPeriodType,
                   double rewardsPerPeriod,
                   boolean connected,
                   double ownerClaimableAmount,
                   double managerClaimableAmount,
                   String womServerUrl,
                   String adminAddress,
                   String womAddress,
                   String uemAddress,
                   long networkId,
                   long avatarUpdateTime) {
    super(deedId,
          city,
          type,
          address,
          name,
          description,
          url,
          color,
          hubOwnerAddress,
          deedOwnerAddress,
          deedManagerAddress,
          createdDate,
          untilDate,
          joinDate,
          updatedDate,
          usersCount,
          rewardsPeriodType,
          rewardsPerPeriod,
          connected,
          ownerClaimableAmount,
          managerClaimableAmount);
    this.womServerUrl = womServerUrl;
    this.adminAddress = adminAddress;
    this.womAddress = womAddress;
    this.uemAddress = uemAddress;
    this.networkId = networkId;
    this.avatarUpdateTime = avatarUpdateTime;
  }

  @Override
  public HubTenant clone() { // NOSONAR
    return new HubTenant(getDeedId(),
                         getCity(),
                         getType(),
                         getAddress(),
                         getName(),
                         getDescription(),
                         getUrl(),
                         getColor(),
                         getHubOwnerAddress(),
                         getDeedOwnerAddress(),
                         getDeedManagerAddress(),
                         getCreatedDate(),
                         getUntilDate(),
                         getJoinDate(),
                         getUpdatedDate(),
                         getUsersCount(),
                         getRewardsPeriodType(),
                         getRewardsPerPeriod(),
                         isConnected(),
                         getOwnerClaimableAmount(),
                         getManagerClaimableAmount(),
                         womServerUrl,
                         adminAddress,
                         womAddress,
                         uemAddress,
                         networkId,
                         avatarUpdateTime);
  }

}
