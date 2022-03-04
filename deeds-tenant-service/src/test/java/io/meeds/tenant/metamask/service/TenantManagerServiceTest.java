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
package io.meeds.tenant.metamask.service;

import static io.meeds.tenant.metamask.service.TenantManagerService.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.container.xml.*;

import io.meeds.tenant.metamask.storage.TenantManagerStorage;

@RunWith(MockitoJUnitRunner.class)
public class TenantManagerServiceTest {

  @Mock
  TenantManagerStorage tenantManagerStorage;

  TenantManagerService tenantManagerService;

  @Before
  public void setUp() {
    reset(tenantManagerStorage);
  }

  @Test
  public void testServerStartup() {
    String nftId = "nftId";

    tenantManagerService = new TenantManagerService(tenantManagerStorage, null);
    tenantManagerService.start();
    verifyNoInteractions(tenantManagerStorage);

    InitParams params = mock(InitParams.class);
    tenantManagerService = new TenantManagerService(tenantManagerStorage, params);
    tenantManagerService.start();
    verifyNoInteractions(tenantManagerStorage);

    when(params.containsKey(NFT_ID_PARAM)).thenReturn(true);
    ValueParam nftIdValue = new ValueParam();
    nftIdValue.setValue(nftId);
    when(params.getValueParam(NFT_ID_PARAM)).thenReturn(nftIdValue);
    tenantManagerService = new TenantManagerService(tenantManagerStorage, params);
    tenantManagerService.start();
    verify(tenantManagerStorage, times(1)).setTenantStatus(nftId, TENANT_STATUS_UP);
  }

  @Test
  public void testServerShutdown() {
    String nftId = "nftId";

    tenantManagerService = new TenantManagerService(tenantManagerStorage, null);
    tenantManagerService.stop();
    verifyNoInteractions(tenantManagerStorage);

    InitParams params = mock(InitParams.class);
    tenantManagerService = new TenantManagerService(tenantManagerStorage, params);
    tenantManagerService.stop();
    verifyNoInteractions(tenantManagerStorage);

    when(params.containsKey(NFT_ID_PARAM)).thenReturn(true);
    ValueParam nftIdValue = new ValueParam();
    nftIdValue.setValue(nftId);
    when(params.getValueParam(NFT_ID_PARAM)).thenReturn(nftIdValue);
    tenantManagerService = new TenantManagerService(tenantManagerStorage, params);
    tenantManagerService.stop();
    verify(tenantManagerStorage, times(1)).setTenantStatus(nftId, TENANT_STATUS_DOWN);
  }

  @Test
  public void testIsTenantManager() {
    String nftId = "nftId";
    String walletAddress = "walletAddress";

    tenantManagerService = new TenantManagerService(tenantManagerStorage, null);
    assertFalse(tenantManagerService.isTenantManager(walletAddress));
    verifyNoInteractions(tenantManagerStorage);

    InitParams params = mock(InitParams.class);
    tenantManagerService = new TenantManagerService(tenantManagerStorage, params);
    assertFalse(tenantManagerService.isTenantManager(walletAddress));
    verifyNoInteractions(tenantManagerStorage);

    when(params.containsKey(NFT_ID_PARAM)).thenReturn(true);
    ValueParam nftIdValue = new ValueParam();
    nftIdValue.setValue(nftId);
    when(params.getValueParam(NFT_ID_PARAM)).thenReturn(nftIdValue);
    tenantManagerService = new TenantManagerService(tenantManagerStorage, params);
    assertFalse(tenantManagerService.isTenantManager(walletAddress));
    verify(tenantManagerStorage, times(1)).getManagerAddress(nftId);

    when(tenantManagerStorage.getManagerAddress(nftId)).thenReturn(walletAddress.toUpperCase());
    tenantManagerService = new TenantManagerService(tenantManagerStorage, params);
    assertTrue(tenantManagerService.isTenantManager(walletAddress));
  }

