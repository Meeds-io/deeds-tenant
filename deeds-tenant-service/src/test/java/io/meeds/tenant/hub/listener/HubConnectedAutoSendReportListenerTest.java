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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.ListenerService;

import io.meeds.tenant.hub.service.HubReportService;
import io.meeds.wallet.model.RewardPeriod;
import io.meeds.wallet.model.RewardReport;
import io.meeds.wallet.reward.service.RewardReportService;
import io.meeds.wom.api.constant.WomException;
import io.meeds.wom.api.model.Hub;

@SpringBootTest(classes = { HubConnectedAutoSendReportListener.class, })
@ExtendWith(MockitoExtension.class)
public class HubConnectedAutoSendReportListenerTest {

  @MockBean
  private RewardReportService                rewardReportService;

  @MockBean
  private HubReportService                   hubReportService;

  @MockBean
  private ListenerService                    listenerService;

  @Mock
  private Hub                                hub;

  @Mock
  private RewardReport                       rewardReport;

  @Mock
  private RewardPeriod                       period;

  @Mock
  private Event<Hub, Object>                 event;

  @Autowired
  private HubConnectedAutoSendReportListener listener;

  private long                               periodId = 3l;

  @Test
  public void autoSendLastReportOnEvent() throws WomException {
    when(event.getSource()).thenReturn(hub);
    when(hub.isConnected()).thenReturn(true);
    when(hub.getJoinDate()).thenReturn(Instant.now());
    when(rewardReportService.getRewardReport(any())).thenReturn(rewardReport);
    when(rewardReport.isCompletelyProcessed()).thenReturn(true);
    when(rewardReport.getPeriod()).thenReturn(period);
    when(period.getId()).thenReturn(periodId);
    when(hubReportService.getReportId(periodId)).thenReturn(0l);

    listener.onEvent(event);

    verify(hubReportService).sendReport(periodId);
  }

  @Test
  public void avoidAutoSendWhenNoLastReport() {
    when(event.getSource()).thenReturn(hub);
    when(hub.isConnected()).thenReturn(true);
    when(hub.getJoinDate()).thenReturn(Instant.now());

    listener.onEvent(event);
    verifyNoInteractions(hubReportService);
  }

  @Test
  public void avoidAutoSendLastReportWhenNotCompleted() {
    when(event.getSource()).thenReturn(hub);
    when(hub.isConnected()).thenReturn(true);
    when(hub.getJoinDate()).thenReturn(Instant.now());
    when(rewardReportService.getRewardReport(any())).thenReturn(rewardReport);

    listener.onEvent(event);

    verifyNoInteractions(hubReportService);
  }

  @Test
  public void avoidAutoSendLastReportWhenAlreadySent() throws WomException {
    when(event.getSource()).thenReturn(hub);
    when(hub.isConnected()).thenReturn(true);
    when(hub.getJoinDate()).thenReturn(Instant.now());
    when(rewardReportService.getRewardReport(any())).thenReturn(rewardReport);
    when(rewardReport.isCompletelyProcessed()).thenReturn(true);
    when(rewardReport.getPeriod()).thenReturn(period);
    when(period.getId()).thenReturn(periodId);
    when(hubReportService.getReportId(periodId)).thenReturn(2l);

    listener.onEvent(event);

    verify(hubReportService, never()).sendReport(anyLong());
  }

  @Test
  public void avoidAutoSendLastReportWhenJoinDateIsBeforeOneHour() {
    when(event.getSource()).thenReturn(hub);
    when(hub.isConnected()).thenReturn(true);
    when(hub.getJoinDate()).thenReturn(Instant.now().minusSeconds(7200l));

    listener.onEvent(event);

    verifyNoInteractions(hubReportService);
    verifyNoInteractions(rewardReportService);
  }

  @Test
  public void avoidAutoSendLastReportWhenJoinDateIsNull() {
    when(event.getSource()).thenReturn(hub);
    when(hub.isConnected()).thenReturn(true);

    listener.onEvent(event);

    verify(hub).isConnected();
    verifyNoInteractions(hubReportService);
  }

  @Test
  public void avoidAutoSendLastReportWhenNotConnected() {
    when(event.getSource()).thenReturn(hub);

    listener.onEvent(event);

    verify(hub, never()).getJoinDate();
    verifyNoInteractions(hubReportService);
  }

}
