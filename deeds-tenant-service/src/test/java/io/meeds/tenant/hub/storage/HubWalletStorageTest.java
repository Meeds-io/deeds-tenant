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

import static org.exoplatform.wallet.utils.WalletUtils.WALLET_ADMIN_REMOTE_ID;
import static org.exoplatform.wallet.utils.WalletUtils.toJsonString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.TreeSet;

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
import org.web3j.abi.EventValues;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.WalletFile;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.JsonRpcError;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.response.TransactionReceiptProcessor;

import org.exoplatform.wallet.model.ContractDetail;
import org.exoplatform.wallet.model.WalletType;
import org.exoplatform.wallet.model.reward.RewardPeriodType;
import org.exoplatform.wallet.model.settings.GlobalSettings;
import org.exoplatform.wallet.service.WalletAccountService;
import org.exoplatform.wallet.utils.WalletUtils;
import org.exoplatform.web.security.codec.AbstractCodec;
import org.exoplatform.web.security.codec.CodecInitializer;

import io.meeds.tenant.hub.service.PolygonContractGasProvider;
import io.meeds.wom.api.constant.WomException;
import io.meeds.wom.api.model.HubReportPayload;

import lombok.SneakyThrows;

@SpringBootTest(classes = {
                            HubWalletStorage.class,
})
@ExtendWith(MockitoExtension.class)
class HubWalletStorageTest {

  @MockBean
  private WalletAccountService        walletAccountService;

  @MockBean
  private HubIdentityStorage          hubIdentityStorage;

  @MockBean
  private CodecInitializer            codecInitializer;

  @MockBean
  private PolygonContractGasProvider  polygonContractGasProvider;

  @Autowired
  private HubWalletStorage            hubWalletStorage;

  @Mock
  private Web3j                       web3j;

  @Mock
  private TransactionManager          transactionManager;

  @Mock
  private TransactionReceiptProcessor transactionReceiptProcessor;

  @Mock
  private AbstractCodec               codec;

  @Mock
  private GlobalSettings              globalSettings;

  @Mock
  private ContractDetail              contractDetail;

  private Credentials                 hubWalletCredentials =
                                                           Credentials.create("0x1da4ef21b864d2cc526dbdb2a120bd2874c36c9d0a1fb7f8c63d7f7a8b41de8f");

  private ECKeyPair                   hubWallet            = hubWalletCredentials.getEcKeyPair();

  private String                      hubAddress           = hubWalletCredentials.getAddress();

  private String                      rewardTokenAddress   = "0x654d85047da64738c065d36e10b2adeb965000d0";

  private long                        rewardTokenNetworkId = 25641l;

  private String                      password             = "password";

  private long                        periodStartTime      = ZonedDateTime.now()
                                                                          .with(DayOfWeek.MONDAY)
                                                                          .minusWeeks(3)
                                                                          .toLocalDate()
                                                                          .atStartOfDay(ZoneOffset.UTC)
                                                                          .toEpochSecond();

  private long                        periodEndTime        = ZonedDateTime.now()
                                                                          .with(DayOfWeek.MONDAY)
                                                                          .minusWeeks(2)
                                                                          .toLocalDate()
                                                                          .atStartOfDay(ZoneOffset.UTC)
                                                                          .toEpochSecond();

  private long                        reportId             = 5l;

  private long                        uemNetworkId         = 80001l;

  private Integer                     decimals             = 18;

  private long                        deedId               = 35l;

  private long                        usersCount           = 125l;

  private long                        participantsCount    = 85l;

  private long                        recipientsCount      = 65l;

  private int                         achievementsCount    = 55698;

  private int                         actionsCount         = 656;

  private double                      tokensSent           = 52.3365d;

  private double                      topReceiverAmount    = 4.6d;

  private String                      uemAddress           = "0x290b11b1ab6a31ff95490e4e0eeffec6402cce99";

  private String                      txHash               =
                                             "0xef4e9db309b5dd7020ce463ae726b4d0759e1de0635661de91d8d98e83ae2862";

  private Instant                     sentDate             = Instant.now();

  private MockedStatic<WalletUtils>   walletUtils;

  private MockedStatic<Contract>      contract;

