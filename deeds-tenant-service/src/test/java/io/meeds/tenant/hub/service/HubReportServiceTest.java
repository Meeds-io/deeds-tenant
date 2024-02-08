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
package io.meeds.tenant.hub.service;

import static io.meeds.tenant.hub.service.HubReportService.REPORT_PERSISTED_EVENT;
import static io.meeds.tenant.hub.service.HubReportService.REPORT_SENDING_ERROR_EVENT;
import static io.meeds.tenant.hub.service.HubReportService.REPORT_SENDING_IN_PROGRESS_EVENT;
import static io.meeds.tenant.hub.service.HubReportService.REPORT_SENT_EVENT;
import static io.meeds.tenant.hub.utils.EntityMapper.toHubLocalReport;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.web3j.crypto.Hash;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.organization.UserStatus;
import org.exoplatform.wallet.model.reward.RewardPeriod;
import org.exoplatform.wallet.model.reward.RewardPeriodType;
import org.exoplatform.wallet.model.reward.RewardReport;
import org.exoplatform.wallet.model.reward.WalletReward;
import org.exoplatform.wallet.model.transaction.TransactionDetail;
import org.exoplatform.wallet.reward.service.RewardReportService;
import org.exoplatform.wallet.utils.WalletUtils;

import io.meeds.gamification.constant.IdentityType;
import io.meeds.gamification.constant.RealizationStatus;
import io.meeds.gamification.service.RealizationService;
import io.meeds.tenant.hub.constant.HubReportStatusType;
import io.meeds.tenant.hub.model.HubReportLocalStatus;
import io.meeds.tenant.hub.model.HubTenant;
import io.meeds.tenant.hub.rest.client.WomClientService;
import io.meeds.tenant.hub.storage.HubReportStorage;
import io.meeds.tenant.hub.storage.HubWalletStorage;
import io.meeds.wom.api.constant.WomException;
import io.meeds.wom.api.model.HubReport;
import io.meeds.wom.api.model.HubReportPayload;
import io.meeds.wom.api.model.HubReportVerifiableData;

@SpringBootTest(classes = {
                            HubReportService.class,
})
@ExtendWith(MockitoExtension.class)
class HubReportServiceTest {

  @MockBean
  private OrganizationService       organizationService;

  @MockBean
  private RewardReportService       rewardReportService;

  @MockBean
  private RealizationService        realizationService;

  @MockBean
  private HubService                hubService;

  @MockBean
  private WomClientService          womServiceClient;

  @MockBean
  private HubWalletStorage          hubWalletStorage;

  @MockBean
  private HubReportStorage          hubReportStorage;

  @MockBean
  private ListenerService           listenerService;

  @Mock
  private RewardReport              rewardReport;

  @Mock
  private RewardPeriod              rewardPeriod;

  @Mock
  private TransactionDetail         transaction;

  @Mock
  private HubTenant                 hub;

  @Mock
  private UserHandler               userHandler;

  @Mock
  private ListAccess<User>          listAccess;

  @Autowired
  private HubReportService          hubReportService;

  private long                      periodStartTime           = ZonedDateTime.now()
                                                                             .with(DayOfWeek.MONDAY)
                                                                             .minusWeeks(3)
                                                                             .toLocalDate()
                                                                             .atStartOfDay(ZoneOffset.UTC)
                                                                             .toEpochSecond();

  private long                      periodEndTime             = ZonedDateTime.now()
                                                                             .with(DayOfWeek.MONDAY)
                                                                             .minusWeeks(2)
                                                                             .toLocalDate()
                                                                             .atStartOfDay(ZoneOffset.UTC)
                                                                             .toEpochSecond();

  private long                      reportId                  = 5l;

  private long                      rewardId                  = 7l;

  private short                     city                      = 1;

  private short                     cardType                  = 3;

  private short                     mintingPower              = 120;

  private long                      maxUsers                  = Long.MAX_VALUE;

  private int                       ownerMintingPercentage    = 60;

  private double                    fixedRewardIndex          = 0.005446d;

  private double                    ownerFixedIndex           = 0.0032676d;

