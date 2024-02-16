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
package io.meeds.tenant.hub.listener;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.services.listener.Asynchronous;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wallet.model.reward.RewardReport;
import org.exoplatform.wallet.reward.service.RewardReportService;

import io.meeds.common.ContainerTransactional;
import io.meeds.tenant.hub.service.HubReportService;
import io.meeds.tenant.hub.service.HubService;
import io.meeds.wom.api.constant.WomException;
import io.meeds.wom.api.model.Hub;

import jakarta.annotation.PostConstruct;

/**
 * This listener will be triggered when the Hub is connected to the WoM. Once
 * connected, the listener will send automatically last elligible reward report.
 */
@Component
@Asynchronous
public class HubConnectedAutoSendReportListener extends Listener<Hub, Object> {

  private static final Log    LOG = ExoLogger.getLogger(HubConnectedAutoSendReportListener.class);

  @Autowired
  private RewardReportService rewardReportService;

  @Autowired
  private HubReportService    hubReportService;

  @Autowired
  private ListenerService     listenerService;

  @Override
  @ContainerTransactional
  public void onEvent(Event<Hub, Object> event) throws Exception { // NOSONAR
    Hub hub = event.getSource();
    if (hub.isConnected() && hub.getJoinDate() != null && Instant.now().minusSeconds(hub.getJoinDate().getEpochSecond()).getEpochSecond() < 3600l) {
      RewardReport rewardReport = rewardReportService.getRewardReport(LocalDate.ofInstant(hub.getJoinDate(), ZoneOffset.UTC)
                                                                               .minusWeeks(1));
      if (rewardReport != null && rewardReport.isCompletelyProceeded()) {
        long periodId = rewardReport.getPeriod().getId();
        if (hubReportService.getReportId(periodId) == 0) {
          int retries = 3;
          while (retries-- > 0) {
            try {
              hubReportService.sendReport(periodId);
              retries = 0;
            } catch (WomException e) {
              if (retries == 0) {
                LOG.error("Error sending automatically the previous period Hub report", e);
              } else {
                LOG.warn("Error sending automatically the previous period Hub report. Automatically retry.");
              }
            }
          }
        }
      }
    }
  }

  @PostConstruct
  public void init() {
    listenerService.addListener(HubService.HUB_CONNECTED_EVENT, this);
  }

}
