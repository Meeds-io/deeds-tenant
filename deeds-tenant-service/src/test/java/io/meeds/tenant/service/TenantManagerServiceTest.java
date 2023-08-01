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
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.wallet.reward.service.RewardReportService;
import org.exoplatform.wallet.reward.service.RewardSettingsService;
import org.exoplatform.wallet.service.WalletAccountService;

import io.meeds.tenant.model.DeedTenantNft;
import io.meeds.tenant.plugin.WalletHubIdentityProvider;
import io.meeds.tenant.rest.client.TenantServiceConsumer;

@RunWith(MockitoJUnitRunner.class)
public class TenantManagerServiceTest {

  protected static final Log LOG = ExoLogger.getLogger(TenantManagerServiceTest.class);

  @Mock
  TenantServiceConsumer      tenantServiceConsumer;

  @Mock
  WalletAccountService       walletAccountService;

  @Mock
  IdentityManager            identityManager;

  @Mock
  OrganizationService        organizationService;

  @Mock
  RewardSettingsService      rewardSettingsService;

  @Mock
  RewardReportService        rewardReportService;

  TenantManagerService       tenantManagerService;

  @Test
  public void testIsTenantManager() throws Exception {
    String nftId = "2";
    String hubAddress = "0xa82f8457fcf644803f4d74f677905f1d410cd395";
    String walletAddress = "0xb82f8457fcf644803f4d74f677905f1d410cd395";

    tenantManagerService = newTenantManagerService(null);
    assertFalse(tenantManagerService.isTenant());
    assertFalse(tenantManagerService.isTenantManager(walletAddress));

    InitParams params = mock(InitParams.class);
    tenantManagerService = newTenantManagerService(params);
    assertFalse(tenantManagerService.isTenant());
    assertFalse(tenantManagerService.isTenantManager(walletAddress));

    Identity hubIdentity = mock(Identity.class);
    Profile hubProfile = mock(Profile.class);
    when(hubIdentity.getProfile()).thenReturn(hubProfile);
    when(hubProfile.getProperty(WalletHubIdentityProvider.DEED_ID)).thenReturn(nftId);
    when(hubProfile.getProperty(WalletHubIdentityProvider.ADDRESS)).thenReturn(hubAddress);
    when(identityManager.getOrCreateIdentity(WalletHubIdentityProvider.PROVIDER_NAME, WalletHubIdentityProvider.ID)).thenReturn(hubIdentity);
    when(tenantServiceConsumer.getDeedTenant(Long.parseLong(nftId))).thenReturn(new DeedTenantNft(Long.parseLong(nftId),
                                                                                                  (short) 1,
                                                                                                  (short) 2));

    tenantManagerService = newTenantManagerService(params);
    assertTrue(tenantManagerService.isTenant());
    assertFalse(tenantManagerService.isTenantManager(walletAddress));
    assertFalse(tenantManagerService.isTenantManager(walletAddress));

    tenantManagerService = newTenantManagerService(params);
    assertTrue(tenantManagerService.isTenant());
    assertFalse(tenantManagerService.isTenantManager(walletAddress));

    when(tenantServiceConsumer.isDeedManager(walletAddress, Long.parseLong(nftId))).thenReturn(true);
    tenantManagerService = newTenantManagerService(params);
    assertTrue(tenantManagerService.isTenant());
    assertTrue(tenantManagerService.isTenantManager(walletAddress));
  }

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
    return new TenantManagerService(identityManager,
                                    walletAccountService,
                                    rewardSettingsService,
                                    rewardReportService,
                                    organizationService,
                                    tenantServiceConsumer,
                                    params);
  }

}
