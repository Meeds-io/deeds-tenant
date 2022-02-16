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
package io.meeds.tenant.metamask.authenticator;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.services.organization.auth.AuthenticatorPlugin;
import org.exoplatform.services.security.*;

import io.meeds.tenant.metamask.service.MetamaskLoginService;
import io.meeds.tenant.metamask.web.filter.MetamaskRegistrationFilter;

/**
 * An authenticator plugin to authenticate Metamask users
 */
public class MetamaskAuthenticator extends AuthenticatorPlugin {

  private MetamaskLoginService metamaskLoginService;

  public MetamaskAuthenticator(MetamaskLoginService metamaskLoginService) {
    this.metamaskLoginService = metamaskLoginService;
  }

  @Override
  public String validateUser(Credential[] credentials) { // NOSONAR
    if (credentials != null && credentials.length == 2 && credentials[0] instanceof UsernameCredential) {
      UsernameCredential usernameCredential = (UsernameCredential) credentials[0];
      String walletAddress = usernameCredential.getUsername();

      if (StringUtils.startsWith(walletAddress, "0x")) {
        PasswordCredential passwordCredential = (PasswordCredential) credentials[1];
        String password = passwordCredential.getPassword();
        String[] passwordParts = StringUtils.split(password, MetamaskRegistrationFilter.SEPARATOR);
        if (passwordParts != null && passwordParts.length == 2) {
          String rawMessage = passwordParts[0];
          String signedMessage = passwordParts[1];

          boolean validated = metamaskLoginService.validateSignedMessage(walletAddress, rawMessage, signedMessage);
          if (validated) {
            String username = metamaskLoginService.getUserWithWalletAddress(walletAddress);
            return StringUtils.isBlank(username) ? walletAddress : username;
          }
        }
      }
    }
    return null;
  }

}
