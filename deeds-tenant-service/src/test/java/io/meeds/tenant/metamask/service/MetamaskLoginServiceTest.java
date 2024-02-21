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

import static io.meeds.tenant.metamask.service.MetamaskLoginService.LOGIN_MESSAGE_ATTRIBUTE_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.OngoingStubbing;
import org.picketlink.idm.api.SecureRandomProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.exoplatform.account.setup.web.AccountSetupService;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;

import io.meeds.portal.security.constant.UserRegistrationType;
import io.meeds.portal.security.service.SecuritySettingService;
import io.meeds.tenant.hub.service.HubService;
import io.meeds.tenant.metamask.FakeTestException;

import jakarta.servlet.http.HttpSession;

@SpringBootTest(classes = {
  MetamaskLoginService.class,
})
class MetamaskLoginServiceTest {

  static final String            SUPER_USER = "superUser";

  @MockBean
  private SecuritySettingService securitySettingService;

  @MockBean
  private OrganizationService    organizationService;

  @MockBean
  private UserACL                userAcl;

  @MockBean
  private SecureRandomProvider   secureRandomProvider;

  @MockBean
  private AccountSetupService    accountSetupService;

  @MockBean
  private HubService             hubService;

  @MockBean
  private UserHandler            userHandler;

  @Autowired
  MetamaskLoginService           metamaskLoginService;

  @BeforeEach
  void setUp() {
    when(organizationService.getUserHandler()).thenReturn(userHandler);
    when(userAcl.getSuperUser()).thenReturn(SUPER_USER);
  }

  @Test
  void testDontSetRootPasswordOnStartup() {
    metamaskLoginService.setAllowedRootWallets(Collections.emptyList());
    metamaskLoginService.init();
    verifyNoInteractions(organizationService);
  }

  @Test
  void testSetRootPasswordOnStartup() throws Exception {
    metamaskLoginService.init();
    verify(userHandler, times(0)).saveUser(any(), anyBoolean());

    User user = mock(User.class);
    when(userHandler.findUserByName(SUPER_USER)).thenReturn(user);

    mockSecureRandomService();

    metamaskLoginService.setAllowedRootWallets(Collections.singletonList("0xAddress"));
    metamaskLoginService.init();
    verify(user, times(1)).setPassword("1-2-3");
    verify(userHandler, times(1)).saveUser(user, false);
  }

  @Test
  void testIsAllowUserRegistration() {
    assertFalse(metamaskLoginService.isAllowUserRegistration());

    when(securitySettingService.getRegistrationType()).thenReturn(UserRegistrationType.OPEN);
    assertTrue(metamaskLoginService.isAllowUserRegistration());

    when(securitySettingService.getRegistrationType()).thenReturn(UserRegistrationType.RESTRICTED);
    assertFalse(metamaskLoginService.isAllowUserRegistration());
  }

  @Test
  void testIsAddressAllowedToRegister() {
    String managerAddress = "managerAddress";
    assertFalse(metamaskLoginService.isAllowUserRegistration(managerAddress));

    when(securitySettingService.getRegistrationType()).thenReturn(UserRegistrationType.OPEN);
    assertTrue(metamaskLoginService.isAllowUserRegistration(managerAddress));

    when(securitySettingService.getRegistrationType()).thenReturn(UserRegistrationType.RESTRICTED);
    assertFalse(metamaskLoginService.isAllowUserRegistration(managerAddress));

    when(hubService.isDeedManager(managerAddress)).thenReturn(true);
    assertTrue(metamaskLoginService.isAllowUserRegistration(managerAddress));
    assertFalse(metamaskLoginService.isAllowUserRegistration("anyOtherAddress"));
  }

  @Test
  void testGetUserWithWalletAddress() throws Exception {
    String managerAddress = "managerAddress";

    assertNull(metamaskLoginService.getUserWithWalletAddress(managerAddress));
    assertFalse(metamaskLoginService.isSuperUser(managerAddress));

    metamaskLoginService.setAllowedRootWallets(Collections.singletonList(managerAddress));
    try {
      assertEquals(SUPER_USER, metamaskLoginService.getUserWithWalletAddress(managerAddress));
      assertTrue(metamaskLoginService.isSuperUser(managerAddress));
      assertFalse(metamaskLoginService.isSuperUser("managerAddress2"));
    } finally {
      metamaskLoginService.setAllowedRootWallets(Collections.emptyList());
    }

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
  void testValidateSignedMessage() {
    String walletAddress = "0x927f51a2996Ff74d1C380F92DC9006b53A225CeF";
    String rawMessage = "-2037692822791791745-3891968992033463560-1384458414145506416";
    String signedMessage =
                         "0x92874882ac3b2292dc4a05af2f0eceac48fee97392a26d8bc9002159c35279ac0b72729cbdd6e864696782176a39a5cdfbca45c3eec5b34e1f82d2a906356a7d1c";

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
  void testGenerateLoginMessage() {
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

  private void mockSecureRandomService() {
    SecureRandom secureRandom = mock(SecureRandom.class);
    when(secureRandomProvider.getSecureRandom()).thenReturn(secureRandom);

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
