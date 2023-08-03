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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.wallet.service.WalletAccountService;

@RunWith(MockitoJUnitRunner.class)
public class TenantManagerServiceTest {

  protected static final Log LOG = ExoLogger.getLogger(TenantManagerServiceTest.class);

  @Mock
  HubService                 hubService;

  @Mock
  OrganizationService        organizationService;

  @Mock
  IdentityManager            identityManager;

  @Mock
  WalletAccountService       walletAccountService;

  TenantManagerService       tenantManagerService;

  @Test
  public void testGetTenantManagerDefaultRoles() {
    tenantManagerService = newTenantManagerService(null);
    List<String> tenantManagerDefaultRoles = tenantManagerService.getTenantManagerDefaultRoles();
    assertNotNull(tenantManagerDefaultRoles);
    assertEquals(0, tenantManagerDefaultRoles.size());

    InitParams params = mock(InitParams.class);
    tenantManagerService = newTenantManagerService(params);
    tenantManagerDefaultRoles = tenantManagerService.getTenantManagerDefaultRoles();
    assertNotNull(tenantManagerDefaultRoles);
    assertEquals(0, tenantManagerDefaultRoles.size());

    String adminRole = "/platform/administrators";
    String rewardingRole = "/platform/rewarding";
    when(params.containsKey(MANAGER_DEFAULT_ROLES_PARAM)).thenReturn(true);
    ValuesParam managerDefaultRolesValues = new ValuesParam();
    managerDefaultRolesValues.setValues(Arrays.asList(adminRole, rewardingRole));
    when(params.getValuesParam(MANAGER_DEFAULT_ROLES_PARAM)).thenReturn(managerDefaultRolesValues);
    tenantManagerService = newTenantManagerService(params);
    tenantManagerDefaultRoles = tenantManagerService.getTenantManagerDefaultRoles();
    assertNotNull(tenantManagerDefaultRoles);
    assertEquals(adminRole, tenantManagerDefaultRoles.get(0));
    assertEquals(rewardingRole, tenantManagerDefaultRoles.get(1));

    List<String> tenantManagerDefaultRolesConstant = tenantManagerDefaultRoles;
    assertThrows(UnsupportedOperationException.class, () -> tenantManagerDefaultRolesConstant.add("test"));
  }

  private TenantManagerService newTenantManagerService(InitParams params) {
    return new TenantManagerService(hubService,
                                    organizationService,
                                    identityManager,
                                    walletAccountService,
                                    params);
  }

}
