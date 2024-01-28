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

import static io.meeds.tenant.hub.utils.ContractUtils.executeRemoteCallTransaction;
import static io.meeds.tenant.hub.utils.ContractUtils.getReportSentEvents;
import static io.meeds.wom.api.utils.JsonUtils.fromJsonString;
import static io.meeds.wom.api.utils.JsonUtils.toJsonString;
import static org.exoplatform.wallet.utils.WalletUtils.WALLET_ADMIN_REMOTE_ID;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.WalletFile;
import org.web3j.protocol.ObjectMapperFactory;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.tx.response.TransactionReceiptProcessor;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.exoplatform.wallet.model.WalletType;
import org.exoplatform.wallet.service.WalletAccountService;
import org.exoplatform.wallet.utils.WalletUtils;
import org.exoplatform.web.security.codec.CodecInitializer;
import org.exoplatform.web.security.security.TokenServiceInitializationException;

import io.meeds.tenant.hub.model.BlockchainHubReport;
import io.meeds.tenant.hub.service.PolygonContractGasProvider;
import io.meeds.tenant.hub.utils.ContractUtils.ReportSentEventResponse;
import io.meeds.wom.api.constant.WomException;
import io.meeds.wom.api.model.HubReportPayload;

import lombok.SneakyThrows;

@Component
public class HubWalletStorage {

  public static final String          FUNC_ADDREPORT = "addReport";

  @Autowired
  private WalletAccountService        walletAccountService;

  @Autowired
  private HubIdentityStorage          hubIdentityStorage;

  @Autowired
  private CodecInitializer            codecInitializer;

  @Autowired(required = false)
  private PolygonContractGasProvider  polygonContractGasProvider;

  private TransactionManager          transactionManager;

  private TransactionReceiptProcessor transactionReceiptProcessor;

  @SneakyThrows
  public String getOrCreateHubAddress() {
    String hubAddress = hubIdentityStorage.getHubAddress();
    if (StringUtils.startsWith(hubAddress, "0x")) {
      return hubAddress;
    } else {
      return createHubWallet();
    }
  }

  public ECKeyPair getHubWallet() throws WomException {
    String hubWalletString = hubIdentityStorage.getHubWallet();
    try {
      hubWalletString = codecInitializer.getCodec().decode(hubWalletString);
      WalletFile hubWallet = fromJsonString(hubWalletString, WalletFile.class);
      return org.web3j.crypto.Wallet.decrypt(walletAccountService.getAdminAccountPassword(), hubWallet);
    } catch (TokenServiceInitializationException | CipherException e) {
      throw new WomException("wom.unableToAccessHubWallet");
    }
  }

  public long sendReportTransaction(HubReportPayload report, String uemAddress, long uemNetworkId) throws WomException {
    BlockchainHubReport blockchainReport = new BlockchainHubReport(report.getHubAddress(),
                                                                   BigInteger.valueOf(report.getUsersCount()),
                                                                   BigInteger.valueOf(report.getRecipientsCount()),
                                                                   BigInteger.valueOf(report.getParticipantsCount()),
                                                                   BigInteger.valueOf(report.getAchievementsCount()),
                                                                   WalletUtils.convertToDecimals(report.getHubRewardAmount(),
                                                                                                 getHubContractDecimals()),
                                                                   report.getRewardTokenAddress(),
                                                                   BigInteger.valueOf(report.getRewardTokenNetworkId()),
                                                                   BigInteger.valueOf(report.getFromDate().getEpochSecond()),
                                                                   BigInteger.valueOf(report.getToDate().getEpochSecond()));

    @SuppressWarnings("rawtypes")
    Function function = new Function(FUNC_ADDREPORT,
                                     Arrays.<Type> asList(blockchainReport,
                                                          new org.web3j.abi.datatypes.Address(160, report.getHubAddress()),
                                                          new org.web3j.abi.datatypes.generated.Uint256(report.getDeedId())),
                                     Arrays.<TypeReference<?>> asList(new TypeReference<Uint256>() {
                                     }));

    RemoteFunctionCall<TransactionReceipt> remoteCall = executeRemoteCallTransaction(getTransactionManager(),
                                                                                     polygonContractGasProvider,
                                                                                     getTransactionReceiptProcessor(),
                                                                                     function,
                                                                                     uemAddress,
                                                                                     uemNetworkId);
    try {
      TransactionReceipt receipt = remoteCall.send();
      if (receipt == null) {
        throw new WomException("uem.sendReportFailedWithoutReceipt");
      } else if (!receipt.isStatusOK()) {
        String message = getUemContractMessage(receipt.getRevertReason());
        if (StringUtils.isNotBlank(message)) {
          throw new WomException(message);
        } else {
          message = getUemContractMessage(receipt.getStatus());
          if (StringUtils.isNotBlank(message)) {
            throw new WomException(message);
          } else {
            throw new WomException("uem.sendReportTransactionFailed");
          }
        }
      } else {
        List<ReportSentEventResponse> reportSentEvents = getReportSentEvents(receipt);
        return reportSentEvents.get(0).reportId.longValue();
      }
    } catch (Exception e) {
      String message = getUemContractExceptionMessage(e);
      if (StringUtils.isNotBlank(message)) {
        throw new WomException(message);
      } else {
        throw new IllegalStateException("Error While processing Deed Update transaction", e);
      }
    }
  }

