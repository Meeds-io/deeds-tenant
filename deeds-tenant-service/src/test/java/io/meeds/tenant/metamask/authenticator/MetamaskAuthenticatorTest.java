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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

import org.exoplatform.services.security.*;

import io.meeds.tenant.metamask.service.MetamaskLoginService;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore(
  {
      "com.sun.*",
      "org.w3c.*",
      "javax.xml.*",
      "javax.management.*",
      "org.xml.*",
  }
)
public class MetamaskAuthenticatorTest {

  @Mock
  private MetamaskLoginService  metamaskLoginService;

  private MetamaskAuthenticator metamaskAuthenticator;

  @Before
  public void setUp() {
    reset(metamaskLoginService);
    metamaskAuthenticator = new MetamaskAuthenticator(metamaskLoginService);
  }

  @Test
  public void testValidateInvalidCredentialsCount() {
    assertNull(metamaskAuthenticator.validateUser(null));
    assertNull(metamaskAuthenticator.validateUser(new Credential[0]));
    assertNull(metamaskAuthenticator.validateUser(new Credential[] { new UsernameCredential("username") }));
    assertNull(metamaskAuthenticator.validateUser(new Credential[] { new UsernameCredential(null) }));
    assertNull(metamaskAuthenticator.validateUser(new Credential[] { new PasswordCredential("password") }));
    assertNull(metamaskAuthenticator.validateUser(new Credential[] { new PasswordCredential(null) }));
  }

  @Test
  public void testValidateInvalidPassword() {
    assertNull(metamaskAuthenticator.validateUser(new Credential[] {
        new UsernameCredential(null),
        new PasswordCredential(null),
    }));
    assertNull(metamaskAuthenticator.validateUser(new Credential[] {
        new UsernameCredential("username"),
        new PasswordCredential("password"),
    }));
    assertNull(metamaskAuthenticator.validateUser(new Credential[] {
        new UsernameCredential("username"),
        new PasswordCredential("password@password@password"),
    }));
  }

  @Test
  public void testValidateNotExistingUser() {
    String walletAddress = "walletAddress";
    String rawMessage = "rawMessage";
    String signedMessage = "signedMessage";

    when(metamaskLoginService.validateSignedMessage(walletAddress, rawMessage, signedMessage)).thenReturn(true);
    assertEquals(walletAddress,
                 metamaskAuthenticator.validateUser(new Credential[] {
                     new UsernameCredential(walletAddress),
                     new PasswordCredential(walletAddress + "@" + rawMessage + "@" + signedMessage),
                 }));
  }

  @Test
  public void testValidateWithDifferentUsernameAsResult() {
    String walletAddress = "walletAddress";
    String rawMessage = "rawMessage";
    String signedMessage = "signedMessage";
    String username = "username";

    when(metamaskLoginService.validateSignedMessage(walletAddress, rawMessage, signedMessage)).thenReturn(true);
    when(metamaskLoginService.getUserWithWalletAddress(walletAddress)).thenReturn(username);
    assertEquals(username,
                 metamaskAuthenticator.validateUser(new Credential[] {
                     new UsernameCredential(walletAddress),
                     new PasswordCredential(walletAddress + "@" + rawMessage + "@" + signedMessage),
                 }));
  }

}
