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

import static io.meeds.tenant.metamask.service.TenantManagerService.MANAGER_DEFAULT_ROLES_PARAM;
import static io.meeds.tenant.metamask.service.TenantManagerService.NFT_ID_PARAM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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

import io.meeds.tenant.model.DeedTenantHost;

@RunWith(MockitoJUnitRunner.class)
public class TenantManagerServiceTest {

  protected static final Log LOG = ExoLogger.getLogger(TenantManagerServiceTest.class);

  TenantManagerService       tenantManagerService;
//
//  @Test
//  public void testStart() throws Exception {
//    String nftId = "nftId";
//    InitParams params = mock(InitParams.class);
//
//    when(params.containsKey(NFT_ID_PARAM)).thenReturn(true);
//    ValueParam nftIdValue = new ValueParam();
//    nftIdValue.setValue(nftId);
//    when(params.getValueParam(NFT_ID_PARAM)).thenReturn(nftIdValue);
//    tenantManagerService = new TenantManagerService(params);
//    tenantManagerService.start();
//
//    when(tenantManagerStorage.getDeedTenant(anyString())).thenThrow(new RuntimeException("FAKE Exception for testStart"));
//    when(tenantManagerStorage.isEnabled()).thenReturn(true);
//    tenantManagerService.start();
//    for (int i = 0; i < TenantManagerService.MAX_START_TENTATIVES; i++) {
//      verify(tenantManagerStorage, timeout(2000).atLeast(i)).isEnabled();
//      assertNull(tenantManagerService.getDeedTenant());
//    }
//    tenantManagerService.start();
//    verify(tenantManagerStorage, after(2000).times(TenantManagerService.MAX_START_TENTATIVES)).isEnabled();
//    assertNull(tenantManagerService.getDeedTenant());
//  }
//
//  @Test
//  public void testIsTenantManager() throws Exception {
//    String nftId = "nftId";
//    String walletAddress = "0xb82f8457fcf644803f4d74f677905f1d410cd395";
//
//    tenantManagerService = new TenantManagerService(tenantManagerStorage, null);
//    assertFalse(tenantManagerService.isTenantManager(walletAddress));
//    verifyNoInteractions(tenantManagerStorage);
//
//    InitParams params = mock(InitParams.class);
//    tenantManagerService = new TenantManagerService(tenantManagerStorage, params);
//    assertFalse(tenantManagerService.isTenantManager(walletAddress));
//    verifyNoInteractions(tenantManagerStorage);
//
//    when(params.containsKey(NFT_ID_PARAM)).thenReturn(true);
//    ValueParam nftIdValue = new ValueParam();
//    nftIdValue.setValue(nftId);
//    when(params.getValueParam(NFT_ID_PARAM)).thenReturn(nftIdValue);
//    tenantManagerService = new TenantManagerService(tenantManagerStorage, params);
//    assertFalse(tenantManagerService.isTenantManager(walletAddress));
//    verify(tenantManagerStorage, times(1)).isManagerAddress(nftId, walletAddress);
//
//    when(tenantManagerStorage.isManagerAddress(nftId, walletAddress)).thenReturn(true);
//    tenantManagerService = new TenantManagerService(tenantManagerStorage, params);
//    assertTrue(tenantManagerService.isTenantManager(walletAddress));
//  }
//
//  @Test
//  public void testIsTenantManagerBlockchainError() throws Exception {
//    String nftId = "0";
//    String walletAddress = "0xb82f8457fcf644803f4d74f677905f1d410cd395";
//    String walletManagerAddress = "0x93bcdc45f7e62f89a8e901dc4a0e2c6c427d9f25";
//
//    tenantManagerService = new TenantManagerService(tenantManagerStorage, null);
//
//    InitParams params = mock(InitParams.class);
//    when(params.containsKey(NFT_ID_PARAM)).thenReturn(true);
//    ValueParam nftIdValue = new ValueParam();
//    nftIdValue.setValue(nftId);
//    when(params.getValueParam(NFT_ID_PARAM)).thenReturn(nftIdValue);
//    tenantManagerService = new TenantManagerService(tenantManagerStorage, params);
//    assertFalse(tenantManagerService.isTenantManager(walletAddress));
//    when(tenantManagerStorage.isManagerAddress(nftId, walletManagerAddress)).thenReturn(true);
//    assertFalse(tenantManagerService.isTenantManager(walletAddress));
//    assertTrue(tenantManagerService.isTenantManager(walletManagerAddress));
//
//    DeedTenantHost deedTenant = new DeedTenantHost(Long.parseLong(nftId));
//    when(tenantManagerStorage.getDeedTenant(anyString())).thenReturn(deedTenant);
//    when(tenantManagerStorage.isEnabled()).thenReturn(true);
//    tenantManagerService.start();
//    verify(tenantManagerStorage, timeout(2000).times(1)).getDeedTenant(nftId);
//    assertFalse(tenantManagerService.isTenantManager(walletAddress));
//    assertNotNull(tenantManagerService.getDeedTenant());
//    assertTrue(tenantManagerService.isDeedTenant());
//
//    assertNull(tenantManagerService.getDeedTenant().getManagerAddress());
//    boolean isTenantManager = tenantManagerService.isTenantManager(walletManagerAddress);
//    assertTrue(isTenantManager);
//    assertEquals(walletManagerAddress, tenantManagerService.getDeedTenant().getManagerAddress());
//
//    when(tenantManagerStorage.isManagerAddress(nftId,
//                                               walletManagerAddress)).thenThrow(new RuntimeException("FAKE Exception for testIsTenantManagerBlockchainError"));
//    assertTrue(tenantManagerService.isTenantManager(walletManagerAddress));
//    assertEquals(walletManagerAddress, tenantManagerService.getDeedTenant().getManagerAddress());
//  }
//
//  @Test
//  public void testGetDeedTenant() throws Exception {
//    String nftId = "0";
//
//    tenantManagerService = new TenantManagerService(tenantManagerStorage, null);
//
//    assertEquals(-1, tenantManagerService.getNftId());
//    assertFalse(tenantManagerService.isDeedTenant());
//    assertNull(tenantManagerService.getDeedTenant());
//    tenantManagerService.start();
//    verify(tenantManagerStorage, never()).isEnabled();
//    verify(tenantManagerStorage, never()).getDeedTenant(anyString());
//    assertFalse(tenantManagerService.isDeedTenant());
//    assertNull(tenantManagerService.getDeedTenant());
//
//    InitParams params = mock(InitParams.class);
//    when(params.containsKey(NFT_ID_PARAM)).thenReturn(true);
//    ValueParam nftIdValue = new ValueParam();
//    nftIdValue.setValue(nftId);
//    when(params.getValueParam(NFT_ID_PARAM)).thenReturn(nftIdValue);
//    tenantManagerService = new TenantManagerService(tenantManagerStorage, params);
//    assertEquals(Long.parseLong(nftId), tenantManagerService.getNftId());
//    tenantManagerService.start();
//    verify(tenantManagerStorage, timeout(2000).times(1)).isEnabled();
//    assertFalse(tenantManagerService.isDeedTenant());
//    assertNull(tenantManagerService.getDeedTenant());
//
//    when(tenantManagerStorage.isEnabled()).thenReturn(true);
//    tenantManagerService.start();
//    verify(tenantManagerStorage, timeout(2000).times(1)).getDeedTenant(nftId);
//    assertFalse(tenantManagerService.isDeedTenant());
//    assertNull(tenantManagerService.getDeedTenant());
//
//    DeedTenantHost deedTenant = new DeedTenantHost(Long.parseLong(nftId));
//    when(tenantManagerStorage.getDeedTenant(anyString())).thenReturn(deedTenant);
//    tenantManagerService.start();
//    verify(tenantManagerStorage, timeout(2000).times(2)).getDeedTenant(nftId);
//    assertNotNull(tenantManagerService.getDeedTenant());
//    assertTrue(tenantManagerService.isDeedTenant());
//  }
//
//  @Test
//  public void testGetTenantManagerDefaultRoles() {
//    tenantManagerService = new TenantManagerService(tenantManagerStorage, null);
//    List<String> tenantManagerDefaultRoles = tenantManagerService.getTenantManagerDefaultRoles();
//    assertNotNull(tenantManagerDefaultRoles);
//    assertEquals(0, tenantManagerDefaultRoles.size());
//
//    InitParams params = mock(InitParams.class);
//    tenantManagerService = new TenantManagerService(tenantManagerStorage, params);
//    tenantManagerDefaultRoles = tenantManagerService.getTenantManagerDefaultRoles();
//    assertNotNull(tenantManagerDefaultRoles);
//    assertEquals(0, tenantManagerDefaultRoles.size());
//
//    String adminRole = "/platform/administrators";
//    String rewardingRole = "/platform/rewarding";
//    when(params.containsKey(MANAGER_DEFAULT_ROLES_PARAM)).thenReturn(true);
//    ValuesParam managerDefaultRolesValues = new ValuesParam();
//    managerDefaultRolesValues.setValues(Arrays.asList(adminRole, rewardingRole));
//    when(params.getValuesParam(MANAGER_DEFAULT_ROLES_PARAM)).thenReturn(managerDefaultRolesValues);
//    tenantManagerService = new TenantManagerService(tenantManagerStorage, params);
//    tenantManagerDefaultRoles = tenantManagerService.getTenantManagerDefaultRoles();
//    assertNotNull(tenantManagerDefaultRoles);
//    assertEquals(adminRole, tenantManagerDefaultRoles.get(0));
//    assertEquals(rewardingRole, tenantManagerDefaultRoles.get(1));
//
//    List<String> tenantManagerDefaultRolesConstant = tenantManagerDefaultRoles;
//    assertThrows(UnsupportedOperationException.class, () -> tenantManagerDefaultRolesConstant.add("test"));
//  }

}
