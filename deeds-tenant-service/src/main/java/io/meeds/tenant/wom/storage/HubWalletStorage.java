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
package io.meeds.tenant.wom.storage;

import static io.meeds.deeds.api.utils.JsonUtils.fromJsonString;
import static io.meeds.deeds.api.utils.JsonUtils.toJsonString;
import static org.exoplatform.wallet.utils.WalletUtils.WALLET_ADMIN_REMOTE_ID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.WalletFile;
import org.web3j.protocol.ObjectMapperFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.exoplatform.wallet.model.WalletType;
import org.exoplatform.wallet.service.WalletAccountService;
import org.exoplatform.web.security.codec.CodecInitializer;
import org.exoplatform.web.security.security.TokenServiceInitializationException;

import io.meeds.deeds.api.constant.WomException;

import lombok.SneakyThrows;

@Component
public class HubWalletStorage {

  @Autowired
  private WalletAccountService walletAccountService;

  @Autowired
  private HubIdentityStorage   hubIdentityStorage;

  @Autowired
  private CodecInitializer     codecInitializer;

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

}
