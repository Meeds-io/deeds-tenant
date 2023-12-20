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

import static io.meeds.tenant.metamask.service.MetamaskLoginService.ALLOWED_ROOT_ACCESS_WALLETS_PARAM;
import static io.meeds.tenant.metamask.service.MetamaskLoginService.LOGIN_MESSAGE_ATTRIBUTE_NAME;
import static io.meeds.tenant.metamask.service.MetamaskLoginService.SECURE_ROOT_ACCESS_WITH_METAMASK_PARAM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.servlet.http.HttpSession;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.OngoingStubbing;

import org.exoplatform.account.setup.web.AccountSetupService;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.organization.GroupHandler;
import org.exoplatform.services.organization.MembershipHandler;
import org.exoplatform.services.organization.MembershipTypeHandler;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.web.security.security.SecureRandomService;

import io.meeds.portal.security.constant.UserRegistrationType;
import io.meeds.portal.security.service.SecuritySettingService;
import io.meeds.tenant.metamask.FakeTestException;
import io.meeds.tenant.service.TenantManagerService;

@RunWith(MockitoJUnitRunner.class)
public class MetamaskLoginServiceTest {

  static final String    SUPER_USER = "superUser";

  @Mock
  OrganizationService    organizationService;

  @Mock
  UserHandler            userHandler;

  @Mock
  GroupHandler           groupHandler;

  @Mock
  MembershipTypeHandler  membershipTypeHandler;

  @Mock
  MembershipHandler      membershipHandler;

  @Mock
  UserACL                userAcl;

  @Mock
  SecureRandomService    secureRandomService;

  @Mock
  TenantManagerService   tenantManagerService;

  @Mock
  SecuritySettingService securitySettingService;

  @Mock
  InitParams             params;

  MetamaskLoginService   metamaskLoginService;

  @Before
  public void setUp() {
    reset(organizationService,
          userHandler,
          groupHandler,
          membershipHandler,
          membershipTypeHandler,
          userAcl,
          secureRandomService,
          tenantManagerService);

    when(organizationService.getUserHandler()).thenReturn(userHandler);
    when(userAcl.getSuperUser()).thenReturn(SUPER_USER);
  }

  @Test
  public void testDontSetRootPasswordOnStartup() {
    newService();
    metamaskLoginService.start();
    verifyNoInteractions(organizationService);
  }

  @Test
  public void testSetRootPasswordOnStartup() throws Exception {
    when(params.containsKey(SECURE_ROOT_ACCESS_WITH_METAMASK_PARAM)).thenReturn(true);
    ValueParam valueParam = new ValueParam();
    valueParam.setValue("true");
    when(params.getValueParam(SECURE_ROOT_ACCESS_WITH_METAMASK_PARAM)).thenReturn(valueParam);

    newService();

    metamaskLoginService.start();
    verify(userHandler, times(0)).saveUser(any(), anyBoolean());

    User user = mock(User.class);
    when(userHandler.findUserByName(SUPER_USER)).thenReturn(user);

    mockSecureRandomService();

    metamaskLoginService.start();
    verify(user, times(1)).setPassword("1-2-3");
    verify(userHandler, times(1)).saveUser(user, false);
  }

  @Test
  public void testIsAllowUserRegistration() throws Exception {
    newService();
    assertFalse(metamaskLoginService.isAllowUserRegistration());

    when(securitySettingService.getRegistrationType()).thenReturn(UserRegistrationType.OPEN);
    newService();
    assertTrue(metamaskLoginService.isAllowUserRegistration());

    when(securitySettingService.getRegistrationType()).thenReturn(UserRegistrationType.RESTRICTED);
    newService();
    assertFalse(metamaskLoginService.isAllowUserRegistration());
  }

  @Test
  public void testIsAddressAllowedToRegister() throws Exception {
    String managerAddress = "managerAddress";
    newService();
    assertFalse(metamaskLoginService.isAllowUserRegistration(managerAddress));

    newService();
    when(securitySettingService.getRegistrationType()).thenReturn(UserRegistrationType.OPEN);
    assertTrue(metamaskLoginService.isAllowUserRegistration(managerAddress));

    newService();
    when(securitySettingService.getRegistrationType()).thenReturn(UserRegistrationType.RESTRICTED);
    assertFalse(metamaskLoginService.isAllowUserRegistration(managerAddress));

    when(tenantManagerService.isTenantManager(managerAddress)).thenReturn(true);
    assertTrue(metamaskLoginService.isAllowUserRegistration(managerAddress));
    assertFalse(metamaskLoginService.isAllowUserRegistration("anyOtherAddress"));
  }

