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

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.picketlink.idm.api.SecureRandomProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.crypto.Sign.SignatureData;
import org.web3j.utils.Numeric;

import org.exoplatform.account.setup.web.AccountSetupService;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;

import io.meeds.common.ContainerTransactional;
import io.meeds.portal.security.constant.UserRegistrationType;
import io.meeds.portal.security.service.SecuritySettingService;
import io.meeds.tenant.hub.service.HubService;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;
import lombok.Setter;

@Service
public class MetamaskLoginService {

  public static final String     LOGIN_MESSAGE_ATTRIBUTE_NAME = "metamask_login_message";

  private static final Log       LOG                          = ExoLogger.getLogger(MetamaskLoginService.class);

  @Autowired
  private SecuritySettingService securitySettingService;

  @Autowired
  private OrganizationService    organizationService;

  @Autowired
  private UserACL                userAcl;

  @Autowired
  private SecureRandomProvider   secureRandomProvider;

  @Autowired
  private AccountSetupService    accountSetupService;

  @Autowired
  private HubService             hubService;

  @Setter
  @Value("${meeds.login.metamask.secureRootAccessWithMetamask:true}")
  private boolean                secureRootAccessWithMetamask;

  @Setter
  @Value("#{'${meeds.login.metamask.allowedRootAccessWallets:}'.split(',')}")
  private List<String>           allowedRootWallets           = new ArrayList<>();

  @PostConstruct
  @ContainerTransactional
  public void init() {
    if (this.secureRootAccessWithMetamask) {
      // Avoid allowing to change root password
      accountSetupService.setSkipSetup(true);
      // Generate a new random password for root user
      secureRootPassword();
    }
  }

  /**
   * @return allowUserRegistration parameter value
   */
  public boolean isAllowUserRegistration() {
    return securitySettingService.getRegistrationType() == UserRegistrationType.OPEN;
  }

  /**
   * @param walletAddress wallet address that attempts to register
   * @return allowUserRegistration parameter value, else, it will checks whether
   *         the Tenant Manager has been registered to the tenant or not. If not
   *         regitered, allow to display the register form, else return false.
   */
  public boolean isAllowUserRegistration(String walletAddress) {
    if (isAllowUserRegistration()) {
      return true;
    } else {
      return isDeedManager(walletAddress);
    }
  }

  /**
   * @param walletAddress to check if it's of Tenant Manager
   * @return true is wallet address is of the Tenant Manager else return false.
   */
  public boolean isDeedManager(String walletAddress) {
    return hubService.isDeedManager(walletAddress);
  }

  /**
   * @param walletAddress wallet address
   * @return true if secure root access is allowed and designated wallet is
   *         allowed to access using root account
   */
  public boolean isSuperUser(String walletAddress) {
    return secureRootAccessWithMetamask
           && allowedRootWallets != null
           && allowedRootWallets.stream().anyMatch(address -> StringUtils.equalsIgnoreCase(address, walletAddress));
  }

  /**
   * Retrieves User name with associated wallet Address
   * 
   * @param walletAddress Ethereum Wallet Address
   * @return username
   */
  public String getUserWithWalletAddress(String walletAddress) {
    if (isSuperUser(walletAddress)) {
      return userAcl.getSuperUser();
    }
    try {
      User user = organizationService.getUserHandler().findUserByName(walletAddress.toLowerCase());
      if (user != null) {
        return user.getUserName();
      }
    } catch (Exception e) {
      LOG.warn("Error retrieving username from walletAddress {}", walletAddress, e);
    }
    return null;
  }

  /**
   * Validates signed message by a wallet using Metamask
   * 
   * @param walletAddress wallet Address (wallet public key)
   * @param rawMessage raw signed message
   * @param signedMessage encrypted message
   * @return true if the message has been decrypted successfully, else false
   */
  public boolean validateSignedMessage(String walletAddress, String rawMessage, String signedMessage) {
    if (StringUtils.isBlank(walletAddress) || StringUtils.isBlank(rawMessage) || StringUtils.isBlank(signedMessage)) {
      return false;
    }

    try {
      byte[] signatureBytes = Numeric.hexStringToByteArray(signedMessage);
      if (signatureBytes.length < 64) {
        return false;
      }
      byte[] r = Arrays.copyOfRange(signatureBytes, 0, 32);
      byte[] s = Arrays.copyOfRange(signatureBytes, 32, 64);
      byte v = signatureBytes[64];
      if (v < 27) {
        v += 27;
      }

      BigInteger publicKey = Sign.signedPrefixedMessageToKey(rawMessage.getBytes(), new SignatureData(v, r, s));
      String recoveredAddress = "0x" + Keys.getAddress(publicKey);
      if (recoveredAddress.equalsIgnoreCase(walletAddress)) {
        return true;
      }
    } catch (Exception e) {
      LOG.warn("Error verifying signedPrefixed Message for wallet {}. Consider user as not authenticated.", walletAddress, e);
      return false;
    }
    return false;
  }

  /**
   * Generates a new Login Message to sign by current user and store it in
   * {@link HttpSession}. If renew = true, a new Token will be generated even if
   * alread exists in {@link HttpSession}, else the token already generated will
   * be returned
   * 
   * @param session {@link HttpSession}
   * @param renew boolean
   * @return already existing token in {@link HttpSession} or a newly generated
   *         one
   */
  public String generateLoginMessage(HttpSession session, boolean renew) {
    String token = getLoginMessage(session);
    if (token != null && !renew) {
      return token;
    }
    token = generateRandomToken();
    if (session != null) {
      session.setAttribute(LOGIN_MESSAGE_ATTRIBUTE_NAME, token);
    }
    return token;
  }

  /**
   * Generates a new Login Message to sign by current user and store it in
   * {@link HttpSession}. If a token already exists in session, it will be
   * returned else a newly generated token will be returned
   * 
   * @param session {@link HttpSession}
   * @return already existing token in {@link HttpSession} or a newly generated
   *         one
   */
  public String generateLoginMessage(HttpSession session) {
    return generateLoginMessage(session, false);
  }

  /**
   * Retrieves Login Message to Sign with Metamask Generated and stored in HTTP
   * Session
   * 
   * @param session {@link HttpSession} of current user
   * @return Login Message
   */
  public String getLoginMessage(HttpSession session) {
    return session == null ? null : (String) session.getAttribute(LOGIN_MESSAGE_ATTRIBUTE_NAME);
  }

  /**
   * @return true if current instance if the one of a Tenant Management
   */
  public boolean isDeedHub() {
    try {
      return hubService.isConnected();
    } catch (Exception e) {
      LOG.warn("Error checking whether the current installation is a Deed Tenant or not, return false", e);
      return false;
    }
  }

  /**
   * @return DEED NFT identifier
   */
  public long getDeedId() {
    return hubService.getDeedId();
  }

  private String generateRandomToken() {
    SecureRandom secureRandom = secureRandomProvider.getSecureRandom();
    return secureRandom.nextLong() + "-" + secureRandom.nextLong() + "-" + secureRandom.nextLong();
  }

  private void secureRootPassword() {
    UserHandler userHandler = organizationService.getUserHandler();
    if (userHandler == null) {
      return;
    }
    try {
      User rootUser = userHandler.findUserByName(userAcl.getSuperUser());
      if (rootUser == null) {
        LOG.warn("Root user wasn't found, can't regenerate password.");
      } else {
        LOG.info("Regenerate root password to allow accessing it via Metamask only");
        rootUser.setPassword(generateRandomToken());
        userHandler.saveUser(rootUser, false);
      }
    } catch (Exception e) {
      LOG.warn("Can't secure root access", e);
    }
  }

}