  @BeforeEach
  void init() {
    walletUtils = mockStatic(WalletUtils.class);
    contract = mockStatic(Contract.class);
    walletUtils.when(WalletUtils::getNetworkId).thenReturn(rewardTokenNetworkId);
    walletUtils.when(WalletUtils::getContractAddress).thenReturn(rewardTokenAddress);
    walletUtils.when(() -> WalletUtils.toJsonString(any())).thenCallRealMethod();
    walletUtils.when(() -> WalletUtils.fromJsonString(any(), any())).thenCallRealMethod();
  }

  @AfterEach
  void close() {
    walletUtils.close();
    contract.close();
  }

  @Test
  void getOrCreateHubAddressWhenExists()  {
    when(hubIdentityStorage.getHubAddress()).thenReturn(hubAddress);
    assertEquals(hubAddress, hubWalletStorage.getOrCreateHubAddress());
    verifyNoInteractions(codecInitializer, walletAccountService);
  }

  @Test
  void getOrCreateHubAddressWhenAdminWalletNotExists() {
    String createdHubAddress = hubWalletStorage.getOrCreateHubAddress();
    assertNull(createdHubAddress);
  }

  @Test
  void getOrCreateHubAddressWhenNotExists() {
    String newHubAddress = generateWallet();
    assertEquals(hubAddress.replace("0x", "").toLowerCase(), newHubAddress.replace("0x", "").toLowerCase());

    String createdHubAddress = hubWalletStorage.getOrCreateHubAddress();
    assertEquals(newHubAddress.replace("0x", "").toLowerCase(),
                 createdHubAddress.replace("0x", "").toLowerCase());
    verify(hubIdentityStorage).saveHubWallet(argThat(address -> newHubAddress.replace("0x", "")
                                                                             .equalsIgnoreCase(createdHubAddress.replace("0x",
                                                                                                                         ""))),
                                             argThat(Objects::nonNull));
  }

  @Test
  @SneakyThrows
  void getOrCreateHubAddressWhenError() {
    WalletFile walletFile = org.web3j.crypto.Wallet.createLight(password, hubWallet);
    when(codecInitializer.getCodec()).thenReturn(codec);
    when(walletAccountService.getPrivateKeyByTypeAndId(WalletType.ADMIN.getId(),
                                                       WALLET_ADMIN_REMOTE_ID)).thenAnswer(invocation -> toJsonString(walletFile));
    when(walletAccountService.getAdminAccountPassword()).thenThrow(IllegalStateException.class);
    assertThrows(WomException.class, () -> hubWalletStorage.getOrCreateHubAddress());
    verify(hubIdentityStorage, never()).saveHubWallet(any(), any());
  }

  @Test
  void signHubMessage() throws WomException {
    when(hubIdentityStorage.getHubWallet()).thenAnswer(invocation -> {
      WalletFile walletFile = org.web3j.crypto.Wallet.createLight(password, hubWallet);
      when(codecInitializer.getCodec()).thenReturn(codec);
      when(walletAccountService.getAdminAccountPassword()).thenReturn(password);
      when(codec.decode(any())).thenAnswer(args -> args.getArgument(0));
      return toJsonString(walletFile);
    });

    String signedMessage = hubWalletStorage.signHubMessage("rawMessage");
    assertEquals("0x02c66a7f71d95d64b85ef051010cdda667f09d2ffc6119bbd05ad3c4ba2245c139b0ae4a259dcff1ddf97d7175550b1de3b9a743d59eaddb783f9b910f7a571b1b".replace("0x", "").toLowerCase(),
                 signedMessage.replace("0x", "").toLowerCase());
  }

