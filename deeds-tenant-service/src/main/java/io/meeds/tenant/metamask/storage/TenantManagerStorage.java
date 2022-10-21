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
package io.meeds.tenant.metamask.storage;

import java.math.BigInteger;

import org.apache.commons.lang3.StringUtils;
import org.web3j.abi.datatypes.Address;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.ReadonlyTransactionManager;
import org.web3j.tx.gas.StaticGasProvider;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import io.meeds.deeds.contract.Deed;
import io.meeds.deeds.contract.DeedTenantProvisioning;
import io.meeds.tenant.model.DeedTenant;

/**
 * A service that allows to detect Deed Tenant Manager address
 */
public class TenantManagerStorage {

  protected static final Log       LOG = ExoLogger.getLogger(TenantManagerStorage.class);

  protected Web3j                  web3j;

  protected DeedTenantProvisioning deedTenantProvisioning;

  protected Deed                   deed;

  private String                   networkUrl;

  private String                   tenantProvisioningAddress;

  private String                   deedAddress;

  public TenantManagerStorage(InitParams params) {
    if (params != null) {
      if (params.containsKey("networkUrl")) {
        this.networkUrl = params.getValueParam("networkUrl").getValue();
      }
      if (params.containsKey("tenantProvisioningAddress")) {
        this.tenantProvisioningAddress = params.getValueParam("tenantProvisioningAddress").getValue();
      }
    }
  }

  public boolean isManagerAddress(String nftId, String address) throws Exception {
    if (!isEnabled()) {
      return false;
    }
    try {
      return getTenantProvisioningContract().isProvisioningManager(address, new BigInteger(nftId)).send();
    } catch (Exception e) {
      LOG.warn("Error checking Tenant Provisioning Manager", e);
    }
    return false;
  }

  public DeedTenant getDeedTenant(String nftId) throws Exception {
    if (StringUtils.isBlank(nftId) || !isEnabled() || getDeedContract() == null || getTenantProvisioningContract() == null) {
      return null;
    }
    DeedTenant deedTenant = new DeedTenant(Long.parseLong(nftId));
    BigInteger cardTypeIndex = getDeedContract().cardType(new BigInteger(nftId)).send();
    if (cardTypeIndex != null) {
      deedTenant.setCardType(cardTypeIndex.shortValue());
    }
    BigInteger cityIndex = getDeedContract().cityIndex(new BigInteger(nftId)).send();
    if (cityIndex != null) {
      deedTenant.setCityIndex(cityIndex.shortValue());
    }
    Boolean isProvisioned = getTenantProvisioningContract().tenantStatus(new BigInteger(nftId)).send();
    if (isProvisioned != null) {
      deedTenant.setProvisioned(isProvisioned.booleanValue());
    }
    return deedTenant;
  }

  public boolean isEnabled() throws Exception {
    return StringUtils.isNotBlank(tenantProvisioningAddress) && StringUtils.isNotBlank(networkUrl)
        && StringUtils.isNotBlank(getDeedAddress()) && getDeedContract() != null && getTenantProvisioningContract() != null;
  }

  private String getDeedAddress() throws Exception {
    if (StringUtils.isBlank(deedAddress)) {
      deedAddress = getTenantProvisioningContract().deed().send();
    }
    return deedAddress;
  }

  private DeedTenantProvisioning getTenantProvisioningContract() {
    if (deedTenantProvisioning == null) {
      BigInteger gasPrice = BigInteger.valueOf(20000000000l);
      BigInteger gasLimit = BigInteger.valueOf(300000l);

      Web3j web3jInstance = getWeb3j();
      deedTenantProvisioning = DeedTenantProvisioning.load(tenantProvisioningAddress,
                                                           web3jInstance,
                                                           new ReadonlyTransactionManager(web3jInstance,
                                                                                          Address.DEFAULT.toString()),
                                                           new StaticGasProvider(gasPrice, gasLimit));
    }
    return deedTenantProvisioning;
  }

  private Deed getDeedContract() throws Exception {
    if (deed == null) {
      BigInteger gasPrice = BigInteger.valueOf(20000000000l);
      BigInteger gasLimit = BigInteger.valueOf(300000l);

      Web3j web3jInstance = getWeb3j();
      deed = Deed.load(getDeedAddress(),
                       web3jInstance,
                       new ReadonlyTransactionManager(web3jInstance, Address.DEFAULT.toString()),
                       new StaticGasProvider(gasPrice, gasLimit));
    }
    return deed;
  }

  private Web3j getWeb3j() {
    if (web3j == null) {
      web3j = Web3j.build(new HttpService(networkUrl));
    }
    return web3j;
  }

}
