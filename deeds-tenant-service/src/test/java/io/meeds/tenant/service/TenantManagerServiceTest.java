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
package io.meeds.tenant.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.meeds.deeds.constant.TenantProvisioningStatus;
import io.meeds.deeds.constant.TenantStatus;
import io.meeds.deeds.elasticsearch.model.DeedTenant;
import io.meeds.deeds.service.ListenerService;
import io.meeds.deeds.service.TenantService;
import io.meeds.tenant.model.DeedTenantHost;

@SpringBootTest(classes = {
                            TenantManagerService.class,
})
public class TenantManagerServiceTest {

  protected static final Log LOG = ExoLogger.getLogger(TenantManagerServiceTest.class);

  @MockBean
  private TenantService      tenantService;

  @MockBean
  private ListenerService    listenerService;

  @Autowired
  TenantManagerService       tenantManagerService;

  @BeforeEach
  public void teardown() {
    DeedTenantHost.clear();
  }

  @Test
  public void testStart() {
    tenantManagerService.start();
    assertNull(DeedTenantHost.getInstance());

    assertEquals(-1, tenantManagerService.getNftId());
    tenantManagerService.setNftId(3l);
    try {
      assertThrows(IllegalStateException.class, () -> tenantManagerService.start());
      assertNull(DeedTenantHost.getInstance());

      assertThrows(IllegalStateException.class, () -> tenantManagerService.start());

      when(tenantService.getDeedTenant(tenantManagerService.getNftId())).thenReturn(new DeedTenant(tenantManagerService.getNftId(),
                                                                                                   (short) 0,
                                                                                                   (short) 0,
                                                                                                   null,
                                                                                                   null,
                                                                                                   null,
                                                                                                   null,
                                                                                                   null,
                                                                                                   TenantProvisioningStatus.STOP_CONFIRMED,
                                                                                                   TenantStatus.UNDEPLOYED,
                                                                                                   false,
                                                                                                   null,
                                                                                                   null));
      tenantManagerService.start();
      assertNotNull(DeedTenantHost.getInstance());
    } finally {
      tenantManagerService.setNftId(-1);
    }
  }

  @Test
  public void testIsTenantManager() {
    long nftId = 2l;
    String walletAddress = "0xb82f8457fcf644803f4d74f677905f1d410cd395";

    assertFalse(tenantManagerService.isTenant());
    assertFalse(tenantManagerService.isTenantManager(walletAddress));

    tenantManagerService.setNftId(nftId);
    assertTrue(tenantManagerService.isTenant());
    assertFalse(tenantManagerService.isTenantManager(walletAddress));
    assertFalse(tenantManagerService.isTenantManager(walletAddress));

    DeedTenantHost.setInstance(2l, (short) 0, (short) 0, false, "anotherWalletAddress", "managerWalletAddress");
    assertTrue(tenantManagerService.isTenant());
    assertFalse(tenantManagerService.isTenantManager(walletAddress));

    DeedTenantHost.setInstance(2l, (short) 0, (short) 0, false, walletAddress, "managerWalletAddress");
    assertTrue(tenantManagerService.isTenant());
    assertTrue(tenantManagerService.isTenantManager(walletAddress));
  }

}
