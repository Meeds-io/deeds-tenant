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
package io.meeds.tenant.model;

import org.exoplatform.wallet.model.reward.RewardPeriodType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HubStatus {

  private long             deedId = -1;

  private short            city;

  private short            type;

  private String           address;

  private String           name;

  private String           description;

  private String           url;

  private String           logoUrl;

  private String           color;

  private String           deedManagerAddress;

  private String           earnerAddress;

  private double           rewardsAmount;

  private RewardPeriodType rewardsPeriod;

  private long             usersCount;

  public HubStatus(long deedId, // NOSONAR
                   short city,
                   short type,
                   String address,
                   String name,
                   String description,
                   String url,
                   String logoUrl,
                   String color,
                   String deedManagerAddress,
                   String earnerAddress) {
    this(deedId,
         city,
         type,
         address,
         name,
         description,
         url,
         logoUrl,
         color,
         deedManagerAddress,
         earnerAddress,
         0,
         null,
         0);
  }

}