  @SuppressWarnings("rawtypes")
  @Test
  @SneakyThrows
  void sendReportTransaction() {
    walletUtils.when(WalletUtils::getSettings).thenReturn(globalSettings);
    walletUtils.when(() -> WalletUtils.convertToDecimals(anyDouble(), anyInt())).thenCallRealMethod();
    when(globalSettings.getContractDetail()).thenReturn(contractDetail);
    when(contractDetail.getDecimals()).thenReturn(decimals);
    when(polygonContractGasProvider.getWeb3j()).thenReturn(web3j);
    when(polygonContractGasProvider.getChainId()).thenReturn(uemNetworkId);
    HubReportPayload reportPayload = newHubReportPayload();
    when(hubIdentityStorage.getHubWallet()).thenAnswer(invocation -> {
      WalletFile walletFile = org.web3j.crypto.Wallet.createLight(password, hubWallet);
      when(codecInitializer.getCodec()).thenReturn(codec);
      when(walletAccountService.getAdminAccountPassword()).thenReturn(password);
      when(codec.decode(any())).thenAnswer(args -> args.getArgument(0));
      return toJsonString(walletFile);
    });

    hubWalletStorage.setTransactionManager(transactionManager);
    hubWalletStorage.setTransactionReceiptProcessor(transactionReceiptProcessor);

    BigInteger nonce = BigInteger.TWO;
    BigInteger estimatedGas = BigInteger.TEN;
    when(web3j.ethGetTransactionCount(hubAddress, DefaultBlockParameterName.PENDING)).thenAnswer(args -> {
      EthGetTransactionCount ethGetTransactionCount = mock(EthGetTransactionCount.class);
      Request request = mock(Request.class);
      when(request.send()).thenReturn(ethGetTransactionCount);
      when(ethGetTransactionCount.getTransactionCount()).thenReturn(nonce);
      return request;
    });
    when(web3j.ethEstimateGas(any())).thenAnswer(args -> {
      EthEstimateGas gasEstimate = mock(EthEstimateGas.class);
      Request request = mock(Request.class);
      when(request.send()).thenReturn(gasEstimate);
      when(gasEstimate.getAmountUsed()).thenReturn(estimatedGas);
      return request;
    });

    BigInteger gasPrice = BigInteger.valueOf(31l);
    when(polygonContractGasProvider.getGasPrice(any())).thenReturn(gasPrice);

    when(transactionManager.getFromAddress()).thenReturn(hubAddress);
    EthSendTransaction ethSendTransaction = mock(EthSendTransaction.class);
    TransactionReceipt receipt = mock(TransactionReceipt.class);
    when(transactionManager.sendTransaction(eq(gasPrice),
                                            eq(BigDecimal.valueOf(estimatedGas.doubleValue())
                                                         .multiply(BigDecimal.valueOf(1.2d))
                                                         .toBigInteger()),
                                            eq(uemAddress),
                                            anyString(),
                                            eq(BigInteger.ZERO))).thenReturn(ethSendTransaction);
    when(transactionReceiptProcessor.waitForTransactionReceipt(any())).thenReturn(receipt);
    when(receipt.isStatusOK()).thenReturn(true);
    when(receipt.getLogs()).thenReturn(Collections.singletonList(mock(Log.class)));
    mockReportSentEventResponse();

    long createdReportId = hubWalletStorage.sendReportTransaction(reportPayload, uemAddress, uemNetworkId);
    assertEquals(reportId, createdReportId);
  }

