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

import io.meeds.deeds.constant.HubRewardReportStatusType;
import io.meeds.deeds.model.HubRewardReport;
import io.meeds.deeds.model.HubRewardReportStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@JsonInclude(value = Include.NON_EMPTY)
public class HubRewardReportLocalStatus extends HubRewardReportStatus {

  private long    id;

  private boolean canRefresh;

  private boolean canSend;

  public HubRewardReportLocalStatus(long id,
                                    String hash,
                                    HubRewardReport hubRewardReport,
                                    HubRewardReportStatusType status,
                                    String error,
                                    boolean canRefresh,
                                    boolean canSend) {
    super(hash, hubRewardReport, status, error);
    this.id = id;
    this.canRefresh = canRefresh;
    this.canSend = canSend;
  }

  public HubRewardReportLocalStatus(HubRewardReportStatus report,
                                    long id,
                                    boolean canRefresh,
                                    boolean canSend) {
    super(report.getHash(),
          report.getHubRewardReport(),
          report.getStatus(),
          report.getError(),
          report.getRewardPayment());
    this.id = id;
    this.canRefresh = canRefresh;
    this.canSend = canSend;
  }

}
