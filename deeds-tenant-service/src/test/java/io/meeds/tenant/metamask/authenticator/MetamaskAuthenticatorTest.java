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
package io.meeds.tenant.metamask.authenticator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.exoplatform.services.security.Authenticator;
import org.exoplatform.services.security.Credential;
import org.exoplatform.services.security.PasswordCredential;
import org.exoplatform.services.security.UsernameCredential;

import io.meeds.tenant.metamask.service.MetamaskLoginService;

@SpringBootTest(classes = {
  MetamaskAuthenticator.class,
})
public class MetamaskAuthenticatorTest {

  private static final String   PASSWORD = "password";

  private static final String   USERNAME = "username";

  @MockBean
  private MetamaskLoginService  metamaskLoginService;

  @MockBean
  private Authenticator         authenticator;

  @Autowired
  private MetamaskAuthenticator metamaskAuthenticator;

  @Test
  public void testValidateInvalidCredentialsCount() {
    assertNull(metamaskAuthenticator.validateUser(null));
    assertNull(metamaskAuthenticator.validateUser(new Credential[0]));
    assertNull(metamaskAuthenticator.validateUser(new Credential[] { new UsernameCredential(USERNAME) }));
    assertNull(metamaskAuthenticator.validateUser(new Credential[] { new UsernameCredential(null) }));
    assertNull(metamaskAuthenticator.validateUser(new Credential[] { new PasswordCredential(PASSWORD) }));
    assertNull(metamaskAuthenticator.validateUser(new Credential[] { new PasswordCredential(null) }));
  }

  @Test
  public void testValidateInvalidPassword() {
    assertNull(metamaskAuthenticator.validateUser(new Credential[] {
                                                                     new UsernameCredential(null),
                                                                     new PasswordCredential(null),
    }));
    assertNull(metamaskAuthenticator.validateUser(new Credential[] {
                                                                     new UsernameCredential(USERNAME),
                                                                     new PasswordCredential(PASSWORD),
    }));
    assertNull(metamaskAuthenticator.validateUser(new Credential[] {
                                                                     new UsernameCredential(USERNAME),
                                                                     new PasswordCredential("password@password@password"),
    }));
  }

  @Test
  public void testValidateNotExistingUser() {
    String walletAddress = "walletAddress";
    String otherUserName = "otherUserName";
    String rawMessage = "rawMessage";
    String signedMessage = "signedMessage";
    String composedPassword = walletAddress + "@" + rawMessage + "@" + signedMessage;

    when(metamaskLoginService.validateSignedMessage(walletAddress, rawMessage, signedMessage)).thenReturn(true);
    assertNull(metamaskAuthenticator.validateUser(new Credential[] { new UsernameCredential(walletAddress),
                                                                     new PasswordCredential(composedPassword) }));

    when(metamaskLoginService.getUserWithWalletAddress(walletAddress)).thenReturn(otherUserName);
    assertEquals(otherUserName,
                 metamaskAuthenticator.validateUser(new Credential[] { new UsernameCredential(walletAddress),
                                                                       new PasswordCredential(composedPassword) }));
  }

  @Test
  public void testValidateWithDifferentUsernameAsResult() {
    String walletAddress = "walletAddress";
    String rawMessage = "rawMessage";
    String signedMessage = "signedMessage";
    String username = USERNAME;

    when(metamaskLoginService.validateSignedMessage(walletAddress, rawMessage, signedMessage)).thenReturn(true);
    when(metamaskLoginService.getUserWithWalletAddress(walletAddress)).thenReturn(username);
    assertEquals(username,
                 metamaskAuthenticator.validateUser(new Credential[] {
                                                                       new UsernameCredential(walletAddress),
                                                                       new PasswordCredential(walletAddress + "@" + rawMessage +
                                                                           "@" + signedMessage),
                 }));
  }

}
