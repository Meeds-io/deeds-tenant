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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package io.meeds.tenant.metamask.service;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.web3j.crypto.*;
import org.web3j.utils.Numeric;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.*;
import org.exoplatform.web.security.security.SecureRandomService;

import io.meeds.tenant.metamask.RegistrationException;
import lombok.Getter;

public class MetamaskLoginService {

  protected static final Log  LOG                          = ExoLogger.getLogger(MetamaskLoginService.class);

  private static final String PERSONAL_MESSAGE_PREFIX      = "\u0019Ethereum Signed Message:\n";

  private static final String NETWORK_ID_PARAM             = "networkId";

  private static final String NETWORK_URL_PARAM            = "networkURL";

  private static final String NETWORK_WS_URL_PARAM         = "networkWSURL";

  private static final String DEED_ADDRESS_PARAM           = "deedAddress";

  private static final String LOGIN_MESSAGE_ATTRIBUTE_NAME = "metamask_login_message";

  @Getter
  private long                networkId;

  @Getter
  private String              networkUrl;

  @Getter
  private String              networkWsUrl;

  @Getter
  private String              deedAddress;

  private OrganizationService organizationService;

  private SecureRandomService secureRandomService;

  public MetamaskLoginService(OrganizationService organizationService,
                              SecureRandomService secureRandomService,
                              InitParams params) {
    this.organizationService = organizationService;
    this.secureRandomService = secureRandomService;
    if (params != null) {
      if (params.containsKey(NETWORK_ID_PARAM)) {
        this.networkId = Long.parseLong(params.getValueParam(NETWORK_ID_PARAM).getValue());
      }
      if (params.containsKey(NETWORK_URL_PARAM)) {
        this.networkUrl = params.getValueParam(NETWORK_URL_PARAM).getValue();
      }
      if (params.containsKey(NETWORK_WS_URL_PARAM)) {
        this.networkWsUrl = params.getValueParam(NETWORK_WS_URL_PARAM).getValue();
      }
      if (params.containsKey(DEED_ADDRESS_PARAM)) {
        this.deedAddress = params.getValueParam(DEED_ADDRESS_PARAM).getValue();
      }
    }
  }

  /**
   * Retrieves User name with associated wallet Address
   * 
   * @param walletAddress Ethereum Wallet Address
   * @return username
   */
  public String getUserWithWalletAddress(String walletAddress) {
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
    String prefix = PERSONAL_MESSAGE_PREFIX + rawMessage.length();
    byte[] rawMessageHash = Hash.sha3((prefix + rawMessage).getBytes());
    byte[] signatureBytes = Numeric.hexStringToByteArray(signedMessage);
    byte[] r = Arrays.copyOfRange(signatureBytes, 0, 32);
    byte[] s = Arrays.copyOfRange(signatureBytes, 32, 64);
    // Iterate for each possible key to recover
    for (int i = 0; i < 4; i++) {
      BigInteger publicKey = Sign.recoverFromSignature(i,
                                                       new ECDSASignature(new BigInteger(1, r),
                                                                          new BigInteger(1, s)),
                                                       rawMessageHash);

      if (publicKey != null) {
        String recoveredAddress = "0x" + Keys.getAddress(publicKey);
        if (recoveredAddress.equalsIgnoreCase(walletAddress)) {
          return true;
        }
      }
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
    SecureRandom secureRandom = secureRandomService.getSecureRandom();
    token = secureRandom.nextLong() + "-" + secureRandom.nextLong() + "-" + secureRandom.nextLong();
    session.setAttribute(LOGIN_MESSAGE_ATTRIBUTE_NAME, token);
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
   * Register new User in platform based on Username, display name and email.
   * 
   * @param username {@link String}
   * @param fullName {@link String}
   * @param email {@link String} with already validated email format
   * @return created {@link User}
   * @throws RegistrationException when a registration error happens. The error
   *           code will be added into exception message
   */
  public User registerUser(String username, String fullName, String email) throws RegistrationException {
    UserHandler userHandler = organizationService.getUserHandler();
    User user = userHandler.createUserInstance(username);
    if (StringUtils.isBlank(fullName)) {
      user.setLastName("");
      user.setFirstName("");
    } else if (StringUtils.contains(fullName, " ")) {
      String[] fullNameParts = fullName.split(" ");
      user.setFirstName(fullNameParts[0]);
      user.setLastName(StringUtils.join(fullNameParts, " ", 1, fullNameParts.length));
    } else {
      user.setLastName(fullName);
      user.setFirstName("");
    }

    user.setEmail(email);
    try {
      User existingUser = userHandler.findUserByName(username);
      if (existingUser != null) {
        throw new RegistrationException("USERNAME_ALREADY_EXISTS");
      }

      if (StringUtils.isNotBlank(email)) {
        ListAccess<User> users;
        int usersLength = 0;
        try {
          // Check if mail address is already used
          Query query = new Query();
          query.setEmail(email);

          users = userHandler.findUsersByQuery(query, UserStatus.ANY);
          usersLength = users.getSize();
        } catch (RuntimeException e) {
          LOG.debug("Error retrieving users list with email {}. Thus, we will consider the email as already used", email, e);
          usersLength = 1;
        }
        if (usersLength > 0) {
          throw new RegistrationException("EMAIL_ALREADY_EXISTS");
        }
      }

      userHandler.createUser(user, true);
      return user;
    } catch (RegistrationException e) {
      throw e;
    } catch (Exception e) {
      LOG.warn("Error regitering user", e);
      throw new RegistrationException("REGISTRATION_ERROR");
    }
  }

}
