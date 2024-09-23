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
package io.meeds.tenant.hub.storage;

import static io.meeds.tenant.hub.storage.HubReportStorage.REWARD_PERIOD_ID_APPLICATION;
import static io.meeds.tenant.hub.storage.HubReportStorage.REWARD_REPORT_ID_APPLICATION;
import static io.meeds.tenant.hub.storage.HubReportStorage.REWARD_REPORT_SENT_DATE_APPLICATION;
import static io.meeds.tenant.hub.storage.HubReportStorage.REWARD_REPORT_STATUS_APPLICATION;
import static io.meeds.tenant.hub.storage.HubReportStorage.UEM_CONTEXT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;

import io.meeds.wallet.reward.service.RewardReportService;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import io.meeds.wallet.wallet.model.reward.RewardPeriod;
import io.meeds.wallet.wallet.model.reward.RewardPeriodType;

@SpringBootTest(classes = {
  HubReportStorage.class,
})
@ExtendWith(MockitoExtension.class)
class HubReportStorageTest {

  @MockBean
  private RewardReportService rewardReportService;

  @MockBean
  private SettingService      settingService;

  @Autowired
  private HubReportStorage    hubReportStorage;

  @Mock
  private RewardPeriod        rewardPeriod;

  private long                periodId = 53l;

  private long                reportId = 356l;

  private Instant             sentDate = Instant.now().minusSeconds(500);

  @Test
  void saveStatus() {
    String status = "NEW";

    hubReportStorage.saveStatus(periodId, status);
    verify(settingService).set(eq(UEM_CONTEXT),
                               eq(REWARD_REPORT_STATUS_APPLICATION),
                               eq(String.valueOf(periodId)),
                               argThat(setting -> StringUtils.equals(status, setting.getValue().toString())));
  }

  @Test
  void saveStatusByPeriodHavingId() {
    String status = "NEW";
    when(rewardPeriod.getId()).thenReturn(periodId);

    hubReportStorage.saveStatus(rewardPeriod, status);
    verify(settingService).set(eq(UEM_CONTEXT),
                               eq(REWARD_REPORT_STATUS_APPLICATION),
                               eq(String.valueOf(periodId)),
                               argThat(setting -> StringUtils.equals(status, setting.getValue().toString())));
  }

  @Test
  void saveStatusByPeriodNotHavingId() {
    String status = "NEW";
    assertThrows(IllegalStateException.class, () -> hubReportStorage.saveStatus(rewardPeriod, status));
  }

  @Test
  void saveStatusByPeriodHavingIdRetrievedFromStore() {
    String status = "NEW";
    RewardPeriodType periodType = RewardPeriodType.WEEK;
    LocalDate periodMedianDate = LocalDate.now();

    when(rewardPeriod.getRewardPeriodType()).thenReturn(periodType);
    when(rewardPeriod.getPeriodMedianDate()).thenReturn(periodMedianDate);
    RewardPeriod rewardPeriodFromStore = mock(RewardPeriod.class);
    when(rewardReportService.getRewardPeriod(periodType, periodMedianDate)).thenReturn(rewardPeriodFromStore);

    when(rewardPeriodFromStore.getId()).thenReturn(periodId);

    hubReportStorage.saveStatus(rewardPeriod, status);
    verify(settingService).set(eq(UEM_CONTEXT),
                               eq(REWARD_REPORT_STATUS_APPLICATION),
                               eq(String.valueOf(periodId)),
                               argThat(setting -> StringUtils.equals(status, setting.getValue().toString())));
  }

  @Test
  void saveReportPeriodId() {
    when(rewardPeriod.getId()).thenReturn(periodId);
    hubReportStorage.saveReportPeriodId(rewardPeriod, reportId);
    verify(settingService).set(eq(UEM_CONTEXT),
                               eq(REWARD_REPORT_ID_APPLICATION),
                               eq(String.valueOf(periodId)),
                               argThat(setting -> StringUtils.equals(String.valueOf(reportId), setting.getValue().toString())));
    verify(settingService).set(eq(UEM_CONTEXT),
                               eq(REWARD_PERIOD_ID_APPLICATION),
                               eq(String.valueOf(reportId)),
                               argThat(setting -> StringUtils.equals(String.valueOf(periodId), setting.getValue().toString())));

  }

  @Test
  void saveSentDate() {
    when(rewardPeriod.getId()).thenReturn(periodId);
    hubReportStorage.saveSentDate(rewardPeriod, sentDate);
    verify(settingService).set(eq(UEM_CONTEXT),
                               eq(REWARD_REPORT_SENT_DATE_APPLICATION),
                               eq(String.valueOf(periodId)),
                               argThat(setting -> StringUtils.equals(String.valueOf(sentDate.toEpochMilli()), setting.getValue().toString())));
  }

  @Test
  void getStatus() {
    String status = "SENT";
    when(rewardPeriod.getId()).thenReturn(periodId);
    assertNull(hubReportStorage.getStatus(rewardPeriod));

    when(settingService.get(UEM_CONTEXT,
                            REWARD_REPORT_STATUS_APPLICATION,
                            String.valueOf(periodId))).thenAnswer(invocation -> SettingValue.create(status));
    assertEquals(status, hubReportStorage.getStatus(rewardPeriod));
  }

  @Test
  void getReportId() {
    when(rewardPeriod.getId()).thenReturn(periodId);
    assertEquals(0, hubReportStorage.getReportId(rewardPeriod));
    
    when(settingService.get(UEM_CONTEXT,
                            REWARD_REPORT_ID_APPLICATION,
                            String.valueOf(periodId))).thenAnswer(invocation -> SettingValue.create(String.valueOf(reportId)));
    assertEquals(reportId, hubReportStorage.getReportId(rewardPeriod));
  }

  @Test
  void getPeriodId() {
    assertEquals(0, hubReportStorage.getPeriodId(reportId));

    when(settingService.get(UEM_CONTEXT,
                            REWARD_PERIOD_ID_APPLICATION,
                            String.valueOf(reportId))).thenAnswer(invocation -> SettingValue.create(String.valueOf(periodId)));
    assertEquals(periodId, hubReportStorage.getPeriodId(reportId));
  }

  @Test
  void getSentDate() {
    when(rewardPeriod.getId()).thenReturn(periodId);
    assertNotNull(hubReportStorage.getSentDate(rewardPeriod));
    assertNotEquals(sentDate.toEpochMilli(), hubReportStorage.getSentDate(rewardPeriod).toEpochMilli());

    when(settingService.get(UEM_CONTEXT,
                            REWARD_REPORT_SENT_DATE_APPLICATION,
                            String.valueOf(periodId))).thenAnswer(invocation -> SettingValue.create(String.valueOf(sentDate.toEpochMilli())));
    assertEquals(sentDate.toEpochMilli(), hubReportStorage.getSentDate(rewardPeriod).toEpochMilli());
  }

}
