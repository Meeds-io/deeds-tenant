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

import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.impl.UserImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.model.WalletProvider;
import org.exoplatform.wallet.service.WalletAccountService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class NewMetamaskCreatedUserListenerTest {

  @Mock
  IdentityManager      identityManager;

  @Mock
  WalletAccountService walletAccountService;

  @Test
  public void testNewMetamaskCreatedUserListener() throws IllegalAccessException {
    String username = "0x8714924ADEdB61b790d639F19c3D6F0FE2Cb7576";
    NewMetamaskCreatedUserListener listener = new NewMetamaskCreatedUserListener(identityManager, walletAccountService);

    User user = new UserImpl("not an address");
    listener.postSave(user, true);
    verify(walletAccountService, times(0)).saveWallet(any(Wallet.class), anyBoolean());

    user = new UserImpl(username);
    listener.postSave(user, false);
    verify(walletAccountService, times(0)).saveWallet(any(Wallet.class), anyBoolean());

    Wallet wallet = new Wallet();
    wallet.setAddress(username);
    Identity userIdentity = new Identity();
    userIdentity.setId(String.valueOf(1l));

    when(walletAccountService.getWalletByAddress(username)).thenReturn(wallet);
    listener.postSave(user, true);
    verify(walletAccountService, times(0)).saveWallet(any(Wallet.class), anyBoolean());

    when(walletAccountService.getWalletByAddress(username)).thenReturn(null);
    when(walletAccountService.createWalletInstance(WalletProvider.METAMASK,
            username,
            1l)).thenReturn(wallet);
    when(identityManager.getOrCreateUserIdentity(username)).thenReturn(userIdentity);

    listener.postSave(user, true);
    verify(walletAccountService, times(1)).saveWallet(any(Wallet.class), anyBoolean());
  }
}