  @SuppressWarnings("rawtypes")
  @Test
  @SneakyThrows
  void sendReportTransactionWhenReceiptError() {
    walletUtils.when(WalletUtils::getSettings).thenReturn(globalSettings);
    walletUtils.when(() -> WalletUtils.convertToDecimals(anyDouble(), anyInt())).thenCallRealMethod();
    when(globalSettings.getContractDetail()).thenReturn(contractDetail);
    when(contractDetail.getDecimals()).thenReturn(decimals);
    when(polygonContractGasProvider.getWeb3j()).thenReturn(web3j);
    when(polygonContractGasProvider.getChainId()).thenReturn(uemNetworkId);
    HubReportPayload reportPayload = newHubReportPayload();
    when(hubIdentityStorage.getHubWallet()).thenAnswer(invocation -> {
      WalletFile walletFile = org.web3j.crypto.Wallet.createLight(password, hubWallet);
      when(codecInitializer.getCodec()).thenReturn(codec);
      when(walletAccountService.getAdminAccountPassword()).thenReturn(password);
      when(codec.decode(any())).thenAnswer(args -> args.getArgument(0));
      return toJsonString(walletFile);
    });

    hubWalletStorage.setTransactionManager(transactionManager);
    hubWalletStorage.setTransactionReceiptProcessor(transactionReceiptProcessor);

    BigInteger nonce = BigInteger.TWO;
    BigInteger estimatedGas = BigInteger.TEN;
    when(web3j.ethGetTransactionCount(hubAddress, DefaultBlockParameterName.PENDING)).thenAnswer(args -> {
      EthGetTransactionCount ethGetTransactionCount = mock(EthGetTransactionCount.class);
      Request request = mock(Request.class);
      when(request.send()).thenReturn(ethGetTransactionCount);
      when(ethGetTransactionCount.getTransactionCount()).thenReturn(nonce);
      return request;
    });
    when(web3j.ethEstimateGas(any())).thenAnswer(args -> {
      EthEstimateGas gasEstimate = mock(EthEstimateGas.class);
      Request request = mock(Request.class);
      when(request.send()).thenReturn(gasEstimate);
      when(gasEstimate.getAmountUsed()).thenReturn(estimatedGas);
      return request;
    });

    BigInteger gasPrice = BigInteger.valueOf(31l);
    when(polygonContractGasProvider.getGasPrice(any())).thenReturn(gasPrice);

    when(transactionManager.getFromAddress()).thenReturn(hubAddress);
    EthSendTransaction ethSendTransaction = mock(EthSendTransaction.class);
    when(ethSendTransaction.getTransactionHash()).thenReturn(txHash);
    when(transactionManager.sendTransaction(eq(gasPrice),
                                            eq(BigDecimal.valueOf(estimatedGas.doubleValue())
                                                         .multiply(BigDecimal.valueOf(1.2d))
                                                         .toBigInteger()),
                                            eq(uemAddress),
                                            anyString(),
                                            eq(BigInteger.ZERO))).thenReturn(ethSendTransaction);

    WomException exception = assertThrows(WomException.class,
                                          () -> hubWalletStorage.sendReportTransaction(reportPayload, uemAddress, uemNetworkId));
    assertEquals("uem.sendReportFailedWithoutReceipt", exception.getMessage());

    TransactionReceipt receipt = mock(TransactionReceipt.class);
    when(transactionReceiptProcessor.waitForTransactionReceipt(any())).thenReturn(receipt);

    exception = assertThrows(WomException.class,
                             () -> hubWalletStorage.sendReportTransaction(reportPayload, uemAddress, uemNetworkId));
    assertEquals("uem.sendReportTransactionFailed", exception.getMessage());

    String revertError = "uem.sendReportTransactionFailedCustomMessage";
    when(receipt.getStatus()).thenReturn(revertError);
    exception = assertThrows(WomException.class,
                             () -> hubWalletStorage.sendReportTransaction(reportPayload, uemAddress, uemNetworkId));
    assertEquals(revertError, exception.getMessage());

    when(receipt.getRevertReason()).thenReturn(revertError);
    exception = assertThrows(WomException.class,
                             () -> hubWalletStorage.sendReportTransaction(reportPayload, uemAddress, uemNetworkId));
    assertEquals(revertError, exception.getMessage());

    revertError = "wom.sendReportTransactionFailedCustomMessage";
    when(receipt.getRevertReason()).thenReturn(revertError);
    exception = assertThrows(WomException.class,
                             () -> hubWalletStorage.sendReportTransaction(reportPayload, uemAddress, uemNetworkId));
    assertEquals(revertError, exception.getMessage());
  }

