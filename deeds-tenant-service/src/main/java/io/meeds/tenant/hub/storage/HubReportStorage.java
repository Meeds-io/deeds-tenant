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
package io.meeds.tenant.hub.storage;

import java.time.Instant;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.wallet.model.reward.RewardPeriod;
import org.exoplatform.wallet.reward.service.RewardReportService;

@Component
public class HubReportStorage {

  private static final Context WOM_CONTEXT                         = Context.GLOBAL.id("WoM");

  private static final String  REWARD_REPORT_ID                    = "RewardReportId";

  private static final String  REWARD_PERIOD_ID                    = "RewardPeriodId";

  private static final String  REWARD_REPORT_STATUS                = "RewardReportStatus";

  private static final String  REWARD_REPORT_SENT_DATE             = "RewardReportSentDate";

  private static final Scope   REWARD_REPORT_STATUS_APPLICATION    = Scope.APPLICATION.id(REWARD_REPORT_STATUS);

  private static final Scope   REWARD_REPORT_SENT_DATE_APPLICATION = Scope.APPLICATION.id(REWARD_REPORT_SENT_DATE);

  private static final Scope   REWARD_REPORT_ID_APPLICATION        = Scope.APPLICATION.id(REWARD_REPORT_ID);

  private static final Scope   REWARD_PERIOD_ID_APPLICATION        = Scope.APPLICATION.id(REWARD_PERIOD_ID);

  @Autowired
  private RewardReportService  rewardReportService;

  @Autowired
  private SettingService       settingService;

  public void saveStatus(RewardPeriod rewardPeriod, String status) {
    long periodId = getPeriodKey(rewardPeriod);
    saveStatus(periodId, status);
  }

  public void saveStatus(long periodId, String status) {
    settingService.set(WOM_CONTEXT,
                       REWARD_REPORT_STATUS_APPLICATION,
                       String.valueOf(periodId),
                       SettingValue.create(status));
  }

  public void saveReportPeriodId(RewardPeriod rewardPeriod, long reportId) {
    long periodId = getPeriodKey(rewardPeriod);
    saveReportPeriodId(periodId, reportId);
  }

  public void saveReportPeriodId(long periodId, long reportId) {
    settingService.set(WOM_CONTEXT,
                       REWARD_REPORT_ID_APPLICATION,
                       String.valueOf(periodId),
                       SettingValue.create(String.valueOf(reportId)));
    settingService.set(WOM_CONTEXT,
                       REWARD_PERIOD_ID_APPLICATION,
                       String.valueOf(reportId),
                       SettingValue.create(String.valueOf(periodId)));
  }

  public void saveSentDate(RewardPeriod rewardPeriod, Instant sentDate) {
    long periodId = getPeriodKey(rewardPeriod);
    settingService.set(WOM_CONTEXT,
                       REWARD_REPORT_SENT_DATE_APPLICATION,
                       String.valueOf(periodId),
                       SettingValue.create(String.valueOf(sentDate.toEpochMilli())));
  }

  public String getStatus(RewardPeriod rewardPeriod) {
    long periodId = getPeriodKey(rewardPeriod);
    SettingValue<?> statusValue = settingService.get(WOM_CONTEXT,
                                                     REWARD_REPORT_STATUS_APPLICATION,
                                                     String.valueOf(periodId));
    return statusValue == null || statusValue.getValue() == null ? null : statusValue.getValue().toString();
  }

  public long getReportId(RewardPeriod rewardPeriod) {
    long periodId = getPeriodKey(rewardPeriod);
    return getReportId(periodId);
  }

  public long getReportId(long periodId) {
    SettingValue<?> settingValue = settingService.get(WOM_CONTEXT,
                                                      REWARD_REPORT_ID_APPLICATION,
                                                      String.valueOf(periodId));
    return settingValue == null || settingValue.getValue() == null ? 0 : Long.parseLong(settingValue.getValue().toString());
  }

  public long getPeriodId(long reportId) {
    SettingValue<?> settingValue = settingService.get(WOM_CONTEXT,
                                                      REWARD_PERIOD_ID_APPLICATION,
                                                      String.valueOf(reportId));
    return settingValue == null || settingValue.getValue() == null ? 0 : Long.parseLong(settingValue.getValue().toString());
  }

  public Instant getSentDate(RewardPeriod rewardPeriod) {
    long periodId = getPeriodKey(rewardPeriod);
    return getSentDate(periodId);
  }

  public Instant getSentDate(long periodId) {
    SettingValue<?> settingValue = settingService.get(WOM_CONTEXT,
                                                      REWARD_REPORT_SENT_DATE_APPLICATION,
                                                      String.valueOf(periodId));
    String sentDateString = settingValue == null || settingValue.getValue() == null ? null : settingValue.getValue().toString();
    if (StringUtils.isBlank(sentDateString)) {
      return Instant.now();
    } else {
      return Instant.ofEpochMilli(Long.parseLong(sentDateString));
    }
  }

  public long getPeriodKey(RewardPeriod rewardPeriod) {
    if (rewardPeriod.getId() > 0) {
      return rewardPeriod.getId();
    } else {
      rewardPeriod = rewardReportService.getRewardPeriod(rewardPeriod.getRewardPeriodType(), rewardPeriod.getPeriodMedianDate());
      if (rewardPeriod == null || rewardPeriod.getId() == 0) {
        throw new IllegalStateException("Selected Rewrd period, doesn't have a matching id: " + rewardPeriod);
      } else {
        return rewardPeriod.getId();
      }
    }
  }

}