  private double                    tenantFixedIndex          = 0.0021784d;

  private double                    lastPeriodUemRewardAmount = 84d;

  private double                    uemRewardAmount           = 90d;

  private Instant                   updatedDate               = Instant.now();

  private boolean                   fraud                     = false;

  private long                      periodId                  = 2l;

  private long                      deedId                    = 35l;

  private long                      usersCount                = 125l;

  private long                      participantsCount         = 85l;

  private long                      recipientsCount           = 65l;

  private int                       achievementsCount         = 55698;

  private double                    tokensSent                = 52.3365d;

  private String                    tokenAddress              = "0x334d85047da64738c065d36e10b2adeb965000d0";

  private long                      tokenNetworkId            = 1l;

  private long                      networkId                 = 80001l;

  private String                    uemAddress                = "0x290b11b1ab6a31ff95490e4e0eeffec6402cce99";

  private String                    hubAddress                = "0x290b11b1ab6a31ff95490e4e0eeffec6402cce99";

  private String                    deedManagerAddress        = "0x609a6f01b7976439603356e41d5456b42df957b7";

  private String                    ownerAddress              = "0x27d282d1e7e790df596f50a234602d9e761d22aa";

  private String                    txHash                    =
                                           "0xef4e9db309b5dd7020ce463ae726b4d0759e1de0635661de91d8d98e83ae2862";

  private Instant                   sentDate                  = Instant.now();

  private MockedStatic<WalletUtils> walletUtils;

  @BeforeEach
  void init() {
    walletUtils = mockStatic(WalletUtils.class);
    walletUtils.when(WalletUtils::getNetworkId).thenReturn(tokenNetworkId);
    walletUtils.when(WalletUtils::getContractAddress).thenReturn(tokenAddress);
  }

  @AfterEach
  void close() {
    walletUtils.close();
  }

