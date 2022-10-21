/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2020 - 2022 Meeds Association contact@meeds.io
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
package io.meeds.tenant.metamask.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigInteger;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteFunctionCall;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;

import io.meeds.deeds.contract.Deed;
import io.meeds.deeds.contract.DeedTenantProvisioning;
import io.meeds.tenant.model.DeedTenant;

@RunWith(MockitoJUnitRunner.class)
public class TenantManagerStorageTest {

  private static final String    TENANT_PROVISIONING_ADDRESS = "tenantProvisioningAddress";

  private static final String    NETWORK_URL                 = "networkUrl";

  private static final String    DEED_ADDRESS                = "deedAddress";

  private static final String    NFT_ID                      = "35422";

  private static final String    MANAGER_ADDRESS             = "managerAddress";

  @Mock
  private Web3j                  web3j;

  @Mock
  private DeedTenantProvisioning deedTenantProvisioning;

  @Mock
  private Deed                   deed;

  private TenantManagerStorage   tenantManagerStorage;

  @Before
  public void setup() {
    InitParams params = mock(InitParams.class);
    when(params.containsKey(NETWORK_URL)).thenReturn(true);
    ValueParam networkUrlvalue = new ValueParam();
    networkUrlvalue.setValue(NETWORK_URL);
    when(params.getValueParam(NETWORK_URL)).thenReturn(networkUrlvalue);
    when(params.containsKey(TENANT_PROVISIONING_ADDRESS)).thenReturn(true);
    ValueParam tenantProvisioningAddressValue = new ValueParam();
    when(params.getValueParam(TENANT_PROVISIONING_ADDRESS)).thenReturn(tenantProvisioningAddressValue);
    tenantProvisioningAddressValue.setValue(TENANT_PROVISIONING_ADDRESS);
    this.tenantManagerStorage = new TenantManagerStorage(params);
  }

  @Test
  public void testIsEnabledFalse() throws Exception {
    assertThrows(Exception.class, () -> tenantManagerStorage.isEnabled());
    setServices();
    assertFalse(tenantManagerStorage.isEnabled());
  }

  @Test
  public void testIsEnabledTrue() throws Exception {
    setEnabled();
    assertTrue(tenantManagerStorage.isEnabled());
  }

  @Test
  public void testIsManagerAddressWhenNotEnabled() throws Exception {
    assertThrows(Exception.class, () -> tenantManagerStorage.isManagerAddress(NFT_ID, MANAGER_ADDRESS));
    setServices();
    assertFalse(tenantManagerStorage.isManagerAddress(NFT_ID, MANAGER_ADDRESS));
  }

  @Test
  public void testIsManagerAddress() throws Exception {
    setEnabled();
    assertFalse(tenantManagerStorage.isManagerAddress(NFT_ID, MANAGER_ADDRESS));

    RemoteFunctionCall<Boolean> mockRemoteCall = mockRemoteCall(true);
    when(deedTenantProvisioning.isProvisioningManager(MANAGER_ADDRESS, new BigInteger(NFT_ID))).thenReturn(mockRemoteCall);
    assertTrue(tenantManagerStorage.isManagerAddress(NFT_ID, MANAGER_ADDRESS));
  }

  @Test
  public void testGetDeedTenantWhenNotEnabled() throws Exception {
    assertThrows(Exception.class, () -> tenantManagerStorage.getDeedTenant(NFT_ID));
    setServices();
    assertNull(tenantManagerStorage.getDeedTenant(NFT_ID));
  }

  @Test
  public void testGetDeedTenant() throws Exception {
    setServices();
    DeedTenant deedTenant = tenantManagerStorage.getDeedTenant(NFT_ID);
    assertNull(deedTenant);

    setEnabled();
    deedTenant = tenantManagerStorage.getDeedTenant(NFT_ID);
    assertNotNull(deedTenant);
    assertEquals(Long.parseLong(NFT_ID), deedTenant.getNftId());
    assertEquals(-1, deedTenant.getCityIndex());
    assertEquals(-1, deedTenant.getCardType());
    assertFalse(deedTenant.isProvisioned());
    assertNull(deedTenant.getManagerAddress());

    BigInteger cardType = BigInteger.valueOf(20);
    RemoteFunctionCall<BigInteger> cardTypeMockRemoteCall = mockRemoteCall(cardType);
    when(deed.cardType(new BigInteger(NFT_ID))).thenReturn(cardTypeMockRemoteCall);
    BigInteger cityIndex = BigInteger.valueOf(10);
    RemoteFunctionCall<BigInteger> cityIndexMockRemoteCall = mockRemoteCall(cityIndex);
    when(deed.cityIndex(new BigInteger(NFT_ID))).thenReturn(cityIndexMockRemoteCall);
    Boolean provisioned = Boolean.TRUE;
    RemoteFunctionCall<Boolean> provisionedMockRemoteCall = mockRemoteCall(provisioned);
    when(deedTenantProvisioning.tenantStatus(new BigInteger(NFT_ID))).thenReturn(provisionedMockRemoteCall);

    deedTenant = tenantManagerStorage.getDeedTenant(NFT_ID);
    assertEquals(cardType.shortValue(), deedTenant.getCardType());
    assertEquals(cityIndex.shortValue(), deedTenant.getCityIndex());
    assertEquals(provisioned, deedTenant.isProvisioned());
    assertEquals(Long.parseLong(NFT_ID), deedTenant.getNftId());
  }

  private void setEnabled() throws Exception {
    setServices();
    RemoteFunctionCall<String> value = mockRemoteCall(DEED_ADDRESS);
    when(deedTenantProvisioning.deed()).thenReturn(value);
  }

  private void setServices() throws Exception {
    this.tenantManagerStorage.web3j = web3j;
    this.tenantManagerStorage.deed = deed;
    this.tenantManagerStorage.deedTenantProvisioning = deedTenantProvisioning;
    RemoteFunctionCall<String> nullStringMockRemoteCall = mockRemoteCall((String) null);
    when(deedTenantProvisioning.deed()).thenReturn(nullStringMockRemoteCall);
    RemoteFunctionCall<BigInteger> nullBigIntegerMockRemoteCall = mockRemoteCall((BigInteger) null);
    when(deed.cardType(any(BigInteger.class))).thenReturn(nullBigIntegerMockRemoteCall);
    when(deed.cityIndex(any(BigInteger.class))).thenReturn(nullBigIntegerMockRemoteCall);
    RemoteFunctionCall<Boolean> falseBooleanMockRemoteCall = mockRemoteCall(Boolean.FALSE);
    when(deedTenantProvisioning.tenantStatus(any(BigInteger.class))).thenReturn(falseBooleanMockRemoteCall);
    when(deedTenantProvisioning.isProvisioningManager(anyString(), any(BigInteger.class))).thenReturn(falseBooleanMockRemoteCall);
  }

  private <T> RemoteFunctionCall<T> mockRemoteCall(T value) throws Exception {
    @SuppressWarnings("unchecked")
    RemoteFunctionCall<T> remoteFunctionCall = mock(RemoteFunctionCall.class);
    when(remoteFunctionCall.send()).thenReturn(value);
    return remoteFunctionCall;
  }

}
