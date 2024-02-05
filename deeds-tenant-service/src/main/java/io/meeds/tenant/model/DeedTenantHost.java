/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 - 2022 Meeds Association contact@meeds.io
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

import lombok.Data;

@Data
public class DeedTenantHost {

  private static DeedTenantHost instance;

  private long                  nftId;

  private short                 cityIndex = -1;

  private short                 cardType  = -1;

  private boolean               provisioned;

  private String                managerAddress;

  private String                managerEmail;

  private DeedTenantHost(long nftId,
                         short cityIndex,
                         short cardType,
                         boolean provisioned,
                         String managerAddress,
                         String managerEmail) {
    this.nftId = nftId;
    this.cityIndex = cityIndex;
    this.cardType = cardType;
    this.provisioned = provisioned;
    this.managerAddress = managerAddress;
    this.managerEmail = managerEmail;
  }

  public static DeedTenantHost getInstance() {
    return instance;
  }

  public static DeedTenantHost setInstance(long nftId,
                                           short cityIndex,
                                           short cardType,
                                           boolean provisioned,
                                           String managerAddress,
                                           String managerEmail) {
    instance = new DeedTenantHost(nftId, cityIndex, cardType, provisioned, managerAddress, managerEmail);
    return instance;
  }

  public static void clear() {
    instance = null;
  }

}