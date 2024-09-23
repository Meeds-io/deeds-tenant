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

import static io.meeds.tenant.hub.service.HubReportService.REPORT_SENDING_ERROR_EVENT;
import static io.meeds.tenant.hub.service.HubReportService.REPORT_SENDING_IN_PROGRESS_EVENT;
import static io.meeds.tenant.hub.service.HubReportService.REPORT_SENT_EVENT;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.services.listener.Asynchronous;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;
import io.meeds.wallet.service.WalletWebSocketService;

import io.meeds.common.ContainerTransactional;
import io.meeds.gamification.service.ProgramService;

import jakarta.annotation.PostConstruct;

@Component
@Asynchronous
public class WebSocketReportStatusListener extends Listener<Long, Object> {

  protected static final List<String> EVENT_NAMES = Arrays.asList(REPORT_SENT_EVENT,
                                                                  REPORT_SENDING_IN_PROGRESS_EVENT,
                                                                  REPORT_SENDING_ERROR_EVENT);

  @Autowired
  private WalletWebSocketService      webSocketService;

  @Autowired
  private ProgramService              programService;

  @Autowired
  private ListenerService             listenerService;

  @Override
  @ContainerTransactional
  public void onEvent(Event<Long, Object> event) throws Exception {
    webSocketService.sendMessage(event.getEventName(), programService.getAdministrators(), false, event.getSource());
  }

  @PostConstruct
  public void init() {
    EVENT_NAMES.forEach(eventName -> listenerService.addListener(eventName, this));
  }

}