  @SuppressWarnings("rawtypes")
  @Test
  @SneakyThrows
  void sendReportTransactionWhenEstimationError() {
    walletUtils.when(WalletUtils::getSettings).thenReturn(globalSettings);
    walletUtils.when(() -> WalletUtils.convertToDecimals(anyDouble(), anyInt())).thenCallRealMethod();
    when(globalSettings.getContractDetail()).thenReturn(contractDetail);
    when(contractDetail.getDecimals()).thenReturn(decimals);
    when(polygonContractGasProvider.getWeb3j()).thenReturn(web3j);
    when(polygonContractGasProvider.getChainId()).thenReturn(uemNetworkId);
    HubReportPayload reportPayload = newHubReportPayload();
    when(hubIdentityStorage.getHubWallet()).thenAnswer(invocation -> {
      WalletFile walletFile = org.web3j.crypto.Wallet.createLight(password, hubWallet);
      when(codecInitializer.getCodec()).thenReturn(codec);
      when(walletAccountService.getAdminAccountPassword()).thenReturn(password);
      when(codec.decode(any())).thenAnswer(args -> args.getArgument(0));
      return toJsonString(walletFile);
    });

    hubWalletStorage.setTransactionManager(transactionManager);
    hubWalletStorage.setTransactionReceiptProcessor(transactionReceiptProcessor);

    BigInteger nonce = BigInteger.TWO;
    when(web3j.ethGetTransactionCount(hubAddress, DefaultBlockParameterName.PENDING)).thenAnswer(args -> {
      EthGetTransactionCount ethGetTransactionCount = mock(EthGetTransactionCount.class);
      Request request = mock(Request.class);
      when(request.send()).thenReturn(ethGetTransactionCount);
      when(ethGetTransactionCount.getTransactionCount()).thenReturn(nonce);
      return request;
    });
    String customRevertException = "uem.customRevertError";
    when(web3j.ethEstimateGas(any())).thenAnswer(args -> {
      EthEstimateGas gasEstimate = mock(EthEstimateGas.class);
      Request request = mock(Request.class);
      when(request.send()).thenReturn(gasEstimate);
      when(gasEstimate.hasError()).thenReturn(true);
      when(gasEstimate.getError()).thenReturn(new org.web3j.protocol.core.Response.Error(0, customRevertException));
      return request;
    });

    BigInteger gasPrice = BigInteger.valueOf(31l);
    when(polygonContractGasProvider.getGasPrice(any())).thenReturn(gasPrice);

    when(transactionManager.getFromAddress()).thenReturn(hubAddress);
    WomException exception = assertThrows(WomException.class,
                                          () -> hubWalletStorage.sendReportTransaction(reportPayload, uemAddress, uemNetworkId));
    assertEquals(customRevertException, exception.getMessage());
  }

  @SuppressWarnings("rawtypes")
  @Test
  @SneakyThrows
  void sendReportTransactionWhenEstimationJsonRpcErrorInMessage() {
    walletUtils.when(WalletUtils::getSettings).thenReturn(globalSettings);
    walletUtils.when(() -> WalletUtils.convertToDecimals(anyDouble(), anyInt())).thenCallRealMethod();
    when(globalSettings.getContractDetail()).thenReturn(contractDetail);
    when(contractDetail.getDecimals()).thenReturn(decimals);
    when(polygonContractGasProvider.getWeb3j()).thenReturn(web3j);
    when(polygonContractGasProvider.getChainId()).thenReturn(uemNetworkId);
    HubReportPayload reportPayload = newHubReportPayload();
    when(hubIdentityStorage.getHubWallet()).thenAnswer(invocation -> {
      WalletFile walletFile = org.web3j.crypto.Wallet.createLight(password, hubWallet);
      when(codecInitializer.getCodec()).thenReturn(codec);
      when(walletAccountService.getAdminAccountPassword()).thenReturn(password);
      when(codec.decode(any())).thenAnswer(args -> args.getArgument(0));
      return toJsonString(walletFile);
    });

    hubWalletStorage.setTransactionManager(transactionManager);
    hubWalletStorage.setTransactionReceiptProcessor(transactionReceiptProcessor);

    BigInteger nonce = BigInteger.TWO;
    when(web3j.ethGetTransactionCount(hubAddress, DefaultBlockParameterName.PENDING)).thenAnswer(args -> {
      EthGetTransactionCount ethGetTransactionCount = mock(EthGetTransactionCount.class);
      Request request = mock(Request.class);
      when(request.send()).thenReturn(ethGetTransactionCount);
      when(ethGetTransactionCount.getTransactionCount()).thenReturn(nonce);
      return request;
    });
    BigInteger gasPrice = BigInteger.valueOf(35l);
    when(polygonContractGasProvider.getGasPrice(any())).thenReturn(gasPrice);

    when(transactionManager.getFromAddress()).thenReturn(hubAddress);

    String customRevertException = "uem.customRevertExceptionInMessage";
    when(web3j.ethEstimateGas(any())).thenThrow(new JsonRpcError(0, customRevertException, null));
    WomException exception = assertThrows(WomException.class,
                                          () -> hubWalletStorage.sendReportTransaction(reportPayload, uemAddress, uemNetworkId));
    assertEquals(customRevertException, exception.getMessage());
  }