  @Test
  void sendReport() throws Exception {
    HubReportLocalStatus report = hubReportService.sendReport(periodId);
    assertNull(report, "Shouldn't send report when Hub reward not found");

    when(rewardReportService.getRewardReportByPeriodId(periodId)).thenReturn(rewardReport);
    when(rewardReport.getPeriod()).thenReturn(rewardPeriod);

    report = hubReportService.sendReport(periodId);
    assertNull(report, "Shouldn't send report when not connected yet");

    when(hubService.isConnected()).thenReturn(true);
    when(hubService.getHub(true)).thenReturn(hub);
    when(hubService.getHubAddress()).thenReturn(hubAddress);
    lenient().when(hub.getAddress()).thenReturn(hubAddress);
    when(hub.getUemAddress()).thenReturn(uemAddress);
    when(hub.getNetworkId()).thenReturn(networkId);
    when(hubService.getDeedId()).thenReturn(deedId);

    when(organizationService.getUserHandler()).thenReturn(userHandler);
    when(userHandler.findAllUsers(UserStatus.ENABLED)).thenReturn(listAccess);
    when(listAccess.getSize()).thenReturn((int) usersCount);

    when(hubReportStorage.getSentDate(rewardPeriod)).thenReturn(sentDate);

    report = hubReportService.sendReport(periodId);
    assertNull(report, "Shouldn't send report when not completely processed yet");

    when(rewardReport.isCompletelyProceeded()).thenReturn(true);
    when(rewardReport.getValidRewardCount()).thenReturn(recipientsCount);
    when(rewardReport.getTokensSent()).thenReturn(tokensSent);
    when(rewardReport.getValidRewards()).thenReturn(Collections.singleton(new WalletReward(null,
                                                                                           null,
                                                                                           transaction,
                                                                                           null,
                                                                                           null)));

    when(rewardPeriod.getId()).thenReturn(periodId);
    when(rewardPeriod.getStartDateInSeconds()).thenReturn(periodStartTime);
    when(rewardPeriod.getEndDateInSeconds()).thenReturn(periodEndTime);
    when(rewardPeriod.getRewardPeriodType()).thenReturn(RewardPeriodType.WEEK);

    when(transaction.getHash()).thenReturn(txHash);
    when(transaction.isSucceeded()).thenReturn(true);
    when(realizationService.countParticipantsBetweenDates(any(), any())).thenReturn(participantsCount);
    when(realizationService.countRealizationsByFilter(argThat(filter -> filter != null
                                                                        && filter.getEarnerType() == IdentityType.USER
                                                                        && filter.getStatus() == RealizationStatus.ACCEPTED
                                                                        && filter.getFromDate().getTime() / 1000
                                                                            == periodStartTime
                                                                        && filter.getToDate().getTime() / 1000 == periodEndTime)))
                                                                                                                                  .thenReturn(achievementsCount);

    when(hubWalletStorage.sendReportTransaction(any(), any(), anyLong())).thenReturn(reportId);

    HubReport hubReport = newHubReport();
    when(womServiceClient.saveReport(any())).thenReturn(hubReport);
    String signature = "0x22235879963145";
    String hash = StringUtils.lowerCase(Hash.sha3(signature));
    when(hubWalletStorage.signHubMessage(any())).thenReturn(signature);
    report = hubReportService.sendReport(periodId);
    assertNotNull(report, "Shouldn't send report when not completely processed yet");

    verify(hubWalletStorage).sendReportTransaction(newHubReportPayload(),
                                                   uemAddress,
                                                   networkId);
    verify(hubReportStorage).saveStatus(rewardPeriod, HubReportStatusType.SENDING.name());
    verify(listenerService).broadcast(REPORT_SENDING_IN_PROGRESS_EVENT, rewardPeriod.getId(), null);

    verify(hubReportStorage).saveReportPeriodId(rewardPeriod, reportId);
    verify(hubReportStorage).saveStatus(rewardPeriod, HubReportStatusType.SENT.name());
    verify(hubReportStorage).saveSentDate(eq(rewardPeriod), any());
    verify(listenerService).broadcast(REPORT_SENT_EVENT, rewardPeriod.getId(), reportId);
    verify(listenerService, never()).broadcast(REPORT_SENDING_ERROR_EVENT, rewardPeriod.getId(), null);

    verify(womServiceClient).saveReport(new HubReportVerifiableData(hash,
                                                                    signature,
                                                                    hubReport));
    verify(listenerService).broadcast(REPORT_PERSISTED_EVENT, reportId, null);

    when(hubWalletStorage.sendReportTransaction(any(), any(), anyLong())).thenThrow(IllegalStateException.class);
    assertThrows(IllegalStateException.class, () -> hubReportService.sendReport(periodId));
    verify(listenerService).broadcast(REPORT_SENDING_ERROR_EVENT, rewardPeriod.getId(), null);
  }

  @Test
  void getReportWithRefreshWhenNoReportId() {
    assertThrows(WomException.class, () -> hubReportService.getReport(periodId, true));
  }

  @Test
  void getReportWithRefreshWhenReportIdExistsButNotOnWom()  {
    when(hubReportStorage.getReportId(periodId)).thenReturn(reportId);
    assertThrows(WomException.class, () -> hubReportService.getReport(periodId, true));
  }

  @Test
  void getReportWithRefreshWhenReportIdExists() throws WomException {
    when(hubReportStorage.getReportId(periodId)).thenReturn(reportId);
    when(womServiceClient.retrieveReport(reportId)).thenAnswer(invocation -> newHubReport());

    when(rewardReportService.getRewardReportByPeriodId(periodId)).thenReturn(rewardReport);
    when(hubService.isConnected()).thenReturn(true);
    when(hubService.getHub()).thenReturn(hub);
    when(hubService.getHubAddress()).thenReturn(hubAddress);
    when(hubService.getDeedId()).thenReturn(deedId);
    when(organizationService.getUserHandler()).thenReturn(userHandler);
    when(hubReportStorage.getSentDate(rewardPeriod)).thenReturn(sentDate);
    when(transaction.getHash()).thenReturn(txHash);
    when(realizationService.countParticipantsBetweenDates(any(), any())).thenReturn(participantsCount);
    when(realizationService.countRealizationsByFilter(argThat(filter -> filter != null
                                                                        && filter.getEarnerType() == IdentityType.USER
                                                                        && filter.getStatus() == RealizationStatus.ACCEPTED
                                                                        && filter.getFromDate().getTime() / 1000
                                                                            == periodStartTime
                                                                        && filter.getToDate().getTime() / 1000 == periodEndTime)))
                                                                                                                                  .thenReturn(achievementsCount);

    HubReportLocalStatus report = hubReportService.getReport(periodId, true);
    assertEquals(toHubLocalReport(newHubReport(),
                                  periodId,
                                  reportId,
                                  HubReportStatusType.SENT.isCanRefresh(),
                                  HubReportStatusType.SENT.isCanSend(),
                                  HubReportStatusType.SENT,
                                  null), report);
  }

