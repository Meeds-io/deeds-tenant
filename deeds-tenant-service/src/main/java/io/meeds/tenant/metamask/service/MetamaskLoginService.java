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

import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.picocontainer.Startable;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.crypto.Sign.SignatureData;
import org.web3j.utils.Numeric;

import org.exoplatform.account.setup.web.AccountSetupService;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.GroupHandler;
import org.exoplatform.services.organization.MembershipHandler;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.MembershipTypeHandler;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.Query;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.organization.UserStatus;
import org.exoplatform.web.security.security.SecureRandomService;

import io.meeds.tenant.metamask.RegistrationException;
import io.meeds.tenant.service.TenantManagerService;

public class MetamaskLoginService implements Startable {

  public static final String   LOGIN_MESSAGE_ATTRIBUTE_NAME           = "metamask_login_message";

  public static final String   METAMASK_ALLOW_REGISTRATION_PARAM      = "allow.registration";

  public static final String   SECURE_ROOT_ACCESS_WITH_METAMASK_PARAM = "secureRootAccessWithMetamask";

  public static final String   ALLOWED_ROOT_ACCESS_WALLETS_PARAM      = "allowedRootAccessWallets";

  protected static final Log   LOG                                    = ExoLogger.getLogger(MetamaskLoginService.class);

  private OrganizationService  organizationService;

  private UserACL              userACL;

  private SecureRandomService  secureRandomService;

  private TenantManagerService tenantManagerService;

  private AccountSetupService  accountSetupService;

  private boolean              allowUserRegistration;

  private boolean              secureRootAccessWithMetamask;

  private List<String>         allowedRootWallets                     = new ArrayList<>();

  public MetamaskLoginService(OrganizationService organizationService,
                              UserACL userACL,
                              SecureRandomService secureRandomService,
                              TenantManagerService tenantManagerService,
                              AccountSetupService accountSetupService,
                              InitParams params) {
    this.organizationService = organizationService;
    this.secureRandomService = secureRandomService;
    this.tenantManagerService = tenantManagerService;
    this.accountSetupService = accountSetupService;
    this.userACL = userACL;
    if (params != null) {
      if (params.containsKey(METAMASK_ALLOW_REGISTRATION_PARAM)) {
        this.allowUserRegistration = Boolean.parseBoolean(params.getValueParam(METAMASK_ALLOW_REGISTRATION_PARAM).getValue());
      }
      if (params.containsKey(SECURE_ROOT_ACCESS_WITH_METAMASK_PARAM)) {
        this.secureRootAccessWithMetamask = Boolean.parseBoolean(params.getValueParam(SECURE_ROOT_ACCESS_WITH_METAMASK_PARAM)
                                                                       .getValue());
      }
      if (params.containsKey(ALLOWED_ROOT_ACCESS_WALLETS_PARAM)) {
        String[] wallets = StringUtils.split(params.getValueParam(ALLOWED_ROOT_ACCESS_WALLETS_PARAM).getValue(), ",");
        Arrays.stream(wallets).forEach(address -> allowedRootWallets.add(address.trim().toLowerCase()));
      }
    }
  }

  @Override
  public void start() {
    if (this.secureRootAccessWithMetamask) {
      // Avoid allowing to change root password
      accountSetupService.setSkipSetup(true);
      // Generate a new random password for root user
      secureRootPassword();
    }
  }

  @Override
  public void stop() {
    // Nothing to stop
  }

  /**
   * @return allowUserRegistration parameter value
   */
  public boolean isAllowUserRegistration() {
    return allowUserRegistration;
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
      return isTenantManager(walletAddress);
    }
  }

  /**
   * @param walletAddress to check if it's of Tenant Manager
   * @return true is wallet address is of the Tenant Manager else return false.
   */
  public boolean isTenantManager(String walletAddress) {
    return tenantManagerService.isTenantManager(walletAddress);
  }

  /**
   * @param walletAddress wallet address
   * @return true if secure root access is allowed and designated wallet is
   *           allowed to access using root account
   */
  public boolean isSuperUser(String walletAddress) {
    return secureRootAccessWithMetamask && allowedRootWallets.contains(walletAddress.toLowerCase());
  }

  /**
   * Retrieves User name with associated wallet Address
   * 
   * @param walletAddress Ethereum Wallet Address
   * @return username
   */
  public String getUserWithWalletAddress(String walletAddress) {
    if (secureRootAccessWithMetamask && allowedRootWallets.contains(walletAddress.toLowerCase())) {
      return userACL.getSuperUser();
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
   *           one
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
    try {
      username = StringUtils.lowerCase(username);
      User user = userHandler.createUserInstance(username);
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

  /**
   * @return true if current instance if the one of a Tenant Management
   */
  public boolean isDeedTenant() {
    try {
      return tenantManagerService.isTenant();
    } catch (Exception e) {
      LOG.warn("Error checking whether the current installation is a Deed Tenant or not, return false", e);
      return false;
    }
  }

  /**
   * @return DEED NFT identifier
   */
  public long getDeedId() {
    return tenantManagerService.getNftId();
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
      user.setFirstName(StringUtils.join(fullNameParts, " ", 0, fullNameParts.length - 1));
      user.setLastName(fullNameParts[fullNameParts.length - 1]);
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
        usersLength = users == null ? 0 : users.getSize();
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

  private String generateRandomToken() {
    SecureRandom secureRandom = secureRandomService.getSecureRandom();
    return secureRandom.nextLong() + "-" + secureRandom.nextLong() + "-" + secureRandom.nextLong();
  }

  private void secureRootPassword() {
    try {
      User rootUser = organizationService.getUserHandler().findUserByName(userACL.getSuperUser());
      if (rootUser == null) {
        LOG.warn("Root user wasn't found, can't regenerate password.");
      } else {
        LOG.info("Regenerate root password to allow accessing it via Metamask only");
        rootUser.setPassword(generateRandomToken());
        organizationService.getUserHandler().saveUser(rootUser, false);
      }
    } catch (Exception e) {
      LOG.warn("Can't secure root access", e);
    }
  }

}