  @SuppressWarnings("rawtypes")
  @Test
  @SneakyThrows
  void sendReportTransactionWhenEstimationJsonRpcErrorInData() {
    walletUtils.when(WalletUtils::getSettings).thenReturn(globalSettings);
    walletUtils.when(() -> WalletUtils.convertToDecimals(anyDouble(), anyInt())).thenCallRealMethod();
    when(globalSettings.getContractDetail()).thenReturn(contractDetail);
    when(contractDetail.getDecimals()).thenReturn(decimals);
    when(polygonContractGasProvider.getWeb3j()).thenReturn(web3j);
    when(polygonContractGasProvider.getChainId()).thenReturn(uemNetworkId);
    HubReportPayload reportPayload = newHubReportPayload();
    when(hubIdentityStorage.getHubWallet()).thenAnswer(invocation -> {
      WalletFile walletFile = org.web3j.crypto.Wallet.createLight(password, hubWallet);
      when(codecInitializer.getCodec()).thenReturn(codec);
      when(walletAccountService.getAdminAccountPassword()).thenReturn(password);
      when(codec.decode(any())).thenAnswer(args -> args.getArgument(0));
      return toJsonString(walletFile);
    });

    hubWalletStorage.setTransactionManager(transactionManager);
    hubWalletStorage.setTransactionReceiptProcessor(transactionReceiptProcessor);

    BigInteger nonce = BigInteger.TWO;
    when(web3j.ethGetTransactionCount(hubAddress, DefaultBlockParameterName.PENDING)).thenAnswer(args -> {
      EthGetTransactionCount ethGetTransactionCount = mock(EthGetTransactionCount.class);
      Request request = mock(Request.class);
      when(request.send()).thenReturn(ethGetTransactionCount);
      when(ethGetTransactionCount.getTransactionCount()).thenReturn(nonce);
      return request;
    });
    BigInteger gasPrice = BigInteger.valueOf(31l);
    when(polygonContractGasProvider.getGasPrice(any())).thenReturn(gasPrice);

    when(transactionManager.getFromAddress()).thenReturn(hubAddress);

    String customRevertException = "uem.customRevertExceptionInData";
    when(web3j.ethEstimateGas(any())).thenThrow(new JsonRpcError(0, null, customRevertException));
    WomException exception = assertThrows(WomException.class,
                                          () -> hubWalletStorage.sendReportTransaction(reportPayload, uemAddress, uemNetworkId));
    assertEquals(customRevertException, exception.getMessage());
  }

  @SneakyThrows
  public String generateWallet() {
    WalletFile walletFile = org.web3j.crypto.Wallet.createLight(password, hubWallet);
    when(codecInitializer.getCodec()).thenReturn(codec);
    when(codec.encode(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(walletAccountService.getAdminAccountPassword()).thenReturn(password);
    when(walletAccountService.getPrivateKeyByTypeAndId(WalletType.ADMIN.getId(),
                                                       WALLET_ADMIN_REMOTE_ID)).thenAnswer(invocation -> toJsonString(walletFile));
    assertNotNull(walletAccountService.getPrivateKeyByTypeAndId(WalletType.ADMIN.getId(), WALLET_ADMIN_REMOTE_ID));
    return walletFile.getAddress();
  }

  void mockReportSentEventResponse() {
    EventValues eventValues = new EventValues(Arrays.asList(new Address(hubAddress), new Uint256(reportId)), null);
    contract.when(() -> Contract.staticExtractEventParameters(any(), any())).thenReturn(eventValues);
  }

  public HubReportPayload newHubReportPayload() {
    return new HubReportPayload(reportId,
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
                                actionsCount,
                                rewardTokenAddress,
                                rewardTokenNetworkId,
                                tokensSent,
                                topReceiverAmount,
                                transactions());
  }

  private Instant toDate() {
    return Instant.ofEpochSecond(periodEndTime);
  }

  private Instant fromDate() {
    return Instant.ofEpochSecond(periodStartTime);
  }

  private TreeSet<String> transactions() {
    TreeSet<String> transactions = new TreeSet<>();
    transactions.add(txHash);
    return transactions;
  }

}
