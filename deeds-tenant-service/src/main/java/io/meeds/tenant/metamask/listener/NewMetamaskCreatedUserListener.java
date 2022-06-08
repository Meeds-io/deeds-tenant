/**
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2022 Meeds Association
 * contact@meeds.io
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
package io.meeds.tenant.metamask.listener;

import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserEventListener;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.model.WalletProvider;
import org.exoplatform.wallet.service.WalletAccountService;
import org.web3j.crypto.WalletUtils;

import static org.exoplatform.wallet.utils.WalletUtils.NEW_ADDRESS_ASSOCIATED_EVENT;

public class NewMetamaskCreatedUserListener extends UserEventListener {

  private static final Log     LOG = ExoLogger.getLogger(NewMetamaskCreatedUserListener.class);

  private IdentityManager      identityManager;

  private WalletAccountService walletAccountService;

  private ListenerService      listenerService;

  public NewMetamaskCreatedUserListener(IdentityManager identityManager,
                                        WalletAccountService walletAccountService,
                                        ListenerService listenerService) {
    this.walletAccountService = walletAccountService;
    this.identityManager = identityManager;
    this.listenerService = listenerService;
  }

  @Override
  public void postSave(User user, boolean isNew) throws Exception {
    String address = user.getUserName();
    if (!isNew || !user.isEnabled() || !WalletUtils.isValidAddress(address)) {
      return;
    }
    Wallet wallet = walletAccountService.getWalletByAddress(address);
    if (wallet != null) {
      LOG.info("Wallet with address {} is already associated to identity id {}."
          + "The used Metamask address will not be associated to current user account.", address, wallet.getTechnicalId());
      return;
    }
    wallet = createUserWalletByAddress(address);
    if (wallet != null) {
      listenerService.broadcast(NEW_ADDRESS_ASSOCIATED_EVENT, wallet.clone(), address);
    }
  }

  private Wallet createUserWalletByAddress(String address) {
    try {
      Identity identity = identityManager.getOrCreateUserIdentity(address);
      Wallet userWallet = walletAccountService.createWalletInstance(WalletProvider.METAMASK,
                                                                    address,
                                                                    Long.valueOf(identity.getId()));
      return walletAccountService.saveWallet(userWallet, true);
    } catch (Exception e) {
      LOG.warn("Error while associating Metamask wallet for user {}", address, e);
      return null;
    }
  }

}
