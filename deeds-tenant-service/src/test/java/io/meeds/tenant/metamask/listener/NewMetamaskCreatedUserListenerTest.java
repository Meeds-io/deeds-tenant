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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.impl.UserImpl;

import io.meeds.tenant.service.TenantManagerService;

@ExtendWith(MockitoExtension.class)
public class NewMetamaskCreatedUserListenerTest {

  @Mock
  TenantManagerService tenantManagerService;

  @Test
  public void testNewMetamaskCreatedUserListener() throws Exception {
    String username = "0x8714924ADEdB61b790d639F19c3D6F0FE2Cb7576";
    NewMetamaskCreatedUserListener listener = new NewMetamaskCreatedUserListener(tenantManagerService);

    User user = new UserImpl("not an address");
    listener.postSave(user, true);
    verify(tenantManagerService, times(0)).createUserWalletByAddress(any());

    user = new UserImpl(username);
    listener.postSave(user, false);
    verify(tenantManagerService, times(0)).createUserWalletByAddress(any());

    listener.postSave(user, true);
    verify(tenantManagerService, times(1)).createUserWalletByAddress(any());
  }

}