  @Test
  public void testGetUserWithWalletAddress() throws Exception {
    String managerAddress = "managerAddress";

    newService();
    assertNull(metamaskLoginService.getUserWithWalletAddress(managerAddress));

    when(params.containsKey(SECURE_ROOT_ACCESS_WITH_METAMASK_PARAM)).thenReturn(true);
    ValueParam secureRootAccessValueParam = new ValueParam();
    secureRootAccessValueParam.setValue("true");
    when(params.getValueParam(SECURE_ROOT_ACCESS_WITH_METAMASK_PARAM)).thenReturn(secureRootAccessValueParam);
    assertFalse(metamaskLoginService.isSuperUser(managerAddress));

    newService();
    assertNull(metamaskLoginService.getUserWithWalletAddress(managerAddress));

    when(params.containsKey(ALLOWED_ROOT_ACCESS_WALLETS_PARAM)).thenReturn(true);
    ValueParam allowedWalletsValueParam = new ValueParam();
    allowedWalletsValueParam.setValue(managerAddress);
    when(params.getValueParam(ALLOWED_ROOT_ACCESS_WALLETS_PARAM)).thenReturn(allowedWalletsValueParam);

    newService();
    assertEquals(SUPER_USER, metamaskLoginService.getUserWithWalletAddress(managerAddress));
    assertTrue(metamaskLoginService.isSuperUser(managerAddress));
    assertFalse(metamaskLoginService.isSuperUser("managerAddress2"));

    secureRootAccessValueParam.setValue("false");
    newService();
    assertNull(metamaskLoginService.getUserWithWalletAddress(managerAddress));

    User user = mock(User.class);
    whenFindUserByName(managerAddress).thenReturn(user);
    String managerUsername = "anotherUsername";
    when(user.getUserName()).thenReturn(managerUsername);
    assertEquals(managerUsername, metamaskLoginService.getUserWithWalletAddress(managerAddress));

    whenFindUserByName(managerAddress).thenThrow(new FakeTestException());
    assertNull(metamaskLoginService.getUserWithWalletAddress(managerAddress));
  }

  @Test
  public void testValidateSignedMessage() throws Exception {
    String walletAddress = "0x927f51a2996Ff74d1C380F92DC9006b53A225CeF";
    String rawMessage = "-2037692822791791745-3891968992033463560-1384458414145506416";
    String signedMessage =
                         "0x92874882ac3b2292dc4a05af2f0eceac48fee97392a26d8bc9002159c35279ac0b72729cbdd6e864696782176a39a5cdfbca45c3eec5b34e1f82d2a906356a7d1c";

    newService();

    assertFalse(metamaskLoginService.validateSignedMessage(null, rawMessage, signedMessage));
    assertFalse(metamaskLoginService.validateSignedMessage(walletAddress, null, signedMessage));
    assertFalse(metamaskLoginService.validateSignedMessage(walletAddress, rawMessage, null));
    assertFalse(metamaskLoginService.validateSignedMessage(walletAddress, rawMessage, walletAddress));
    assertFalse(metamaskLoginService.validateSignedMessage(walletAddress, walletAddress, signedMessage));
    assertFalse(metamaskLoginService.validateSignedMessage("0x12eF3db2F4F2F2ace676d9EAABFaE4d98EFdA9f5",
                                                           rawMessage,
                                                           signedMessage));
    assertTrue(metamaskLoginService.validateSignedMessage(walletAddress, rawMessage, signedMessage));
  }

  @Test
  public void testGenerateLoginMessage() throws Exception {
    newService();
    mockSecureRandomService();

    assertNotNull(metamaskLoginService.generateLoginMessage(null));

    HttpSession session = mock(HttpSession.class);
    String token = metamaskLoginService.generateLoginMessage(session);
    assertNotNull(token);
    verify(session, times(1)).setAttribute(LOGIN_MESSAGE_ATTRIBUTE_NAME, token);

    when(session.getAttribute(LOGIN_MESSAGE_ATTRIBUTE_NAME)).thenReturn(token);
    String token2 = metamaskLoginService.generateLoginMessage(session);
    assertEquals(token, token2);

    token2 = metamaskLoginService.generateLoginMessage(session, false);
    assertEquals(token, token2);

    token2 = metamaskLoginService.generateLoginMessage(session, true);
    assertNotEquals(token, token2);
    verify(session, times(1)).setAttribute(LOGIN_MESSAGE_ATTRIBUTE_NAME, token2);
  }

  private void newService() {
    metamaskLoginService = new MetamaskLoginService(organizationService,
                                                    userAcl,
                                                    secureRandomService,
                                                    tenantManagerService,
                                                    mock(AccountSetupService.class),
                                                    securitySettingService,
                                                    params);
  }

  private void mockSecureRandomService() {
    SecureRandom secureRandom = mock(SecureRandom.class);
    when(secureRandomService.getSecureRandom()).thenReturn(secureRandom);

    AtomicLong index = new AtomicLong();
    when(secureRandom.nextLong()).thenAnswer(new Answer<Long>() {
      @Override
      public Long answer(InvocationOnMock invocation) throws Throwable {
        return index.incrementAndGet();
      }
    });
  }

  private OngoingStubbing<User> whenFindUserByName(String managerAddress) throws Exception {
    return when(userHandler.findUserByName(managerAddress.toLowerCase()));
  }

}
