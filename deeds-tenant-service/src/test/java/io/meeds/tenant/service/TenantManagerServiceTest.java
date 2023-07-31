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

import static io.meeds.tenant.service.TenantManagerService.MANAGER_DEFAULT_ROLES_PARAM;
import static io.meeds.tenant.service.TenantManagerService.NFT_ID_PARAM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wallet.service.WalletAccountService;

import io.meeds.tenant.model.DeedTenantHub;
import io.meeds.tenant.rest.client.TenantServiceConsumer;

@RunWith(MockitoJUnitRunner.class)
public class TenantManagerServiceTest {

  protected static final Log LOG = ExoLogger.getLogger(TenantManagerServiceTest.class);

  @Mock
  TenantServiceConsumer      tenantServiceConsumer;

  @Mock
  WalletAccountService       walletAccountService;

  TenantManagerService       tenantManagerService;

  @Test
  public void testIsTenantManager() throws Exception {
    String nftId = "2";
    String walletAddress = "0xb82f8457fcf644803f4d74f677905f1d410cd395";

    tenantManagerService = new TenantManagerService(walletAccountService, tenantServiceConsumer, null);
    assertFalse(tenantManagerService.isTenant());
    assertFalse(tenantManagerService.isTenantManager(walletAddress));

    InitParams params = mock(InitParams.class);
    tenantManagerService = new TenantManagerService(walletAccountService, tenantServiceConsumer, params);
    assertFalse(tenantManagerService.isTenant());
    assertFalse(tenantManagerService.isTenantManager(walletAddress));

    when(params.containsKey(NFT_ID_PARAM)).thenReturn(true);
    ValueParam nftIdValue = new ValueParam();
    nftIdValue.setValue(nftId);
    when(params.getValueParam(NFT_ID_PARAM)).thenReturn(nftIdValue);

    when(tenantServiceConsumer.getDeedTenant(Long.parseLong(nftId))).thenReturn(new DeedTenantHub(Long.parseLong(nftId),
                                                                                                  (short) 1,
                                                                                                  (short) 2));

    tenantManagerService = new TenantManagerService(walletAccountService, tenantServiceConsumer, params);
    assertTrue(tenantManagerService.isTenant());
    assertFalse(tenantManagerService.isTenantManager(walletAddress));
    assertFalse(tenantManagerService.isTenantManager(walletAddress));

    tenantManagerService = new TenantManagerService(walletAccountService, tenantServiceConsumer, params);
    assertTrue(tenantManagerService.isTenant());
    assertFalse(tenantManagerService.isTenantManager(walletAddress));

    when(tenantServiceConsumer.isDeedManager(walletAddress, Long.parseLong(nftId))).thenReturn(true);
    tenantManagerService = new TenantManagerService(walletAccountService, tenantServiceConsumer, params);
    assertTrue(tenantManagerService.isTenant());
    assertTrue(tenantManagerService.isTenantManager(walletAddress));
  }

  @Test
  public void testGetTenantManagerDefaultRoles() {
    tenantManagerService = new TenantManagerService(walletAccountService, tenantServiceConsumer, null);
    List<String> tenantManagerDefaultRoles = tenantManagerService.getTenantManagerDefaultRoles();
    assertNotNull(tenantManagerDefaultRoles);
    assertEquals(0, tenantManagerDefaultRoles.size());

    InitParams params = mock(InitParams.class);
    tenantManagerService = new TenantManagerService(walletAccountService, tenantServiceConsumer, params);
    tenantManagerDefaultRoles = tenantManagerService.getTenantManagerDefaultRoles();
    assertNotNull(tenantManagerDefaultRoles);
    assertEquals(0, tenantManagerDefaultRoles.size());

    String adminRole = "/platform/administrators";
    String rewardingRole = "/platform/rewarding";
    when(params.containsKey(MANAGER_DEFAULT_ROLES_PARAM)).thenReturn(true);
    ValuesParam managerDefaultRolesValues = new ValuesParam();
    managerDefaultRolesValues.setValues(Arrays.asList(adminRole, rewardingRole));
    when(params.getValuesParam(MANAGER_DEFAULT_ROLES_PARAM)).thenReturn(managerDefaultRolesValues);
    tenantManagerService = new TenantManagerService(walletAccountService, tenantServiceConsumer, params);
    tenantManagerDefaultRoles = tenantManagerService.getTenantManagerDefaultRoles();
    assertNotNull(tenantManagerDefaultRoles);
    assertEquals(adminRole, tenantManagerDefaultRoles.get(0));
    assertEquals(rewardingRole, tenantManagerDefaultRoles.get(1));

    List<String> tenantManagerDefaultRolesConstant = tenantManagerDefaultRoles;
    assertThrows(UnsupportedOperationException.class, () -> tenantManagerDefaultRolesConstant.add("test"));
  }

}