  @Test
  void getReportWithoutRefreshWhenNoReward() {
    assertThrows(WomException.class, () -> hubReportService.getReport(periodId, false));
  }

  @Test
  void getReportNotSentToWomWithLocalRewardReport() throws Exception {
    when(rewardReportService.getRewardReportByPeriodId(periodId)).thenReturn(rewardReport);
    when(rewardReport.getPeriod()).thenReturn(rewardPeriod);
    when(hubService.isConnected()).thenReturn(true);
    when(hubService.getHub()).thenReturn(hub);
    when(hubService.getHubAddress()).thenReturn(hubAddress);
    lenient().when(hub.getAddress()).thenReturn(hubAddress);
    when(hubService.getDeedId()).thenReturn(deedId);
    when(organizationService.getUserHandler()).thenReturn(userHandler);
    when(userHandler.findAllUsers(UserStatus.ENABLED)).thenReturn(listAccess);
    when(listAccess.getSize()).thenReturn((int) usersCount);
    when(hubReportStorage.getSentDate(rewardPeriod)).thenReturn(sentDate);
    when(rewardReport.getValidRewardCount()).thenReturn(recipientsCount);
    when(rewardReport.getTokensSent()).thenReturn(tokensSent);
    when(rewardReport.getValidRewards()).thenReturn(Collections.singleton(new WalletReward(null,
                                                                                           null,
                                                                                           transaction,
                                                                                           null,
                                                                                           null)));

    lenient().when(rewardPeriod.getId()).thenReturn(periodId);
    when(rewardPeriod.getStartDateInSeconds()).thenReturn(periodStartTime);
    when(rewardPeriod.getEndDateInSeconds()).thenReturn(periodEndTime);
    when(rewardPeriod.getRewardPeriodType()).thenReturn(RewardPeriodType.WEEK);

    when(transaction.getHash()).thenReturn(txHash);
    when(transaction.isSucceeded()).thenReturn(true);
    when(realizationService.countParticipantsBetweenDates(any(), any())).thenReturn(participantsCount);
    when(realizationService.countRealizationsByFilter(argThat(filter -> filter != null
                                                                        && filter.getEarnerType() == IdentityType.USER
                                                                        && filter.getStatus() == RealizationStatus.ACCEPTED
                                                                        && filter.getFromDate().getTime() / 1000
                                                                            == periodStartTime
                                                                        && filter.getToDate().getTime() / 1000 == periodEndTime)))
                                                                                                                                  .thenReturn(achievementsCount);

    HubReport hubReport = newHubReport();
    hubReport.setDeedId(-1); // Invalid, thus no deedId

    HubReportLocalStatus report = hubReportService.getReport(periodId, false);
    assertEquals(toHubLocalReport(hubReport,
                                  0,
                                  0,
                                  false,
                                  false,
                                  HubReportStatusType.INVALID,
                                  null), report);

    when(hubReportStorage.getStatus(rewardPeriod)).thenReturn("SENT");
    when(hubReportStorage.getReportId(rewardPeriod)).thenReturn(reportId);
    when(hubReportStorage.getPeriodKey(rewardPeriod)).thenReturn(periodId);

    hubReport = newHubReport();
    report = hubReportService.getReport(periodId, false);
    assertEquals(toHubLocalReport(hubReport,
                                  periodId,
                                  reportId,
                                  false,
                                  false,
                                  HubReportStatusType.SENT,
                                  null), report);

  }

