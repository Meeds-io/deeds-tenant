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

import org.exoplatform.services.organization.impl.UserImpl;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.wallet.service.WalletAccountService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NewMetamaskCreatedUserListenerTest {

  @Mock
  IdentityManager      identityManager;

  @Mock
  WalletAccountService walletAccountService;

  @Test
  public void testNewMetamaskCreatedUserListener() throws Exception {
    String username = "0x29H59f54055966197fC2442Df38B6C980ff56585";
    NewMetamaskCreatedUserListener listener = new NewMetamaskCreatedUserListener(identityManager, walletAccountService);

    listener.postSave(new UserImpl(username), false);

    // TODO
  }
}
