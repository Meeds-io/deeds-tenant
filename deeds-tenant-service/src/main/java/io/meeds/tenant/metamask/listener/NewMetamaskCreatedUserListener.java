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

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserEventListener;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.model.WalletProvider;
import org.exoplatform.wallet.service.WalletAccountService;
import org.web3j.crypto.WalletUtils;

import static io.meeds.tenant.metamask.utils.Utils.getIdentityByUsername;

public class NewMetamaskCreatedUserListener extends UserEventListener {

  private static final Log LOG = ExoLogger.getLogger(NewMetamaskCreatedUserListener.class);

  private WalletAccountService       walletAccountService;

  public NewMetamaskCreatedUserListener(WalletAccountService walletAccountService) {
    this.walletAccountService = walletAccountService;
  }

  @Override
  public void postSave(User user, boolean isNew) {
    String username  = user.getUserName();
    if (!isNew || user == null || !user.isEnabled() || !WalletUtils.isValidAddress(username)) {
      return;
    }
    Wallet wallet = walletAccountService.getWalletByAddress(username);
    if (wallet != null) {
      LOG.info("wallet with same address already exists");
      return;
    }
    try {
      Identity identity = getIdentityByUsername(username);
      wallet = walletAccountService.createWalletInstance(WalletProvider.METAMASK, username, Long.valueOf(identity.getId()));
      walletAccountService.saveWallet(wallet, true);
    } catch (Exception e) {
      LOG.error("Error while associating Metamask wallet for user {}", username, e);
    }
  }
}