  @Test
  void getReportsWhenNoLocalRewards() {
    List<HubReportLocalStatus> reports = hubReportService.getReports(0, 10);
    assertNotNull(reports);
    assertEquals(0, reports.size());
  }

  @Test
  void getReportsWhenLocalRewardsNotSent()  {
    when(rewardReportService.findRewardReportPeriods(0, 10)).thenReturn(Collections.singletonList(rewardPeriod));
    
    List<HubReportLocalStatus> reports = hubReportService.getReports(0, 10);
    assertNotNull(reports);
    assertEquals(0, reports.size());
  }

  @Test
  void getReportsWhenLocalRewardsSent() {
    when(rewardReportService.findRewardReportPeriods(0, 10)).thenReturn(Collections.singletonList(rewardPeriod));
    when(rewardReportService.getRewardReportByPeriodId(periodId)).thenReturn(rewardReport);
    when(hubService.isConnected()).thenReturn(true);
    when(hubService.getHub()).thenReturn(hub);
    when(hubService.getHubAddress()).thenReturn(hubAddress);
    when(hubService.getDeedId()).thenReturn(deedId);
    when(organizationService.getUserHandler()).thenReturn(userHandler);
    when(hubReportStorage.getSentDate(rewardPeriod)).thenReturn(sentDate);

    when(realizationService.countParticipantsBetweenDates(any(), any())).thenReturn(participantsCount);
    when(realizationService.countRealizationsByFilter(argThat(filter -> filter != null
                                                                        && filter.getEarnerType() == IdentityType.USER
                                                                        && filter.getStatus() == RealizationStatus.ACCEPTED
                                                                        && filter.getFromDate().getTime() / 1000
                                                                            == periodStartTime
                                                                        && filter.getToDate().getTime() / 1000 == periodEndTime)))
                                                                                                                                  .thenReturn(achievementsCount);

    when(hubReportStorage.getStatus(rewardPeriod)).thenReturn("SENT");
    when(hubReportStorage.getReportId(rewardPeriod)).thenReturn(reportId);
    when(hubReportStorage.getPeriodKey(rewardPeriod)).thenReturn(periodId);

    List<HubReportLocalStatus> reports = hubReportService.getReports(0, 10);
    assertNotNull(reports);
    assertEquals(0, reports.size());
  }

  private HubReportPayload newHubReportPayload() {
    return new HubReportPayload(reportId,
                                StringUtils.lowerCase(hubAddress),
                                deedId,
                                fromDate(),
                                toDate(),
                                sentDate,
                                RewardPeriodType.WEEK.name(),
                                usersCount,
                                participantsCount,
                                recipientsCount,
                                achievementsCount,
                                StringUtils.lowerCase(tokenAddress),
                                tokenNetworkId,
                                tokensSent,
                                transactions());
  }

  private HubReport newHubReport() {
    return new HubReport(reportId,
                         hubAddress,
                         deedId,
                         fromDate(),
                         toDate(),
                         sentDate,
                         RewardPeriodType.WEEK.name(),
                         usersCount,
                         participantsCount,
                         recipientsCount,
                         achievementsCount,
                         tokenAddress,
                         tokenNetworkId,
                         tokensSent,
                         transactions(),
                         rewardId,
                         city,
                         cardType,
                         mintingPower,
                         maxUsers,
                         deedManagerAddress,
                         ownerAddress,
                         ownerMintingPercentage,
                         fixedRewardIndex,
                         ownerFixedIndex,
                         tenantFixedIndex,
                         fraud,
                         lastPeriodUemRewardAmount,
                         uemRewardAmount,
                         updatedDate);
  }

  private Instant toDate() {
    return Instant.ofEpochSecond(periodEndTime);
  }

  private Instant fromDate() {
    return Instant.ofEpochSecond(periodStartTime);
  }

  private TreeSet<String> transactions() {
    TreeSet<String> transactions = new TreeSet<>();
    transactions.add(transaction.getHash());
    return transactions;
  }

}
