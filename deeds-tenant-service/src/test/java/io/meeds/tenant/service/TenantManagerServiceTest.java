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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.meeds.tenant.integration.service.TenantServiceFacade;
import io.meeds.tenant.model.DeedTenantHost;

@RunWith(MockitoJUnitRunner.class)
public class TenantManagerServiceTest {

  protected static final Log LOG = ExoLogger.getLogger(TenantManagerServiceTest.class);

  TenantManagerService       tenantManagerService;

  @Mock
  TenantServiceFacade        tenantServiceSkeleton;

  @Before
  public void teardown() throws Exception {
    DeedTenantHost.clear();
  }

  @Test
  public void testStart() throws Exception {
    tenantManagerService = new TenantManagerService(null);
    tenantManagerService.start();
    assertNull(DeedTenantHost.getInstance());

    String nftId = "3";

    InitParams params = mock(InitParams.class);
    when(params.containsKey(NFT_ID_PARAM)).thenReturn(true);
    ValueParam nftIdValue = new ValueParam();
    nftIdValue.setValue(nftId);
    when(params.getValueParam(NFT_ID_PARAM)).thenReturn(nftIdValue);

    tenantManagerService = new TenantManagerService(params);
    assertThrows(IllegalStateException.class, () -> tenantManagerService.start());
    assertNull(DeedTenantHost.getInstance());

    tenantManagerService.tenantServiceSkeleton = tenantServiceSkeleton;
    assertThrows(IllegalStateException.class, () -> tenantManagerService.start());

    when(tenantServiceSkeleton.getDeedTenant(tenantManagerService.getNftId())).thenAnswer(invocation -> {
      return DeedTenantHost.setInstance(tenantManagerService.getNftId(), (short) 0, (short) 0, false, null, null);
    });
    tenantManagerService.start();
    assertNotNull(DeedTenantHost.getInstance());
  }

  @Test
  public void testIsTenantManager() throws Exception {
    String nftId = "2";
    String walletAddress = "0xb82f8457fcf644803f4d74f677905f1d410cd395";

    tenantManagerService = new TenantManagerService(null);
    assertFalse(tenantManagerService.isTenant());
    assertFalse(tenantManagerService.isTenantManager(walletAddress));

    InitParams params = mock(InitParams.class);
    tenantManagerService = new TenantManagerService(params);
    assertFalse(tenantManagerService.isTenant());
    assertFalse(tenantManagerService.isTenantManager(walletAddress));

    when(params.containsKey(NFT_ID_PARAM)).thenReturn(true);
    ValueParam nftIdValue = new ValueParam();
    nftIdValue.setValue(nftId);
    when(params.getValueParam(NFT_ID_PARAM)).thenReturn(nftIdValue);
    tenantManagerService = new TenantManagerService(params);
    assertTrue(tenantManagerService.isTenant());
    assertThrows(IllegalStateException.class, () -> tenantManagerService.isTenantManager(walletAddress));
    tenantManagerService.tenantServiceSkeleton = tenantServiceSkeleton;
    assertFalse(tenantManagerService.isTenantManager(walletAddress));

    DeedTenantHost.setInstance(2l, (short) 0, (short) 0, false, "anotherWalletAddress", "managerWalletAddress");
    tenantManagerService = new TenantManagerService(params);
    assertTrue(tenantManagerService.isTenant());
    assertFalse(tenantManagerService.isTenantManager(walletAddress));

    DeedTenantHost.setInstance(2l, (short) 0, (short) 0, false, walletAddress, "managerWalletAddress");
    tenantManagerService = new TenantManagerService(params);
    assertTrue(tenantManagerService.isTenant());
    assertTrue(tenantManagerService.isTenantManager(walletAddress));
  }

  @Test
  public void testGetTenantManagerDefaultRoles() {
    tenantManagerService = new TenantManagerService(null);
    List<String> tenantManagerDefaultRoles = tenantManagerService.getTenantManagerDefaultRoles();
    assertNotNull(tenantManagerDefaultRoles);
    assertEquals(0, tenantManagerDefaultRoles.size());

    InitParams params = mock(InitParams.class);
    tenantManagerService = new TenantManagerService(params);
    tenantManagerDefaultRoles = tenantManagerService.getTenantManagerDefaultRoles();
    assertNotNull(tenantManagerDefaultRoles);
    assertEquals(0, tenantManagerDefaultRoles.size());

    String adminRole = "/platform/administrators";
    String rewardingRole = "/platform/rewarding";
    when(params.containsKey(MANAGER_DEFAULT_ROLES_PARAM)).thenReturn(true);
    ValuesParam managerDefaultRolesValues = new ValuesParam();
    managerDefaultRolesValues.setValues(Arrays.asList(adminRole, rewardingRole));
    when(params.getValuesParam(MANAGER_DEFAULT_ROLES_PARAM)).thenReturn(managerDefaultRolesValues);
    tenantManagerService = new TenantManagerService(params);
    tenantManagerDefaultRoles = tenantManagerService.getTenantManagerDefaultRoles();
    assertNotNull(tenantManagerDefaultRoles);
    assertEquals(adminRole, tenantManagerDefaultRoles.get(0));
    assertEquals(rewardingRole, tenantManagerDefaultRoles.get(1));

    List<String> tenantManagerDefaultRolesConstant = tenantManagerDefaultRoles;
    assertThrows(UnsupportedOperationException.class, () -> tenantManagerDefaultRolesConstant.add("test"));
  }

}
