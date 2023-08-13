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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.meeds.deeds.api.model.HubReport;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@JsonInclude(value = Include.NON_EMPTY)
public class HubReportLocalStatus extends HubReport {

  private long    id;

  private boolean canRefresh;

  private boolean canSend;

  public HubReportLocalStatus(HubReport report,
                              long id,
                              boolean canRefresh,
                              boolean canSend) {
    super(report);
    this.setHash(report.getHash());
    this.setSignature(report.getSignature());
    this.setEarnerAddress(report.getEarnerAddress());
    this.setDeedManagerAddress(report.getDeedManagerAddress());
    this.setStatus(report.getStatus());
    this.setError(report.getError());
    this.setSentDate(report.getSentDate());
    this.setUemRewardIndex(report.getUemRewardIndex());
    this.setUemRewardAmount(report.getUemRewardAmount());
    this.setLastPeriodUemRewardAmount(report.getLastPeriodUemRewardAmount());
    this.setRewardId(report.getRewardId());
    this.setRewardHash(report.getRewardHash());

    this.id = id;
    this.canRefresh = canRefresh;
    this.canSend = canSend;
  }

}
