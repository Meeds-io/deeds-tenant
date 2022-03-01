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
import java.util.Arrays;
import java.util.List;

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

public class MetamaskLoginService {

  protected static final Log   LOG                               = ExoLogger.getLogger(MetamaskLoginService.class);

  private static final String  PERSONAL_MESSAGE_PREFIX           = "\u0019Ethereum Signed Message:\n";

  private static final String  LOGIN_MESSAGE_ATTRIBUTE_NAME      = "metamask_login_message";

  private static final String  METAMASK_ALLOW_REGISTRATION_PARAM = "allow.registration";

  private OrganizationService  organizationService;

  private SecureRandomService  secureRandomService;

  private TenantManagerService tenantManagerService;

  private boolean              allowUserRegistration;

  public MetamaskLoginService(OrganizationService organizationService,
                              SecureRandomService secureRandomService,
                              TenantManagerService tenantManagerService,
                              InitParams params) {
    this.organizationService = organizationService;
    this.secureRandomService = secureRandomService;
    this.tenantManagerService = tenantManagerService;
    if (params != null && params.containsKey(METAMASK_ALLOW_REGISTRATION_PARAM)) {
      this.allowUserRegistration = Boolean.parseBoolean(params.getValueParam(METAMASK_ALLOW_REGISTRATION_PARAM).getValue());
    }
  }

  /**
   * @return allowUserRegistration parameter value, else, it will checks whether
   *           the Tenant Manager has been registered to the tenant or not. If
   *           not regitered, allow to display the register form, else return
   *           false.
   */
  public boolean isAllowUserRegistration() {
    if (allowUserRegistration) {
      return true;
    } else {
      String tenantManagerAddress = tenantManagerService.getManagerAddress();
      if (StringUtils.isNotBlank(tenantManagerAddress)) {
        try {
          User tenantManager = organizationService.getUserHandler().findUserByName(tenantManagerAddress.toLowerCase());
          if (tenantManager == null) {
            // Allow manager to register the first time the tenant is
            // provisioned
            return true;
          }
        } catch (Exception e) {
          LOG.warn("Error retrieving user with name {}", tenantManagerAddress, e);
        }
      }
      return allowUserRegistration;
    }
  }

  /**
   * @param walletAddress wallet address that attempts to register
   * @return allowUserRegistration parameter value, else, it will checks whether
   *           the Tenant Manager has been registered to the tenant or not. If
   *           not regitered, allow to display the register form, else return
   *           false.
   */
  public boolean isAllowUserRegistration(String walletAddress) {
    if (allowUserRegistration) {
      return true;
    } else {
      return tenantManagerService.isTenantManager(walletAddress);
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
   *           one
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
   *           one
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
    try {
      validateUsername(username);
      validateAndSetFullName(user, fullName);
      validateAndSetEmail(user, email);

      userHandler.createUser(user, true);
      if (tenantManagerService.isTenantManager(user.getUserName())) {
        setTenantManagerRoles(user);
      }
      return user;
    } catch (RegistrationException e) {
      throw e;
    } catch (Exception e) {
      LOG.warn("Error regitering user", e);
      throw new RegistrationException("REGISTRATION_ERROR");
    }
  }

  private void setTenantManagerRoles(User user) {
    List<String> tenantManagerRoles = tenantManagerService.getTenantManagerDefaultRoles();
    LOG.info("Tenant manager registered, setting its default memberships as manager.");
    for (String role : tenantManagerRoles) {
      if (StringUtils.isNotBlank(role)) {
        LOG.info("Add Tenant manager membership {}.", role);
        if (StringUtils.contains(role, ":")) {
          String[] roleParts = StringUtils.split(role, ":");
          String membershipTypeId = roleParts[0];
          String groupId = roleParts[1];

          addUserToGroup(user, groupId, membershipTypeId);
        } else {
          addUserToGroup(user, role, "*");
        }
      }
    }
  }

  private void addUserToGroup(User user, String groupId, String membershipTypeId) {
    GroupHandler groupHandler = organizationService.getGroupHandler();
    MembershipHandler membershipHandler = organizationService.getMembershipHandler();
    MembershipTypeHandler membershipTypeHandler = organizationService.getMembershipTypeHandler();
    try {
      Group group = groupHandler.findGroupById(groupId);
      MembershipType membershipType = membershipTypeHandler.findMembershipType(membershipTypeId);
      if (group != null && membershipType != null) {
        membershipHandler.linkMembership(user, group, membershipType, true);
      } else if (group == null) {
        LOG.warn("Group with id {} wasn't found. Tenant manager membership {} will not be set.",
                 groupId,
                 membershipTypeId + ":" + groupId);
      } else {
        LOG.warn("Membership Type with id {} wasn't found. Tenant manager membership {} will not be set.",
                 membershipTypeId,
                 membershipTypeId + ":" + groupId);
      }
    } catch (Exception e) {
      LOG.warn("Error while adding user {} to role {}:{}", user.getUserName(), membershipTypeId, groupId, e);
    }
  }

  private void validateAndSetFullName(User user, String fullName) throws RegistrationException {
    if (StringUtils.isBlank(fullName)) {
      throw new RegistrationException("FULLNAME_MANDATORY");
    } else if (StringUtils.contains(fullName, " ")) {
      String[] fullNameParts = fullName.split(" ");
      user.setFirstName(fullNameParts[0]);
      user.setLastName(StringUtils.join(fullNameParts, " ", 1, fullNameParts.length));
    } else {
      user.setLastName(fullName);
      user.setFirstName("");
    }
  }

  private void validateUsername(String username) throws Exception {
    User existingUser = organizationService.getUserHandler().findUserByName(username);
    if (existingUser != null) {
      throw new RegistrationException("USERNAME_ALREADY_EXISTS");
    }
  }

  private void validateAndSetEmail(User user, String email) throws Exception {
    if (StringUtils.isNotBlank(email)) {
      ListAccess<User> users;
      int usersLength = 0;
      try {
        // Check if mail address is already used
        Query query = new Query();
        query.setEmail(email);

        users = organizationService.getUserHandler().findUsersByQuery(query, UserStatus.ANY);
        usersLength = users.getSize();
      } catch (RuntimeException e) {
        LOG.debug("Error retrieving users list with email {}. Thus, we will consider the email as already used", email, e);
        usersLength = 1;
      }
      if (usersLength > 0) {
        throw new RegistrationException("EMAIL_ALREADY_EXISTS");
      }
      user.setEmail(email);
    }
  }

}
