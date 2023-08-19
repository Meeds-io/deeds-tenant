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
package io.meeds.tenant.storage;

import java.time.Instant;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.wallet.model.reward.RewardPeriod;
import org.exoplatform.wallet.reward.service.RewardReportService;

public class HubReportStorage {

  private static final Context WOM_CONTEXT                         = Context.GLOBAL.id("WoM");

  private static final String  REWARD_REPORT_HASH                  = "RewardReportHash";

  private static final String  REWARD_REPORT_STATUS                = "RewardReportStatus";

  private static final String  REWARD_REPORT_SENT_DATE             = "RewardReportSentDate";

  private static final Scope   REWARD_REPORT_STATUS_APPLICATION    = Scope.APPLICATION.id(REWARD_REPORT_STATUS);

  private static final Scope   REWARD_REPORT_SENT_DATE_APPLICATION = Scope.APPLICATION.id(REWARD_REPORT_SENT_DATE);

  private static final Scope   REWARD_REPORT_HASH_APPLICATION      = Scope.APPLICATION.id(REWARD_REPORT_HASH);

  private RewardReportService  rewardReportService;

  private SettingService       settingService;

  public HubReportStorage(RewardReportService rewardReportService,
                          SettingService settingService) {
    // TODO manage this in an entity with database table
    this.rewardReportService = rewardReportService;
    this.settingService = settingService;
  }

  public void saveRewardPeriodStatus(RewardPeriod rewardPeriod, String status) {
    long periodId = getPeriodKey(rewardPeriod);
    saveRewardPeriodStatus(periodId, status);
  }

  public void saveRewardPeriodStatus(long periodId, String status) {
    settingService.set(WOM_CONTEXT,
                       REWARD_REPORT_STATUS_APPLICATION,
                       String.valueOf(periodId),
                       SettingValue.create(status));
  }

  public void saveRewardPeriodHash(RewardPeriod rewardPeriod, String hash) {
    long periodId = getPeriodKey(rewardPeriod);
    saveRewardPeriodHash(periodId, hash);
  }

  public void saveRewardPeriodHash(long periodId, String hash) {
    settingService.set(WOM_CONTEXT,
                       REWARD_REPORT_HASH_APPLICATION,
                       String.valueOf(periodId),
                       SettingValue.create(hash));
  }

  public void saveRewardPeriodSentDate(RewardPeriod rewardPeriod, Instant sentDate) {
    long periodId = getPeriodKey(rewardPeriod);
    settingService.set(WOM_CONTEXT,
                       REWARD_REPORT_SENT_DATE_APPLICATION,
                       String.valueOf(periodId),
                       SettingValue.create(String.valueOf(sentDate.toEpochMilli())));
  }

  public String getRewardPeriodStatus(RewardPeriod rewardPeriod) {
    long periodId = getPeriodKey(rewardPeriod);
    SettingValue<?> statusValue = settingService.get(WOM_CONTEXT,
                                                     REWARD_REPORT_STATUS_APPLICATION,
                                                     String.valueOf(periodId));
    return statusValue == null || statusValue.getValue() == null ? null : statusValue.getValue().toString();
  }

  public String getRewardPeriodHash(RewardPeriod rewardPeriod) {
    long periodId = getPeriodKey(rewardPeriod);
    return getRewardPeriodHash(periodId);
  }

  public String getRewardPeriodHash(long periodId) {
    SettingValue<?> settingValue = settingService.get(WOM_CONTEXT,
                                                      REWARD_REPORT_HASH_APPLICATION,
                                                      String.valueOf(periodId));
    return settingValue == null || settingValue.getValue() == null ? null : settingValue.getValue().toString();
  }

  public Instant getRewardPeriodSentDate(RewardPeriod rewardPeriod) {
    long periodId = getPeriodKey(rewardPeriod);
    return getRewardPeriodSentDate(periodId);
  }

  public Instant getRewardPeriodSentDate(long periodId) {
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

  public void cleanRewardPeriodSentDate(long periodId) {
    settingService.remove(WOM_CONTEXT,
                          REWARD_REPORT_SENT_DATE_APPLICATION,
                          String.valueOf(periodId));
  }

  public void cleanRewardPeriodStatus(long periodId) {
    settingService.remove(WOM_CONTEXT,
                          REWARD_REPORT_STATUS_APPLICATION,
                          String.valueOf(periodId));
  }

  public void cleanRewardPeriodHash(long periodId) {
    settingService.remove(WOM_CONTEXT,
                          REWARD_REPORT_HASH_APPLICATION,
                          String.valueOf(periodId));
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
