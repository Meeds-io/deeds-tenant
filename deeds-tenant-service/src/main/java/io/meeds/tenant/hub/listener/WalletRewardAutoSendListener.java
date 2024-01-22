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
package io.meeds.tenant.hub.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.services.listener.Asynchronous;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.wallet.model.reward.RewardReport;

import io.meeds.common.ContainerTransactional;
import io.meeds.tenant.hub.service.HubReportService;

import jakarta.annotation.PostConstruct;

@Asynchronous
@Component
public class WalletRewardAutoSendListener extends Listener<RewardReport, Object> {

  private static final String EVENT_NAME = "exo.wallet.reward.report.success";

  @Autowired
  private HubReportService    reportService;

  @Autowired
  private ListenerService     listenerService;

  @Override
  @ContainerTransactional
  public void onEvent(Event<RewardReport, Object> event) throws Exception {
    RewardReport rewardReport = event.getSource();
    reportService.sendReport(rewardReport);
  }

  @PostConstruct
  public void init() {
    listenerService.addListener(EVENT_NAME, this);
  }

}