  private String createHubWallet() throws WomException {
    // Re-use Admin wallet the first time the hub gets initialized
    ECKeyPair ecKeyPair = getAdminWalletKeys();
    try {
      return saveHubWallet(ecKeyPair);
    } catch (Exception e) {
      throw new WomException("wom.unableCreateHubWallet", e);
    }
  }

  @SneakyThrows
  private String saveHubWallet(ECKeyPair ecKeyPair) {
    String walletPassword = walletAccountService.getAdminAccountPassword();
    WalletFile hubWalletFile = org.web3j.crypto.Wallet.createStandard(walletPassword, ecKeyPair);
    String walletFileJson = toJsonString(hubWalletFile);
    walletFileJson = codecInitializer.getCodec().encode(walletFileJson);
    String address = hubWalletFile.getAddress();
    if (!StringUtils.startsWith(address, "0x")) {
      address = "0x" + address;
    }
    hubIdentityStorage.saveHubWallet(address, walletFileJson);
    return address;
  }

  private ECKeyPair getAdminWalletKeys() {
    String privateKey = walletAccountService.getPrivateKeyByTypeAndId(WalletType.ADMIN.getId(), WALLET_ADMIN_REMOTE_ID);
    if (StringUtils.isBlank(privateKey)) {
      return null;
    }
    WalletFile adminWallet = null;
    try {
      ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();
      adminWallet = objectMapper.readerFor(WalletFile.class).readValue(privateKey);
    } catch (Exception e) {
      throw new IllegalStateException("An error occurred while parsing admin wallet keys", e);
    }
    try {
      return org.web3j.crypto.Wallet.decrypt(walletAccountService.getAdminAccountPassword(), adminWallet);
    } catch (CipherException e) {
      throw new IllegalStateException("Can't descrypt stored admin wallet", e);
    }
  }

  private String getUemContractExceptionMessage(Throwable e) {
    if (e != null) {
      if (StringUtils.contains(e.getMessage(), "wom.")) {
        String message = getUemContractMessage(e.getMessage());
        if (StringUtils.isNotBlank(message)) {
          return message;
        }
      }
      if (e.getCause() != null) {
        return getUemContractExceptionMessage(e.getCause());
      }
    }
    return null;
  }

  private String getUemContractMessage(String message) {
    Matcher matcher = Pattern.compile("uem\\.[a-zA-Z0-9]+").matcher(message);
    if (matcher.find()) {
      return matcher.group();
    }
    return getWomContractMessage(message);
  }

  private String getWomContractMessage(String message) {
    Matcher matcher = Pattern.compile("wom\\.[a-zA-Z0-9]+").matcher(message);
    if (matcher.find()) {
      return matcher.group();
    }
    return null;
  }

  private int getHubContractDecimals() {
    return WalletUtils.getSettings().getContractDetail().getDecimals();
  }

  private TransactionManager getTransactionManager() throws WomException {
    if (transactionManager == null) {
      transactionManager = new RawTransactionManager(polygonContractGasProvider.getWeb3j(),
                                                     Credentials.create(getHubWallet()),
                                                     polygonContractGasProvider.getChainId());
    }
    return transactionManager;
  }

  private TransactionReceiptProcessor getTransactionReceiptProcessor() {
    if (transactionReceiptProcessor == null) {
      transactionReceiptProcessor = new PollingTransactionReceiptProcessor(polygonContractGasProvider.getWeb3j(),
                                                                           TransactionManager.DEFAULT_POLLING_FREQUENCY,
                                                                           TransactionManager.DEFAULT_POLLING_ATTEMPTS_PER_TX_HASH);
    }
    return transactionReceiptProcessor;
  }

}