  @Test
  public void testGetTenantManagerDefaultRoles() {
    tenantManagerService = new TenantManagerService(tenantManagerStorage, null);
    List<String> tenantManagerDefaultRoles = tenantManagerService.getTenantManagerDefaultRoles();
    assertNotNull(tenantManagerDefaultRoles);
    assertEquals(0, tenantManagerDefaultRoles.size());

    InitParams params = mock(InitParams.class);
    tenantManagerService = new TenantManagerService(tenantManagerStorage, params);
    tenantManagerDefaultRoles = tenantManagerService.getTenantManagerDefaultRoles();
    assertNotNull(tenantManagerDefaultRoles);
    assertEquals(0, tenantManagerDefaultRoles.size());

    String adminRole = "/platform/administrators";
    String rewardingRole = "/platform/rewarding";
    when(params.containsKey(MANAGER_DEFAULT_ROLES_PARAM)).thenReturn(true);
    ValuesParam managerDefaultRolesValues = new ValuesParam();
    managerDefaultRolesValues.setValues(Arrays.asList(adminRole, rewardingRole));
    when(params.getValuesParam(MANAGER_DEFAULT_ROLES_PARAM)).thenReturn(managerDefaultRolesValues);
    tenantManagerService = new TenantManagerService(tenantManagerStorage, params);
    tenantManagerDefaultRoles = tenantManagerService.getTenantManagerDefaultRoles();
    assertNotNull(tenantManagerDefaultRoles);
    assertEquals(adminRole, tenantManagerDefaultRoles.get(0));
    assertEquals(rewardingRole, tenantManagerDefaultRoles.get(1));

    List<String> tenantManagerDefaultRolesConstant = tenantManagerDefaultRoles;
    assertThrows(UnsupportedOperationException.class, () -> tenantManagerDefaultRolesConstant.add("test"));
  }

  @Test
  public void testGetCityIndex() {
    String nftId = "nftId";

    tenantManagerService = new TenantManagerService(tenantManagerStorage, null);
    assertNull(tenantManagerService.getCityIndex());
    verifyNoInteractions(tenantManagerStorage);

    InitParams params = mock(InitParams.class);
    tenantManagerService = new TenantManagerService(tenantManagerStorage, params);
    assertNull(tenantManagerService.getCityIndex());
    verifyNoInteractions(tenantManagerStorage);

    when(params.containsKey(NFT_ID_PARAM)).thenReturn(true);
    ValueParam nftIdValue = new ValueParam();
    nftIdValue.setValue(nftId);
    when(params.getValueParam(NFT_ID_PARAM)).thenReturn(nftIdValue);
    tenantManagerService = new TenantManagerService(tenantManagerStorage, params);
    assertNull(tenantManagerService.getCityIndex());
    verify(tenantManagerStorage, times(1)).getCityIndex(nftId);

    String cityIndex = "0";
    when(tenantManagerStorage.getCityIndex(nftId)).thenReturn(cityIndex);
    tenantManagerService = new TenantManagerService(tenantManagerStorage, params);
    assertEquals(cityIndex, tenantManagerService.getCityIndex());
  }

  @Test
  public void testGetCardType() {
    String nftId = "nftId";

    tenantManagerService = new TenantManagerService(tenantManagerStorage, null);
    assertNull(tenantManagerService.getCardType());
    verifyNoInteractions(tenantManagerStorage);

    InitParams params = mock(InitParams.class);
    tenantManagerService = new TenantManagerService(tenantManagerStorage, params);
    assertNull(tenantManagerService.getCardType());
    verifyNoInteractions(tenantManagerStorage);

    when(params.containsKey(NFT_ID_PARAM)).thenReturn(true);
    ValueParam nftIdValue = new ValueParam();
    nftIdValue.setValue(nftId);
    when(params.getValueParam(NFT_ID_PARAM)).thenReturn(nftIdValue);
    tenantManagerService = new TenantManagerService(tenantManagerStorage, params);
    assertNull(tenantManagerService.getCardType());
    verify(tenantManagerStorage, times(1)).getCardType(nftId);

    String cardType = "0";
    when(tenantManagerStorage.getCardType(nftId)).thenReturn(cardType);
    tenantManagerService = new TenantManagerService(tenantManagerStorage, params);
    assertEquals(cardType, tenantManagerService.getCardType());
  }

}
