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
    // Use @AlArgsConstructor to detect any change on model by having a
    // compilation error when the list of attributes changes
    HubReport reportTmp = new HubReport(report.getHash(),
                                        report.getSignature(),
                                        report.getEarnerAddress(),
                                        report.getDeedManagerAddress(),
                                        report.getStatus(),
                                        report.getError(),
                                        report.getSentDate(),
                                        report.getUemRewardIndex(),
                                        report.getUemRewardAmount(),
                                        report.getLastPeriodUemRewardAmount(),
                                        report.getLastPeriodUemDiff(),
                                        report.getHubRewardAmountPerPeriod(),
                                        report.getHubRewardLastPeriodDiff(),
                                        report.getLastPeriodUemRewardAmountPerPeriod(),
                                        report.getMp(),
                                        report.getRewardId(),
                                        report.getRewardHash());

    this.setHash(reportTmp.getHash());
    this.setSignature(reportTmp.getSignature());
    this.setEarnerAddress(reportTmp.getEarnerAddress());
    this.setDeedManagerAddress(reportTmp.getDeedManagerAddress());
    this.setStatus(reportTmp.getStatus());
    this.setError(reportTmp.getError());
    this.setSentDate(reportTmp.getSentDate());
    this.setUemRewardIndex(reportTmp.getUemRewardIndex());
    this.setUemRewardAmount(reportTmp.getUemRewardAmount());
    this.setLastPeriodUemRewardAmount(getLastPeriodUemRewardAmount());
    this.setLastPeriodUemDiff(getLastPeriodUemDiff());
    this.setHubRewardAmountPerPeriod(getHubRewardAmountPerPeriod());
    this.setHubRewardLastPeriodDiff(getHubRewardLastPeriodDiff());
    this.setLastPeriodUemRewardAmountPerPeriod(getLastPeriodUemRewardAmountPerPeriod());
    this.setMp(getMp());
    this.setRewardId(reportTmp.getRewardId());
    this.setRewardHash(reportTmp.getRewardHash());

    this.id = id;
    this.canRefresh = canRefresh;
    this.canSend = canSend;
  }

}
