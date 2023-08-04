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

import static io.meeds.deeds.utils.JsonUtils.fromJsonString;
import static io.meeds.deeds.utils.JsonUtils.toJsonString;
import static io.meeds.tenant.plugin.WalletHubIdentityProvider.ADDRESS;
import static io.meeds.tenant.plugin.WalletHubIdentityProvider.WALLET;

import org.apache.commons.lang3.StringUtils;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.WalletFile;
import org.web3j.crypto.WalletUtils;

import org.exoplatform.wallet.service.WalletAccountService;
import org.exoplatform.web.security.codec.CodecInitializer;
import org.exoplatform.web.security.security.TokenServiceInitializationException;

import io.meeds.deeds.constant.WomException;
import io.meeds.deeds.constant.WomParsingException;

public class HubWalletStorage {

  private WalletAccountService walletAccountService;

  private HubIdentityStorage   hubIdentityStorage;

  private CodecInitializer     codecInitializer;

  public HubWalletStorage(WalletAccountService walletAccountService,
                          HubIdentityStorage hubIdentityStorage,
                          CodecInitializer codecInitializer) {
    this.walletAccountService = walletAccountService;
    this.hubIdentityStorage = hubIdentityStorage;
    this.codecInitializer = codecInitializer;
  }

  public String getOrCreateHubAddress(String hubPrivateKey) throws WomException {
    String hubAddress = hubIdentityStorage.getHubProperty(ADDRESS);
    if (StringUtils.startsWith(hubAddress, "0x")) {
      return hubAddress;
    } else {
      return createHubWallet(hubPrivateKey);
    }
  }

  public ECKeyPair getHubWallet() throws WomException {
    String hubWalletString = hubIdentityStorage.getHubProperty(WALLET);
    try {
      hubWalletString = codecInitializer.getCodec().decode(hubWalletString);
      WalletFile hubWallet = fromJsonString(hubWalletString, WalletFile.class);
      return org.web3j.crypto.Wallet.decrypt(walletAccountService.getAdminAccountPassword(), hubWallet);
    } catch (TokenServiceInitializationException | CipherException e) {
      throw new WomException("wom.unableToAccessHubWallet");
    }
  }

  private String createHubWallet(String hubPrivateKey) throws WomException {
    ECKeyPair ecKeyPair = generateWalletKeys(hubPrivateKey);
    try {
      return saveHubWallet(ecKeyPair);
    } catch (Exception e) {
      throw new WomException("wom.unableCreateHubWallet", e);
    }
  }

  private ECKeyPair generateWalletKeys(String hubPrivateKey) throws WomException {
    ECKeyPair ecKeyPair = null;
    if (StringUtils.isBlank(hubPrivateKey)) {
      try {
        ecKeyPair = Keys.createEcKeyPair();
      } catch (Exception e) {
        throw new WomException("wom.unableCreateHubWallet", e);
      }
    } else {
      if (!WalletUtils.isValidPrivateKey(hubPrivateKey)) {
        throw new WomException("wom.unableCreateHubWallet");
      }
      ecKeyPair = Credentials.create(hubPrivateKey).getEcKeyPair();
    }
    return ecKeyPair;
  }

  private String saveHubWallet(ECKeyPair ecKeyPair) throws CipherException, WomParsingException,
                                                    TokenServiceInitializationException {
    String walletPassword = walletAccountService.getAdminAccountPassword();
    WalletFile hubWalletFile = org.web3j.crypto.Wallet.createStandard(walletPassword, ecKeyPair);
    String walletFileJson = toJsonString(hubWalletFile);
    walletFileJson = codecInitializer.getCodec().encode(walletFileJson);
    hubIdentityStorage.saveHubProperty(WALLET, walletFileJson);
    String address = hubWalletFile.getAddress();
    if (!StringUtils.startsWith(address, "0x")) {
      address = "0x" + address;
    }
    hubIdentityStorage.saveHubProperty(ADDRESS, StringUtils.lowerCase(address));
    return address;
  }

}
