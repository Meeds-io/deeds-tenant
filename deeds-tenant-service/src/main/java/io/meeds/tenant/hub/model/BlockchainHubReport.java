package io.meeds.tenant.hub.model;

import java.math.BigInteger;

import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.StaticStruct;
import org.web3j.abi.datatypes.generated.Uint256;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = false)
public class BlockchainHubReport extends StaticStruct {

  public String     hub;               // NOSONAR

  public BigInteger usersCount;        // NOSONAR

  public BigInteger recipientsCount;   // NOSONAR

  public BigInteger participantsCount; // NOSONAR

  public BigInteger achievementsCount; // NOSONAR

  public BigInteger amount;            // NOSONAR

  public String     tokenAddress;      // NOSONAR

  public BigInteger tokenChainId;      // NOSONAR

  public BigInteger fromDate;          // NOSONAR

  public BigInteger toDate;            // NOSONAR

  public BlockchainHubReport(String hub, // NOSONAR
                             BigInteger usersCount,
                             BigInteger recipientsCount,
                             BigInteger participantsCount,
                             BigInteger achievementsCount,
                             BigInteger amount,
                             String tokenAddress,
                             BigInteger tokenChainId,
                             BigInteger fromDate,
                             BigInteger toDate) {
    super(new org.web3j.abi.datatypes.Address(160, hub),
          new org.web3j.abi.datatypes.generated.Uint256(usersCount),
          new org.web3j.abi.datatypes.generated.Uint256(recipientsCount),
          new org.web3j.abi.datatypes.generated.Uint256(participantsCount),
          new org.web3j.abi.datatypes.generated.Uint256(achievementsCount),
          new org.web3j.abi.datatypes.generated.Uint256(amount),
          new org.web3j.abi.datatypes.Address(160, tokenAddress),
          new org.web3j.abi.datatypes.generated.Uint256(tokenChainId),
          new org.web3j.abi.datatypes.generated.Uint256(fromDate),
          new org.web3j.abi.datatypes.generated.Uint256(toDate));
    this.hub = hub;
    this.usersCount = usersCount;
    this.recipientsCount = recipientsCount;
    this.participantsCount = participantsCount;
    this.achievementsCount = achievementsCount;
    this.amount = amount;
    this.tokenAddress = tokenAddress;
    this.tokenChainId = tokenChainId;
    this.fromDate = fromDate;
    this.toDate = toDate;
  }

  public BlockchainHubReport(Address hub, // NOSONAR
                             Uint256 usersCount,
                             Uint256 recipientsCount,
                             Uint256 participantsCount,
                             Uint256 achievementsCount,
                             Uint256 amount,
                             Address tokenAddress,
                             Uint256 tokenChainId,
                             Uint256 fromDate,
                             Uint256 toDate) {
    super(hub,
          usersCount,
          recipientsCount,
          participantsCount,
          achievementsCount,
          amount,
          tokenAddress,
          tokenChainId,
          fromDate,
          toDate);
    this.hub = hub.getValue();
    this.usersCount = usersCount.getValue();
    this.recipientsCount = recipientsCount.getValue();
    this.participantsCount = participantsCount.getValue();
    this.achievementsCount = achievementsCount.getValue();
    this.amount = amount.getValue();
    this.tokenAddress = tokenAddress.getValue();
    this.tokenChainId = tokenChainId.getValue();
    this.fromDate = fromDate.getValue();
    this.toDate = toDate.getValue();
  }

}
