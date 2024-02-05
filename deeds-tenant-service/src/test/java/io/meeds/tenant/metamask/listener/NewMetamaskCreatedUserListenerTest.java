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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.impl.UserImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.wallet.model.Wallet;
import org.exoplatform.wallet.model.WalletProvider;
import org.exoplatform.wallet.service.WalletAccountService;

@SpringBootTest(classes = {
  NewMetamaskCreatedUserListener.class,
})
class NewMetamaskCreatedUserListenerTest {

  @MockBean
  OrganizationService            organizationService;

  @MockBean
  WalletAccountService           walletAccountService;

  @MockBean
  IdentityManager                identityManager;

  @Autowired
  NewMetamaskCreatedUserListener listener;

  @Test
  void testNewMetamaskCreatedUserListenerWhenWalletExists() throws Exception {
    String username = "0x8714924ADEdB61b790d639F19c3D6F0FE2Cb7576";
    Wallet wallet = new Wallet();

    when(walletAccountService.getWalletByAddress(username)).thenReturn(wallet);
    User user = new UserImpl(username);
    listener.postSave(user, true);
    verifyNoInteractions(identityManager);
    verify(walletAccountService, times(0)).saveWallet(any(), anyBoolean());
  }

  @Test
  void testNewMetamaskCreatedUserListener() throws Exception {
    String username = "0x8714924ADEdB61b790d639F19c3D6F0FE2Cb7576";
    String identityId = "2554";

    Identity identity = mock(Identity.class);
    when(identity.getId()).thenReturn(identityId);
    when(identityManager.getOrCreateUserIdentity(username)).thenReturn(identity);

    Wallet wallet = mock(Wallet.class);
    when(walletAccountService.createWalletInstance(WalletProvider.METAMASK,
                                                   username,
                                                   Long.valueOf(identity.getId()))).thenReturn(wallet);
    when(walletAccountService.saveWallet(any(), anyBoolean())).thenAnswer(invocation -> invocation.getArgument(0));

    User user = new UserImpl("not an address");
    listener.postSave(user, true);
    verify(walletAccountService, times(0)).saveWallet(any(), anyBoolean());

    user = new UserImpl(username);
    listener.postSave(user, false);
    verify(walletAccountService, times(0)).saveWallet(any(), anyBoolean());

    listener.postSave(user, true);
    verify(walletAccountService, times(1)).saveWallet(any(), anyBoolean());
  }

}
