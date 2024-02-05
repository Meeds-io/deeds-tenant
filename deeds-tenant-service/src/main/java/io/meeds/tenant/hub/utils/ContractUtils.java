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
package io.meeds.tenant.hub.utils;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.web3j.abi.EventValues;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.JsonRpcError;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.response.TransactionReceiptProcessor;

import io.meeds.tenant.hub.service.PolygonContractGasProvider;
import io.meeds.wom.api.constant.WomException;

import lombok.SneakyThrows;

public class ContractUtils {

  public static final Event REPORTSENT_EVENT = new Event("ReportSent",
                                                         Arrays.<TypeReference<?>> asList(new TypeReference<Address>(true) {
                                                         },
                                                                                          new TypeReference<Uint256>(true) {
                                                                                          }));

  private ContractUtils() {
    // NOSONAR
  }

  public static RemoteFunctionCall<TransactionReceipt> executeRemoteCallTransaction(TransactionManager transactionManager,
                                                                                    PolygonContractGasProvider polygonContractGasProvider,
                                                                                    TransactionReceiptProcessor transactionReceiptProcessor,
                                                                                    Function function,
                                                                                    String uemAddress,
                                                                                    long uemNetworkId) {
    return new RemoteFunctionCall<>(function,
                                    () -> executeTransaction(transactionManager,
                                                             polygonContractGasProvider,
                                                             transactionReceiptProcessor,
                                                             FunctionEncoder.encode(function),
                                                             function.getName(),
                                                             uemAddress,
                                                             uemNetworkId));
  }

  @SneakyThrows
  private static TransactionReceipt executeTransaction(TransactionManager transactionManager,
                                                       PolygonContractGasProvider polygonContractGasProvider,
                                                       TransactionReceiptProcessor transactionReceiptProcessor,
                                                       String data,
                                                       String funcName,
                                                       String uemAddress,
                                                       long uemNetworkId) {
    try {
      BigInteger estimatedGas;

      String fromAddress = transactionManager.getFromAddress();
      BigInteger maxPriorityFeePerGas = polygonContractGasProvider.getMaxPriorityFeePerGas(funcName);
      BigInteger maxFeePerGas = polygonContractGasProvider.getMaxFeePerGas(funcName);
      BigInteger gasLimit = polygonContractGasProvider.getGasLimit(funcName);
      BigInteger nonce = getNonce(polygonContractGasProvider.getWeb3j(), fromAddress);
      Transaction tx = Transaction.createFunctionCallTransaction(fromAddress,
                                                                 nonce,
                                                                 maxFeePerGas,
                                                                 gasLimit,
                                                                 uemAddress,
                                                                 data);
      EthEstimateGas gasEstimate = polygonContractGasProvider.getWeb3j().ethEstimateGas(tx).send();
      if (gasEstimate.hasError()) {
        throw new WomException(gasEstimate.getError().getMessage());
      } else {
        estimatedGas = gasEstimate.getAmountUsed();
      }

      EthSendTransaction ethSendTransaction = transactionManager.sendEIP1559Transaction(uemNetworkId,
                                                                                        maxPriorityFeePerGas,
                                                                                        maxFeePerGas,
                                                                                        BigDecimal.valueOf(estimatedGas.doubleValue())
                                                                                                  .multiply(BigDecimal.valueOf(1.2d))
                                                                                                  .toBigInteger(),
                                                                                        uemAddress,
                                                                                        data,
                                                                                        BigInteger.ZERO,
                                                                                        false);
      return processResponse(transactionReceiptProcessor, ethSendTransaction);
    } catch (JsonRpcError error) {
      if (error.getData() != null) {
        throw new TransactionException(error.getData().toString());
      } else {
        throw new TransactionException(
                                       String.format(
                                                     "JsonRpcError thrown with code %d. Message: %s",
                                                     error.getCode(),
                                                     error.getMessage()));
      }
    }
  }

  @SneakyThrows
  private static BigInteger getNonce(Web3j web3j, String address) {
    EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(address, DefaultBlockParameterName.PENDING)
                                                         .send();
    return ethGetTransactionCount.getTransactionCount();
  }

  private static TransactionReceipt processResponse(TransactionReceiptProcessor transactionReceiptProcessor,
                                                    EthSendTransaction ethSendTransaction) throws IOException,
                                                                                           TransactionException {
    if (ethSendTransaction.hasError()) {
      throw new JsonRpcError(ethSendTransaction.getError());
    }

    String transactionHash = ethSendTransaction.getTransactionHash();

    return transactionReceiptProcessor.waitForTransactionReceipt(transactionHash);
  }

  public static List<ReportSentEventResponse> getReportSentEvents(TransactionReceipt transactionReceipt) {
    List<EventValuesWithLog> valueList = staticExtractEventParametersWithLog(REPORTSENT_EVENT, transactionReceipt);
    ArrayList<ReportSentEventResponse> responses = new ArrayList<>(valueList.size());
    for (EventValuesWithLog eventValues : valueList) {
      ReportSentEventResponse typedResponse = new ReportSentEventResponse();
      typedResponse.log = eventValues.getLog();
      typedResponse.hub = (String) eventValues.getIndexedValues().get(0).getValue();
      typedResponse.reportId = (BigInteger) eventValues.getIndexedValues().get(1).getValue();
      responses.add(typedResponse);
    }
    return responses;
  }

  private static List<EventValuesWithLog> staticExtractEventParametersWithLog(Event event,
                                                                              TransactionReceipt transactionReceipt) {
    return transactionReceipt.getLogs()
                             .stream()
                             .map(log -> staticExtractEventParametersWithLog(event, log))
                             .filter(Objects::nonNull)
                             .toList();
  }

  private static EventValuesWithLog staticExtractEventParametersWithLog(Event event, Log log) {
    final EventValues eventValues = Contract.staticExtractEventParameters(event, log);
    return (eventValues == null) ? null : new EventValuesWithLog(eventValues, log);
  }

  @SuppressWarnings("rawtypes")
  public static class EventValuesWithLog {

    private final EventValues eventValues;

    private final Log         log;

    public EventValuesWithLog(EventValues eventValues, Log log) {
      this.eventValues = eventValues;
      this.log = log;
    }

    public List<Type> getIndexedValues() {
      return eventValues.getIndexedValues();
    }

    public List<Type> getNonIndexedValues() {
      return eventValues.getNonIndexedValues();
    }

    public Log getLog() {
      return log;
    }
  }

  public static class ReportSentEventResponse extends BaseEventResponse {

    public String     hub;      // NOSONAR

    public BigInteger reportId; // NOSONAR

  }

}
